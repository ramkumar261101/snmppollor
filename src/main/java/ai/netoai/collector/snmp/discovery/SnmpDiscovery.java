    /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.netoai.collector.snmp.discovery;

import ai.netoai.collector.cache.NodeCacheManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import ai.netoai.collector.deviceprofile.Config;
import ai.netoai.collector.deviceprofile.ConfigManager;
import ai.netoai.collector.deviceprofile.PhysicalLinkConfig;
import ai.netoai.collector.deviceprofile.SnmpConfig;
import ai.netoai.collector.model.*;
import ai.netoai.collector.settings.KafkaTopicSettings;
import ai.netoai.collector.settings.SettingsManager;
import ai.netoai.collector.settings.SnmpSettings;
import ai.netoai.collector.utils.IpHostsDiscoveryUtil;
import ai.netoai.collector.utils.TopicJsonSender;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.AbstractTarget;
import org.snmp4j.PDU;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;

import ai.netoai.collector.snmp.SnmpPoller;
import ai.netoai.collector.startup.PollerThreadFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.InetAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.snmp4j.CommunityTarget;
import org.snmp4j.Target;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;


public class SnmpDiscovery extends Discovery {

    private static final Logger log = LoggerFactory.getLogger(SnmpDiscovery.class);
    private static final String tracePrefix = "[" + SnmpDiscovery.class.getSimpleName() + "]: ";
    private static int globalCounter;
    private SnmpPoller poller;
    private TopicJsonSender topicInstance;
    private String tenantId;
    private SnmpSettings snmpSettings;
    private NodeCacheManager cacheManager;
    private static String pingMonitorScriptPath;
    private ScheduledExecutorService executor;

    public SnmpDiscovery(DiscoveryTask dt) {
        super(dt);
        this.poller = SnmpPoller.getInstance();
        this.tenantId = System.getProperty("tenantId");
        this.cacheManager = NodeCacheManager.getInstance();
        this.executor = Executors.newScheduledThreadPool(5,
                new PollerThreadFactory("TopologyDiscoveryTask"));
        SettingsManager setting = SettingsManager.getInstance();
        this.snmpSettings = new SnmpSettings();
        Map<String, Object> collectorSettings = setting.getSettings();
        String jsPath = (String) collectorSettings.get("monitorScriptsLocation");
        pingMonitorScriptPath = jsPath + "/ping-monitor.py";
        log.info("Ping Monitor Script Path " + pingMonitorScriptPath);
    }

