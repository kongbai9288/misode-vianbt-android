package net.querz.mcaselector.util.collection;

import java.util.Collection;
import java.util.Set;

/**
 * Dependency-free replacement for {@code it.unimi.dsi.fastutil.longs.Long2ObjectMap}.
 */
public interface Long2ObjectMap<V> {

	interface Entry<V> {

		long getLongKey();

		V getValue();

		V setValue(V value);
	}

	int size();

	boolean isEmpty();

	boolean containsKey(long key);

	V get(long key);

	V put(long key, V value);

	V remove(long key);

	void clear();

	Set<Entry<V>> long2ObjectEntrySet();

	Collection<V> values();

	LongSet keySet();
}
