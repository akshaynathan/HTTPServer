import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class CacheMap<K, V> extends HashMap<K, V> {
	
	private int cap;
	private final int TTL = 30;
	private Map<K, Long> ttls;

	public CacheMap(int capacity) {
		ttls = new HashMap<K, Long>();
		cap = capacity;
	}

	public V put(K key, V value) {
		if (this.size() < this.cap || this.containsKey(key)) {
			super.put(key, value);
			ttls.put(key, new Long(System.currentTimeMillis() + TTL * 1000));
		}
		return null;
	}
	
	public V get(Object key) {
		Long t = ttls.get(key);
		if(t != null && System.currentTimeMillis() > t) // It's expired!
			return null;
		return super.get(key);
	}
}