    @Override
    public void start() {
        discoveryTaskExecutors.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (dt.getDiscoveryType().equals(DiscoveryType.INVENTORY.toString())) {
                        initDiscovery();
                    } else {
                        initTopoDiscovery();
                    }
                } catch (InterruptedException e) {
                    log.error("Failed", e);
                }
            }
        });
    }

    private void initTopoDiscovery() {
        log.info(tracePrefix + " Initializing Topology Discovery");
        TopicJsonSender topicInstance = TopicJsonSender.getInstance();
        this.topicInstance = topicInstance;
        String schedult = dt.getSchedule();

        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                Gson gson = new GsonBuilder().serializeNulls().create();
                SnmpProfileHelper snmp = new SnmpProfileHelper();
                List<String> ipRanges = dt.getIpRange();
                Set<String> ipList = new HashSet<>();
                for (String ipRange : ipRanges) {
                    ipList.addAll(IpHostsDiscoveryUtil.getIpsFromRange(ipRange));
                }

                log.info(tracePrefix + " IPs obtained : " + ipList.size());
                NodeManager nodeManager = NodeManager.getInstance();
                for (String ip : ipList) {
                    NetworkElement ne = nodeManager.getNetworkElement(ip);
                    if (ne != null) {
                        try {
                            ConfigManager config = ConfigManager.getInstance();
                            SnmpConfig snmpConfig = config.getConfigObjectForDevice(ne.getProfileId());
                            if (snmpConfig != null) {
                                List<Config> configL = config.getConfigObjectForDevice(ne.getProfileId()).getChildConfig(PhysicalLinkConfig.class);
                                if (configL.size() == 0) continue;
                                List<GenericJavaBean> linkList = snmp.createGenericBeanObjects(configL.get(0), createTarget(ne), ne);
                                for (GenericJavaBean bean : linkList) {
                                    String jsonString = gson.toJson(bean);
                                    if (bean instanceof NetworkElement) {
                                        NetworkElement node = (NetworkElement) bean;
                                        if (node.getName() == null) {
                                            node.setName(node.getIp());
                                        }
                                    }
                                    topicInstance.send(KafkaTopicSettings.INVENTORY_TOPIC, jsonString);
                                }
                            } else {
                                log.error(tracePrefix + " No Config Obj found for profile : " + ne.getProfileId());
                            }
                        } catch (UnknownHostException ex) {
                            log.error(tracePrefix + ex.getMessage());
                        }
                    }
                }
            }
        }, 0, 1, TimeUnit.HOURS);
    }

    private Target createTarget(NetworkElement ne) throws UnknownHostException {
        if (ne.getProtocol() == NetworkProtocol.SNMPv1 || ne.getProtocol() == NetworkProtocol.SNMPv2c) {
            CommunityTarget target = new CommunityTarget();
            target.setAddress(new UdpAddress(InetAddress.getByName(ne.getIp()), ne.getPort()));
            target.setCommunity(new OctetString(ne.getCommunityString()));
            target.setVersion(SnmpConstants.version2c);
            target.setRetries(ne.getSnmpRetries());
            target.setTimeout(ne.getSnmpTimeout());
            return target;
        } else {
            return null;
        }
    }

    public void initDiscovery() throws InterruptedException {
        TopicJsonSender topicInstance = TopicJsonSender.getInstance();
        this.topicInstance = topicInstance;

        List<String> ipRanges = this.dt.getIpRange();
        Set<String> ips = new HashSet<>();
        for (String ipRange : ipRanges) {
            ips.addAll(IpHostsDiscoveryUtil.getIpsFromRange(ipRange));
        }
        log.info(tracePrefix + " IPs obtained: " + ips.size());

        for (String ip : ips) {
            nodeDiscoveryExecutors.execute(new Runnable() {
                @Override
                public void run() {
                    createNetworkElement(ip);
                }
            });
            globalCounter++;
        }
        nodeDiscoveryExecutors.shutdown();
        //poller.stop();
    }

    private void createNetworkElement(String ip) {
        log.info("Trying to create a Network element " + ip);
        List<SnmpAuthProfile> authProfiles = this.dt.getAuthProfiles();
        NodeManager nodeManager = NodeManager.getInstance();
        Set<NetworkElement> allNE = nodeManager.getNetworkElements();
        boolean neFound = allNE.stream().anyMatch(ne -> ne.getIp().equals(ip));

        for (SnmpAuthProfile auth : authProfiles) {
            NetworkElement element = createTargetandSendSnmpReq(ip, auth);
            if (element != null) {
                if (element.getName() == null) {
                    element.setName(element.getIp());
                }
                Gson gson = new GsonBuilder().serializeNulls().create();
                String jsonString = gson.toJson(element);
//                topicInstance.send(KafkaTopicSettings.INVENTORY_TOPIC, jsonString);

                NodeManager nodeInstance = NodeManager.getInstance();
                nodeInstance.addNetworkElement(element);
                cacheManager.invalidate(element);
                break;
            }
        }
    }

    public NetworkElement createTargetandSendSnmpReq(String ip, SnmpAuthProfile auth) {
        NetworkElement networkElement = null;
        OID nameOID = SnmpConstants.sysName;
        OID locationOID = SnmpConstants.sysLocation;
        AbstractTarget target = null;
        try {
            switch (auth.getSnmpVersion()) {
                case SNMPv3:
                    target = poller.createSnmpV3Target(ip, auth);
                    break;
                case SNMPv2C:
                    target = poller.createSnmpV2Target(ip, auth);
                    break;
                case SNMPv1:
                    target = poller.createSnmpV1Target(ip, auth);
                    break;
                default:
                    break;
            }
        } catch (UnknownHostException e) {
            log.error(tracePrefix + "Failed creating target for IP: " + ip, e);
        }

        PDU pdu = new PDU();
        pdu.add(new VariableBinding(nameOID));
        pdu.add(new VariableBinding(locationOID));
        pdu.setType(PDU.GET);
        log.info("Sending SNMP Request for IP " + target.getAddress().toString());

        String nameValue = null;
        String locationValue = null;
        int counter = 0;
        int retries = 3;
        while ( counter < retries ) {
            try {
                if ( counter > 0 ) {
                    log.warn("{}Retrying the Address {} for {}/{} time", tracePrefix, target.getAddress().toString(), (counter + 1), retries);
                }
                PDU responsePdu = poller.sendSyncGetRequest(target, pdu, globalCounter);
                if (responsePdu != null) {
                    log.info("-->  Response for Ip " + target.getAddress().toString() + " is " + responsePdu);
                    Vector<VariableBinding> vbs = (Vector<VariableBinding>) responsePdu.getVariableBindings();
                    if (vbs != null && vbs.size() != 0) {
                        String value = vbs.get(0).getVariable().toString();
                        if (!value.equals("noSuchObject") && !value.isEmpty()) {
                            nameValue = value;
                            networkElement = new NetworkElement();
                        } else {
                            nameValue = target.getAddress().toString().split("\\/")[0];
                            networkElement = new NetworkElement();
                        }
                        if (vbs.size() > 0) {
                            String locVal = vbs.get(1).getVariable().toString();
                            if (!locVal.equals("noSuchObject") && !locVal.isEmpty()) {
                                locationValue = locVal.toString().trim().toLowerCase();
                            }
                        }
                    }
                    break;
                } else {
                    log.error("-->  No Response received for Ip " + target.getAddress().toString() + ", Response " + responsePdu);
                }
            } finally {
                counter++;
            }
        }


        if (nameValue != null) {
            networkElement.setIp(ip);
            networkElement.setName(nameValue);
            networkElement.setPort(auth.getPort());
            networkElement.setProtocol(SnmpVersion.getNetworkProtocol(auth.getSnmpVersion()));
            networkElement.setConnState(NetworkElement.State.UNKNOWN);
            networkElement.setReachableState(NetworkElement.ReachabilityState.UP);
            networkElement.setProfileId("unknown");
            networkElement.setSnmpTimeout(auth.getTimeout());
            networkElement.setSnmpRetries(auth.getRetries());
            networkElement.setCommunityString(auth.getCommunity());
            networkElement.setAuthProtocol(auth.getAuthProtocol());
            networkElement.setPrivProtocol(auth.getPrivProtocol());
            networkElement.setAuthPassword(auth.getAuthPassword());
            networkElement.setPrivPassword(auth.getPrivPassword());
            networkElement.setUserName(auth.getUserName());
            networkElement.setAuthorativeEngineId(auth.getContextName());
            networkElement.setInvSyncInterval(snmpSettings.getInventoryReSyncInterval());
            List<String> monitorIds = new ArrayList<String>();
            monitorIds.add("ping-monitor");
            networkElement.setMonitorIds(monitorIds);
        }
        if(locationValue != null){
            log.info("-->  location value is  " + locationValue);
            networkElement.setClliCode(StringUtils.substringBetween(locationValue, "cllicode :", "|"));
            networkElement.setPhoneNumber(StringUtils.substringBetween(locationValue, "phonenumber :", "|"));
            networkElement.setContactPerson(StringUtils.substringBetween(locationValue, "contactperson :", "|"));
            networkElement.setAddress(StringUtils.substringBetween(locationValue, "address :", "|"));
            networkElement.setLatitude(StringUtils.substringBetween(locationValue, "latitude :", "|"));
            networkElement.setLongitude(StringUtils.substringBetween(locationValue, "longitude :", "|"));
            networkElement.setDrivingInstructions(StringUtils.substringBetween(locationValue, "drivinginstructions :", "|"));
        }
        log.info("{}Created network element {} with name: {}", tracePrefix, networkElement, nameValue);
        return networkElement;
    }

    // Sends ping request to a provided IP address
    public static boolean sendPingRequest(String ipAddress) {
        Process process;
        try {
            process = Runtime.getRuntime().exec("python " + pingMonitorScriptPath + " " + ipAddress);
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output = in.readLine();
            if ( output == null ) {
                log.error("{}Ping request failed for IP {}, Response: {}", tracePrefix, ipAddress, output);
                return false;
            }
            Pattern pattern = Pattern.compile(".*PING OK.*");
            Matcher matcher = pattern.matcher(output);
            if (matcher.find()) return true;
            else return false;
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            log.error("Failed", e1);
        }
				
		
		/*InetAddress inetAddress;
		try {
			inetAddress = InetAddress.getByName(ipAddress);
			if (inetAddress.isReachable(5000)) {
				log.info("Host is reachable");
				return true;
			} else {
				log.info("Can't reach to the host "+ipAddress);
				return false;
			}
		} catch (Exception e) {
			log.error("Failed", e);
		}*/
        return false;
    }

}
