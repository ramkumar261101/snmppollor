package ai.netoai.collector.convert;

import ai.netoai.collector.deviceprofile.ConfigManager;
import ai.netoai.collector.deviceprofile.SnmpConfig;
import ai.netoai.collector.deviceprofile.TrapConfig;
import ai.netoai.collector.model.EndPoint;
import ai.netoai.collector.model.NetworkElement;
import ai.netoai.collector.snmp.discovery.NodeManager;
import ai.netoai.collector.cache.NodeCacheManager;
import ai.netoai.collector.snmp.WrappedCREvent;
import ai.netoai.collector.snmp.trap.SnmpTrap;
import com.google.gson.Gson;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;


public class ConvertManagerHelper {
    private static final Logger log = LoggerFactory.getLogger(ConvertManagerHelper.class);
    private static final String tracePrefix = "[" + ConvertManagerHelper.class.getName()+ "] : ";
    
    private static ConvertManagerHelper instance;
    private ExecutorService trapConvertExecutor;
    private int executorThreads = 20;
    
    private ConvertManagerHelper() {
        init();
    };
    
    public static ConvertManagerHelper getInstance() {
        if(instance == null) {
            instance = new ConvertManagerHelper();
        }
        return instance;
    }
    
    private void init() {
        trapConvertExecutor = Executors.newFixedThreadPool(executorThreads);
    }
    
    public void processTrapEvents(WrappedCREvent wEvent) {
        log.info("Processing Trap Event : " + wEvent);
        CommandResponderEvent event = wEvent.getEvent();
        long recTime = wEvent.getReceivedTime();
        SnmpTrap trap = wEvent.getTrap();
        trapConvertExecutor.submit(new Runnable() {
            @Override
            public void run() {
                String peerAddress = null;
                if ( trap == null ) {
                    PDU trapPdu = event.getPDU();
                    log.info(tracePrefix + trapPdu.getType() + ", address : " + event.getPeerAddress());
                    if (trapPdu.getType() == PDU.V1TRAP) {
                        PDUv1 trapPduV1 = (PDUv1) trapPdu;
                        peerAddress = trapPduV1.getAgentAddress().toString();
                    } else if (trapPdu.getType() == PDU.TRAP || trapPdu.getType() == PDU.NOTIFICATION) {
                        String tmp = event.getPeerAddress().toString();
                        peerAddress = tmp.substring(0, tmp.indexOf('/'));
                    } else {
                        log.error(tracePrefix + " Not handling PDU type : " + trapPdu.getTypeString(trapPdu.getType()));
                        return;
                    }
                } else {
                    log.info(tracePrefix + "Trap is not null ...");
                    peerAddress = trap.getAgentAddress();
                }
                log.info(tracePrefix + " PeerAddress : " + peerAddress);
                
                NetworkElement node = NodeManager.getInstance().getNetworkElementByIp(peerAddress);
                if ( node == null ) {
                    log.error(tracePrefix + " No Device Found for ip : " + peerAddress);
                    return;
                }
                
                List<EndPoint> endPointL = NodeCacheManager.getInstance().getEndPoints(node);
                EndPoint nodeEndPoint = null;
                for (EndPoint ep : endPointL) {
                    if(ep.getSourceId().equals(node.getIp())){
                        nodeEndPoint = ep;
                        break;
                    }
                }
                
                if ( nodeEndPoint == null) {
                    log.error(tracePrefix + " No EndPoint Device Found for ip : " + peerAddress);
                    return;
                }
                
                SnmpConfig snmpConfig = ConfigManager.getInstance().getConfigObjectForDevice(node.getProfileId());
                if ( snmpConfig == null ) {
                    log.error(tracePrefix + " No Config File found for profile : " + node.getProfileId());
                    return;
                }
                List<TrapConfig> trapConfigL = snmpConfig.getTrapConfigs();
                if ( trapConfigL == null ) {
                   log.error(tracePrefix + " No Trap Config found for profile : " + node.getProfileId());
                   return;
                }
                SnmpTrap snmpTrap = null;
                if ( trap == null ) {
                    snmpTrap = new SnmpTrap(event.getPDU());
                    snmpTrap = snmpTrap.construct();
                    snmpTrap.setAgentAddress(peerAddress);
                } else {
                    snmpTrap = trap;
                }
                
                if ( log.isDebugEnabled() ) {
                    log.info(tracePrefix + " Trap Constructed : " + snmpTrap);
                }
            }
        });
    }
    

}
