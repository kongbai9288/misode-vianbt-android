package net.querz.mcaselector.util.collection;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Dependency-free replacement for
 * {@code it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap}.
 *
 * Insertion ordered, which is what the LRU style region buffer relies on.
 */
public class Long2ObjectLinkedOpenHashMap<V> extends Long2ObjectOpenHashMap<V>
	implements Long2ObjectSortedMap<V>, Serializable {

	private static final long serialVersionUID = 1L;

	private final LinkedHashMap<Long, V> linked;

	public Long2ObjectLinkedOpenHashMap() {
		this.linked = new LinkedHashMap<>();
	}

	public Long2ObjectLinkedOpenHashMap(int expected) {
		super(expected);
		this.linked = new LinkedHashMap<>(Math.max(4, expected));
	}

	public Long2ObjectLinkedOpenHashMap(int expected, float loadFactor) {
		super(expected, loadFactor);
		this.linked = new LinkedHashMap<>(Math.max(4, expected), loadFactor);
	}

	private List<Long> orderedKeys() { return new ArrayList<>(linked.keySet()); }

	@Override public int size() { return linked.size(); }
	@Override public boolean isEmpty() { return linked.isEmpty(); }
	@Override public boolean containsKey(long key) { return linked.containsKey(key); }
	@Override public V get(long key) { return linked.get(key); }
	@Override public V put(long key, V value) { return linked.put(key, value); }
	@Override public V remove(long key) { return linked.remove(key); }
	@Override public void clear() { linked.clear(); }

	@Override
	public Set<Entry<V>> long2ObjectEntrySet() {
		return new AbstractSet<>() {
			@Override
			public Iterator<Entry<V>> iterator() {
				Iterator<Map.Entry<Long, V>> it = linked.entrySet().iterator();
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

			@Override public int size() { return linked.size(); }
		};
	}

	@Override public Collection<V> values() { return new ArrayList<>(linked.values()); }

	@Override
	public LongSet keySet() {
		LongOpenHashSet set = new LongOpenHashSet(linked.size());
		for (Long k : linked.keySet()) set.add(k);
		return set;
	}

	@Override
	public long firstLongKey() {
		List<Long> keys = orderedKeys();
		if (keys.isEmpty()) throw new NoSuchElementException();
		return keys.get(0);
	}

	@Override
	public long lastLongKey() {
		List<Long> keys = orderedKeys();
		if (keys.isEmpty()) throw new NoSuchElementException();
		return keys.get(keys.size() - 1);
	}
}
