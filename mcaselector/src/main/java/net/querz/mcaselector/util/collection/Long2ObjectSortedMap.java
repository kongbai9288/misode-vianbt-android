package net.querz.mcaselector.util.collection;

/** Dependency-free replacement for {@code it.unimi.dsi.fastutil.longs.Long2ObjectSortedMap}. */
public interface Long2ObjectSortedMap<V> extends Long2ObjectMap<V> {
	long firstLongKey();
	long lastLongKey();
}
