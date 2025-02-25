/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.netoai.collector.snmp.discovery;

import ai.netoai.collector.model.DiscoveryTask;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DiscoveryManager {
    
    private static final Logger log = LoggerFactory.getLogger(DiscoveryManager.class);
    private static final String tracePrefix = "[" + DiscoveryManager.class.getSimpleName() + "]: ";
    
    private Map<String, Discovery> discoveryTaskMap = new ConcurrentHashMap<>();
    
    private DiscoveryManager() {
        log.info(tracePrefix + "Initializing Discovery Manager");
    }

    private static class DiscoveryManagerHelper {
        public static final DiscoveryManager INSTANCE = new DiscoveryManager();
    }
    
    public static DiscoveryManager getInstance() {
        return DiscoveryManagerHelper.INSTANCE;
    }
    
    public void startDiscovery(DiscoveryTask dt) {
        if ( dt.getAuthProfiles() == null || dt.getAuthProfiles().isEmpty() ) {
            log.error(tracePrefix + "Auth profiles not found for discovery task: " + dt.getName());
            return;
        }
        
        // Currently only SNMP Discovery is supported.
        Discovery discovery = new SnmpDiscovery(dt);
        this.discoveryTaskMap.put(dt.getUuid(), discovery);
        discovery.start();
    }
    
}
