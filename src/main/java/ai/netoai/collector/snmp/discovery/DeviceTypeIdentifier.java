/**
 * 
 */
package ai.netoai.collector.snmp.discovery;

import java.io.FileReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ai.netoai.collector.Constants;
import ai.netoai.collector.model.NetworkElement;
import ai.netoai.collector.model.NetworkProtocol;
import ai.netoai.collector.settings.SettingsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.CommunityTarget;
import org.snmp4j.Target;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import com.google.common.base.Stopwatch;
import com.google.gson.Gson;


public class DeviceTypeIdentifier {
	private static final Logger log = LoggerFactory.getLogger(DeviceTypeIdentifier.class);
	private static final String tracePrefix = "[" + DeviceTypeIdentifier.class.getSimpleName() + "]: ";
	
	private static DeviceTypeIdentifier instance;
	private DeviceTypeRepo deviceTypeRepo;
	
	private DeviceTypeIdentifier() {
		log.info(tracePrefix + "init()");
		loadDiscoveryTree();
	}
	
	public synchronized static DeviceTypeIdentifier getInstance() {
		if ( instance == null ) {
			instance = new DeviceTypeIdentifier();
		}
		
		return instance;
	}
	
	private void loadDiscoveryTree() {
		Stopwatch sw = Stopwatch.createStarted();
		Gson gson = new Gson();
		Map<String, Object> collectorSettings = SettingsManager.getInstance().getSettings();
		log.info(tracePrefix + "Collector Settings: " + collectorSettings);
		String discoveryTreePath = null;
		if ( collectorSettings.containsKey("discoveryTreeConfLocation") ) {
			discoveryTreePath = collectorSettings.get("discoveryTreeConfLocation").toString();
		}
		if ( discoveryTreePath == null || discoveryTreePath.isEmpty() ) {
			log.error(tracePrefix + "Failed getting Discovery tree path from collector settings, using default");
			discoveryTreePath = "../conf/discoverytree.json";
		}
		log.info(tracePrefix + "Loading discovery tree from location: " + discoveryTreePath);
		try {
			deviceTypeRepo = gson.fromJson(new FileReader(discoveryTreePath), DeviceTypeRepo.class);
			if ( log.isTraceEnabled() ) {
				deviceTypeRepo.getDeviceTypes().forEach((key, v) -> { 
					log.trace(tracePrefix + "Key: " + key + ", DeviceModel: " + v);
					log.trace(tracePrefix + "Key: " + key + ", Rules: " + v.getRuleList());
				});
			}
			log.info(tracePrefix + "Loaded " + deviceTypeRepo.getDeviceTypes().size() + " deviceType(s) in " + sw.stop());
		} catch (Exception ex) {
			log.error(tracePrefix + "Failed loading discovery tree", ex);
		}
	}
	
	public String identifyNE(NetworkElement ne) {
		Stopwatch sw = Stopwatch.createStarted();
		List<VariableBinding> varbindCache = new ArrayList<>();
		String deviceType = null;
		try {
			Target target = createTarget(ne);
			deviceType = deviceTypeRepo.searchDeviceType(target, varbindCache);
		} catch (Exception ex) {
			log.error(tracePrefix + "Failed identifying the deviceType for ne: " + ne.getIp(), ex);
		}
		log.info(tracePrefix + "Device Type [" + deviceType + "] found for Ne: " + ne.getIp() + ":" + ne.getPort() + " in " + sw.stop());
		return deviceType;
		
	}
	
	private Target createTarget(NetworkElement ne) throws UnknownHostException {
		if ( ne.getProtocol() == NetworkProtocol.SNMPv1 || ne.getProtocol() == NetworkProtocol.SNMPv2c ) {
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

}
