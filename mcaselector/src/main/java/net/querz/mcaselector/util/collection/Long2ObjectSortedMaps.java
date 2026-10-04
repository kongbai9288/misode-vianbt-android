package net.querz.mcaselector.util.collection;

import java.util.Collection;
import java.util.Set;

/** Dependency-free replacement for {@code it.unimi.dsi.fastutil.longs.Long2ObjectSortedMaps}. */
public final class Long2ObjectSortedMaps {

	private Long2ObjectSortedMaps() {}

	/** Wraps the map so every mutating call synchronises on [lock]. */
	public static <V> Long2ObjectSortedMap<V> synchronize(
		final Long2ObjectSortedMap<V> map, final Object lock) {
		return new Synchronized<>(map, lock);
	}

	private static class Synchronized<V> implements Long2ObjectSortedMap<V> {

		private final Long2ObjectSortedMap<V> delegate;
		private final Object lock;

		Synchronized(Long2ObjectSortedMap<V> delegate, Object lock) {
			this.delegate = delegate;
			this.lock = lock;
		}

		@Override public int size() { synchronized (lock) { return delegate.size(); } }
		@Override public boolean isEmpty() { synchronized (lock) { return delegate.isEmpty(); } }
		@Override public boolean containsKey(long key) { synchronized (lock) { return delegate.containsKey(key); } }
		@Override public V get(long key) { synchronized (lock) { return delegate.get(key); } }
		@Override public V put(long key, V value) { synchronized (lock) { return delegate.put(key, value); } }
		@Override public V remove(long key) { synchronized (lock) { return delegate.remove(key); } }
		@Override public void clear() { synchronized (lock) { delegate.clear(); } }
		@Override public Set<Entry<V>> long2ObjectEntrySet() { synchronized (lock) { return delegate.long2ObjectEntrySet(); } }
		@Override public Collection<V> values() { synchronized (lock) { return delegate.values(); } }
		@Override public LongSet keySet() { synchronized (lock) { return delegate.keySet(); } }
		@Override public long firstLongKey() { synchronized (lock) { return delegate.firstLongKey(); } }
		@Override public long lastLongKey() { synchronized (lock) { return delegate.lastLongKey(); } }
	}
}
