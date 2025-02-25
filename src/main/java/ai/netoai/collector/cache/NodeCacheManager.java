package ai.netoai.collector.cache;

import ai.netoai.collector.model.EndPoint;
import ai.netoai.collector.model.NetworkElement;
import ai.netoai.collector.settings.SettingsManager;
import ai.netoai.collector.snmp.discovery.NodeManager;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.*;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NodeCacheManager {

    private static final Logger log = LoggerFactory.getLogger(NodeCacheManager.class);
    private static final String tracePrefix = "" + NodeCacheManager.class.getSimpleName() + " : ";
    private static NodeCacheManager instance;
    private LoadingCache<NetworkElement, List<EndPoint>> endPoints;

    private NodeCacheManager() {
        SettingsManager sm = SettingsManager.getInstance();
        start();
    }

    public static synchronized NodeCacheManager getInstance() {
        if (instance == null) {
            instance = new NodeCacheManager();
        }
        return instance;
    }

    public void start() {
        endPoints = CacheBuilder.newBuilder()
                .build(new CacheLoader<NetworkElement, List<EndPoint>>() {
                    @Override
                    public List<EndPoint> load(NetworkElement ne) throws Exception {
                        return Collections.emptyList();
                    }
                });
        log.info(tracePrefix + "NodeCacheManager Started");
    }

    public void cacheEndpoints(NetworkElement ne, List<EndPoint> endpoints) {
        this.endPoints.put(ne, endpoints);
        log.info("{}Cached endpoints for: {}", tracePrefix, ne);
    }

    public void stop() {
        if (endPoints != null) {
            endPoints.cleanUp();
        }
        log.info(tracePrefix + "NodeCacheManager Stopped");
    }

    public void invalidate(NetworkElement ne) {
        this.endPoints.invalidate(ne);
        log.info(tracePrefix + "Invalidated Cache for Node: " + ne);
    }
    
    public void invalidate(ArrayList<EndPoint> epList) {
        Set<NetworkElement> neKeys = new HashSet<>();
        Set<String> strKeys = new HashSet<>();
        
        for(EndPoint ep : epList) {
            NetworkElement ne = new NetworkElement();
            ne.setIp(ep.getNetworkElementIp());
            neKeys.add(ne);
        }
        
        this.endPoints.invalidateAll(neKeys);
        log.info(tracePrefix + "Invalidated Cache for EndPoints: " + epList);
    }

    /**
     * get endpoints linked with the network element provided if not returns
     * empty list.
     *
     * @param ne network element for which end points to be returned
     *
     * @return List of end points for the network element
	 *
     */
    public List<EndPoint> getEndPoints(NetworkElement ne) {
        List<EndPoint> endPoints = null;
        try {
            endPoints = this.endPoints.get(ne);
        } catch (ExecutionException ex) {
            log.error(tracePrefix + "Failed fetching endPoints from Cache for NE: " + ne.getName(), ex);
        }
        if (endPoints == null) {
            endPoints = new ArrayList<>();
        }
        return endPoints;
    }
    

}
