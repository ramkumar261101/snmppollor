package ai.netoai.collector.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MapCache<K, T> {
	private Map<K, T> cacheObj = new ConcurrentHashMap<>();
	
	public MapCache() {
		
	}
	
	public void put(K key, T value) {
		cacheObj.put(key, value);
	}
	
	public void putAll(Map<K, T> map) {
		cacheObj.putAll(map);
	}
	
	public void remove(String key) {
		cacheObj.remove(key);
	}
	
	public T get(String key) {
		if ( this.cacheObj.containsKey(key)) {
			return this.cacheObj.get(key);
		}
		return null;
	}
	
	public void clear() {
		this.cacheObj.clear();
	}
	
	public boolean isEmpty() {
		if(this.cacheObj != null) {
			return this.cacheObj.isEmpty();
		}
		return true;
	}
	
	public List<K> getAllKeys() {
		if ( this.cacheObj != null ) {
			return new ArrayList<>(cacheObj.keySet());
		}
		return new ArrayList<K>();
	}
	
	public boolean containsKey(String key) {
		if ( this.cacheObj != null && key != null ) {
			return this.cacheObj.containsKey(key);
		}
		return false;
	}
	
	public int size() {
		return this.cacheObj.size();
	}
}
