package net.querz.mcaselector.util.collection;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Dependency-free replacement for
 * {@code it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap}.
 *
 * Backed by {@link HashMap}; primitive long keys are boxed, which costs a
 * little allocation per lookup but keeps fastutil (several hundred KB) out of
 * the APK.
 */
public class Long2ObjectOpenHashMap<V> implements Long2ObjectMap<V>, Iterable<Long2ObjectMap.Entry<V>>, Serializable {

	private static final long serialVersionUID = 1L;

	protected final HashMap<Long, V> map;

	public Long2ObjectOpenHashMap() { this.map = new HashMap<>(); }

	public Long2ObjectOpenHashMap(int expected) { this.map = new HashMap<>(Math.max(4, expected)); }

	public Long2ObjectOpenHashMap(int expected, float loadFactor) {
		this.map = new HashMap<>(Math.max(4, expected), loadFactor);
	}

	@Override public int size() { return map.size(); }
	@Override public boolean isEmpty() { return map.isEmpty(); }
	@Override public boolean containsKey(long key) { return map.containsKey(key); }
	@Override public V get(long key) { return map.get(key); }
	@Override public V put(long key, V value) { return map.put(key, value); }
	@Override public V remove(long key) { return map.remove(key); }
	@Override public void clear() { map.clear(); }

	public V getOrDefault(long key, V defaultValue) {
		V v = map.get(key);
		return v == null && !map.containsKey(key) ? defaultValue : v;
	}

	@Override
	public Set<Entry<V>> long2ObjectEntrySet() {
		return new AbstractSet<>() {
			@Override
			public Iterator<Entry<V>> iterator() {
				Iterator<Map.Entry<Long, V>> it = map.entrySet().iterator();
				return new Iterator<>() {
					@Override public boolean hasNext() { return it.hasNext(); }
					@Override
					public Entry<V> next() {
						Map.Entry<Long, V> e = it.next();
						return new Entry<>() {
							@Override public long getLongKey() { return e.getKey(); }
							@Override public V getValue() { return e.getValue(); }
							@Override public V setValue(V value) {
								V old = e.getValue();
								e.setValue(value);
								return old;
							}
						};
					}
				};
			}

			@Override public int size() { return map.size(); }
		};
	}

	@Override public Collection<V> values() { return new ArrayList<>(map.values()); }

	@Override
	public LongSet keySet() {
		LongOpenHashSet set = new LongOpenHashSet(map.size());
		for (Long key : map.keySet()) set.add(key);
		return set;
	}

	/** Shallow copy; Selection#subtract relies on it. */
	@SuppressWarnings("unchecked")
	@Override
	public Long2ObjectOpenHashMap<V> clone() {
		Long2ObjectOpenHashMap<V> copy = new Long2ObjectOpenHashMap<>(map.size());
		copy.map.putAll(map);
		return copy;
	}

	/** Lets {@code for (Entry<V> e : map)} work. */
	@Override
	public java.util.Iterator<Entry<V>> iterator() {
		return long2ObjectEntrySet().iterator();
	}

	@Override public String toString() { return map.toString(); }
}
