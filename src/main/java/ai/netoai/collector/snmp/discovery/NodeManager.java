package ai.netoai.collector.snmp.discovery;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


import ai.netoai.collector.model.NetworkElement;
import ai.netoai.collector.settings.SnmpSettings;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.netoai.collector.cache.NodeCacheManager;
import ai.netoai.collector.startup.CollectorMain;

public class NodeManager {

    private static final Logger log = LoggerFactory.getLogger(NodeManager.class);
    private static final String tracePrefix = "[" + NodeManager.class.getSimpleName() + "]: ";
    private static NodeManager instance;
    private SnmpSettings snmpSettings;
    private Map<String, NodeAdapter> networkElementsMap = new ConcurrentHashMap<String, NodeAdapter>();
    private ScheduledExecutorService mainExecutor = Executors.newScheduledThreadPool(1);
    private ScheduledExecutorService invResyncExecutor = Executors.newScheduledThreadPool(10);

    private NodeManager() {
        snmpSettings = new SnmpSettings();
    }

    public static synchronized NodeManager getInstance() {
        if (instance == null) {
            instance = new NodeManager();
        }
        return instance;
    }

    public Set<NodeAdapter> getNetworkElementAdapters(){
        Map<NetworkElement,NodeAdapter> adapterMap = new HashMap<>();
        Collection<NodeAdapter> values = this.networkElementsMap.values();
        Set<NodeAdapter> adapters = new HashSet<>(values);
        return adapters;
    }
    
    public Set<NetworkElement> getNetworkElements() {
        Collection<NodeAdapter> values = this.networkElementsMap.values();
        Set<NetworkElement> elements = new HashSet<>();
        for (NodeAdapter node : values) {
            elements.add(node.getNetworkelement());
        }
        return elements;
    }
    
    public NetworkElement getNetworkElementByIp(String ip) {
        NetworkElement element = null;
            for(NetworkElement ne : getNetworkElements()) {
                if(ne.getIp().equals(ip)) {
                    element = ne;
                }
            }
        return element;
    }
        
     public NetworkElement getNetworkElement(String ip) {
        NetworkElement element = null;
        for(NetworkElement ne : getNetworkElements()) {
            if(ne.getIp().equals(ip)) {
                element = ne;
                break;
            }
        }
        return element;
    }

    public NetworkElement getNetworkElementByName(String name) {
        NetworkElement element = null;
        for(NetworkElement ne : getNetworkElements()) {
            if( StringUtils.equalsIgnoreCase(ne.getName(), name) ) {
                element = ne;
                break;
            }
        }
        return element;
    }

    public void updateNetworkElements() {
        // TODO: Need to query from the stored state to populate the map of network elements in the memory
        log.info("Need to query the network elements stored in the DB and load in the memory ...");
        List<NetworkElement> elements = new ArrayList<>();
        for (NetworkElement ne : elements) {
            String id = ne.getIp();
            NodeAdapter adapter = new NodeAdapter(ne);
            instance.networkElementsMap.put(id, adapter);
        }
    }

    public void addNetworkElement(NetworkElement ne) {
         log.info("Adding Network Element: " + ne.getIp());
        String id = ne.getIp();
        if (networkElementsMap.containsKey(id)) {
            NodeAdapter adapter = networkElementsMap.get(id);
            adapter.setCurrentState(adapter.getUnknownStateObj());
            /*return;*/
        } else {
            NodeAdapter adapter = new NodeAdapter(ne);
            networkElementsMap.put(id, adapter);
        }
    }

    public void start() {
        mainExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                log.debug(tracePrefix + "Total Network Elements Discovered " + networkElementsMap.size());
                // log.info("*************** MAIN THREAD RUN
                // ******************");
                for (Map.Entry<String, NodeAdapter> entry : networkElementsMap.entrySet()) {

                    log.debug(tracePrefix + " Node: " + entry.getValue().getNetworkelement()
                            + ", Current State: " + entry.getValue().getNetworkelement().getConnState());

                    switch (entry.getValue().getNetworkelement().getConnState()) {
                        case UNKNOWN:
                            entry.getValue().doNoOperation();
                            break;
                        case CONNECTING:
                            entry.getValue().checkConnection();
                            break;
                        case CONNECTED:
                            entry.getValue().doNoOperation();
                            break;
                        case VERIFYING:
                            entry.getValue().doDiscovery();
                            break;
                        case SYNC_INV:
                            entry.getValue().doInvSync();
                            break;
                        case SYNCED:
                            entry.getValue().checkConnection();
                            break;
                        case DISCONNECTING:
                            entry.getValue().doCleanUp();
                            break;
                        case DISCONNECTED:
                            entry.getValue().checkConnection();
                            break;
                    }
                }
            }
        }, 0, 1, TimeUnit.SECONDS);

        int invSyncInterval = snmpSettings.getInventoryReSyncInterval();
        invResyncExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                log.info(tracePrefix + " Inventory ReSynching ..");
                for (Map.Entry<String, NodeAdapter> entry : networkElementsMap.entrySet()) {
                    NetworkElement ne = entry.getValue().getNetworkelement();
                    if (!ne.getConnState().equals(NetworkElement.State.DISCONNECTED)
                            && !ne.getConnState().equals(NetworkElement.State.DISCONNECTING)) {
                        entry.getValue().setCurrentState(entry.getValue().getVerifyingStateObj());
                    }
                }
            }
        }, invSyncInterval, invSyncInterval, TimeUnit.HOURS);
    }

    public void removeNodes(String[] ids) {
        log.info(tracePrefix + "Removing the following nodes: " + Arrays.toString(ids));
        for(String id : ids) {
            if ( this.networkElementsMap.containsKey(id) ) {
                NodeAdapter removedAdapter = this.networkElementsMap.remove(id);
                NodeCacheManager.getInstance().invalidate(removedAdapter.getNetworkelement());
            } else {
                log.warn(tracePrefix + "Node: " + id + ", Not present in the current map: " + this.networkElementsMap.keySet());
            }
        }
    }

}
