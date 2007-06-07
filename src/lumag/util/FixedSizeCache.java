package lumag.util;

import java.util.Map;

public class FixedSizeCache<K, V> {
	private static final int DEFAULT_MAX_SIZE = 10;

	private Map.Entry<K, V>[] entries;
	int maxSize;
	int size;
	
	public FixedSizeCache(int size) {
		setMaxSize(size);
	}
	
	public FixedSizeCache() {
		this(DEFAULT_MAX_SIZE);
	}

	public V get(K key) {
		if (key == null) {
			return getNullKey();
		}
		return getNonNullKey(key);
	}

	private V getNullKey() {
		Map.Entry<K, V> entry = entries[0];
		if (entry == null || entry.getKey() != null) {
			return null;
		}
		
		return entry.getValue();
	}
	
	private V getNonNullKey(K key) {
		int pos = key.hashCode() % maxSize;
		Map.Entry<K, V> entry = entries[pos];
		
		if (entry == null || ! key.equals(entry.getKey())) {
			return null;
		}

		return entry.getValue();
	}
	
	public void add(K key, V value) {
		int pos = key.hashCode() % maxSize;
		Map.Entry<K, V> entry = entries[pos];
		
		entries[pos] = new SimpleEntry<K, V>(key, value);

		if (entry == null) {
			size ++;
		}
	}
	
	@SuppressWarnings("unchecked")
	public void setMaxSize(int newMaxSize) {
		Map.Entry<K, V>[] newEntries = new Map.Entry[newMaxSize];
		int newSize = 0;

		if (entries != null) {
			for (Map.Entry<K, V> en: entries) {
				int pos = en.hashCode() % newMaxSize;
				if (newEntries[pos] == null) {
					newSize ++;
				}
				newEntries[pos] = en;
			}
		}
		
		entries = newEntries;
		maxSize = newMaxSize;
		size = newSize;
	}
}
