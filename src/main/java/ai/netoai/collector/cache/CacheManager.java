package ai.netoai.collector.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ai.netoai.collector.deviceprofile.SnmpConfig;
import ai.netoai.collector.model.ISNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CacheManager {
	
	private static final Logger log = LoggerFactory.getLogger(CacheManager.class);
	private static final String tracePrefix = "[" + CacheManager.class.getSimpleName() + "]: ";
	private static CacheManager instance;
	
	private CacheManager(){}
	
	// Cache for IP address -> Device type
	private MapCache<String, ISNode> ipDeviceTypecache;
	
	// Cache for Device type -> Device configuration
	private MapCache<String, SnmpConfig> deviceTypeCache;
	
	// Cache for IP address - > Polled or not
	private MapCache<String, List<String>> polledDevices;
	
	// Non reachable devices
	private MapCache<String, Long> nonReachableDevices;
	
	private MapCache<String, List<SnmpInvCacheObject>> invCache;
	
	public static void init() {
		if ( instance != null ) {
			throw new IllegalStateException("CacheManager already initialized");
		}
		instance = new CacheManager();
	}
	
	public static CacheManager getInstance() {
		if ( instance == null ) {
			throw new IllegalStateException("CacheManager not initialized");
		}
		return instance;
	}
	
	public void start() {
		this.ipDeviceTypecache   = new MapCache<>();
		this.deviceTypeCache     = new MapCache<>();
		this.polledDevices       = new MapCache<>();
		this.nonReachableDevices = new MapCache<>();
		this.invCache = new MapCache<>();
		log.info(tracePrefix + "Started ...");
	}
	
	public void stop() {
		this.ipDeviceTypecache.clear();
		this.deviceTypeCache.clear();
		this.polledDevices.clear();
		this.nonReachableDevices.clear();
		this.invCache.clear();
	}
	
	public void invalidateIpDeviceCache() {
		this.ipDeviceTypecache.clear();
	}
	
	public Object getDeviceTypeByIp(String key) {
		return this.ipDeviceTypecache.get(key);
	}
	
	public void putIp(String key, ISNode val) {
		log.info(tracePrefix + "Caching key: " + key + ", Val: " + val );
		this.ipDeviceTypecache.put(key, val);
	}
	
	public SnmpConfig getProfileByDeviceType(String key) {
		return this.deviceTypeCache.get(key);
	}
	
	public void putProfile(String key, SnmpConfig val) {
		log.info(tracePrefix + "DeviceType: Caching key: " + key + ", Val: " + val );
		this.deviceTypeCache.put(key, val);
	}
	
	public void removeProfileByConfig(String key) {
		if ( this.deviceTypeCache.containsKey(key) ) {
			this.deviceTypeCache.remove(key);
		}
	}
	
	public List<String> getAllDeviceIps() {
		return this.ipDeviceTypecache.getAllKeys();
	}
	
	public void storeDevicePollInfo(String ip, List<String> indices) {
		this.polledDevices.put(ip, indices);
	}
	
	public boolean isDevicePolled(String ip) {
		return this.polledDevices.containsKey(ip);
	}
	
	public int getPolledDeviceSize() {
		return this.polledDevices.size();
	}
	
	public List<String> getIndexByDevice(String ip) {
		List<String> list = new ArrayList<String>();
		List<String> value = this.polledDevices.get(ip);
		if ( value != null ) {
			list.addAll(value);
		}
		return list;
	}
	
	public void invalidatePolledDeviceCache() {
		this.polledDevices.clear();
	}
	
	public void invalidatePolledDeviceCache(String ip) {
		this.polledDevices.remove(ip);
	}
	
	public void deviceNotReachable(String ip, long time) {
		this.nonReachableDevices.put(ip, time);
	}
	
	public boolean isDeviceNotReachable(String ip) {
		return this.nonReachableDevices.containsKey(ip);
	}
	
	public long getDeviceDownTime(String ip) {
		if ( this.nonReachableDevices.containsKey(ip)) {
			long time = this.nonReachableDevices.get(ip);
			return time;
		}
		return 0l;
	}
	
	public void cacheInventory(String deviceIp, List<SnmpInvCacheObject> invList) {
		this.invCache.put(deviceIp, invList);
	}
	
	public List<SnmpInvCacheObject> getInventory(String deviceIp) {
		return this.invCache.get(deviceIp);
	}
}
