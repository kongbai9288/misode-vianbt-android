package net.querz.mcaselector.util.collection;

import java.util.PrimitiveIterator;

/**
 * Dependency-free replacement for {@code it.unimi.dsi.fastutil.longs.LongSet}.
 *
 * {@link #iterator()} returns a {@link PrimitiveIterator.OfLong} so that the
 * {@code for (long x : set)} loops used throughout the code base keep working
 * through auto-unboxing.
 */
public interface LongSet extends Iterable<Long> {

	int size();

	boolean isEmpty();

	boolean contains(long key);

	boolean add(long key);

	boolean remove(long key);

	void clear();

	@Override
	PrimitiveIterator.OfLong iterator();

	long[] toLongArray();
}
