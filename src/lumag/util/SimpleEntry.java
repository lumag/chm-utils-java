/**
 * 
 */
package lumag.util;

import java.util.Map;

class SimpleEntry<K, V> implements Map.Entry<K, V> {
	private K key;
	private V value;
	private int hash;

	public SimpleEntry(K key, V value) {
		this.key = key;
		this.value = value;
	}

	public K getKey() {
		return key;
	}

	public V getValue() {
		return value;
	}

	public V setValue(V value) {
		V oldValue = this.value;
		this.value = value;
		return oldValue;
	}

	@Override
	public int hashCode() {
		if (hash != 0) {
			return hash;
		}

		if (key == null) {
			return 0;
		}
		hash = key.hashCode();
		return hash;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof SimpleEntry) {
			SimpleEntry<?, ?> e1 = (SimpleEntry) o;

			if (key == null) {
				if (e1.key != null) {
					return false;
				}
			} else {
				if (!key.equals(e1.key)) {
					return false;
				}
			}
			
			if (value == null) {
				if (e1.value != null) {
					return false;
				}
				return true;
			}
			return value.equals(e1.value);
		}
		return false;
	}
}
