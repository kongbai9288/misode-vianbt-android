package net.querz.mcaselector.util.collection;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PrimitiveIterator;
import java.util.Set;

/** Dependency-free replacement for {@code it.unimi.dsi.fastutil.longs.LongOpenHashSet}. */
public class LongOpenHashSet implements LongSet, Serializable {

	private static final long serialVersionUID = 1L;

	private final HashSet<Long> set;

	public LongOpenHashSet() { this.set = new HashSet<>(); }

	public LongOpenHashSet(int expected) { this.set = new HashSet<>(Math.max(4, expected)); }

	public LongOpenHashSet(int expected, float loadFactor) {
		this.set = new HashSet<>(Math.max(4, expected), loadFactor);
	}

	/** Replacement for {@code LongOpenHashSet.of()}. */
	public static LongOpenHashSet of() { return new LongOpenHashSet(); }

	/** Replacement for {@code LongOpenHashSet.of(long...)}. */
	public static LongOpenHashSet of(long... values) {
		LongOpenHashSet set = new LongOpenHashSet(values.length);
		for (long v : values) set.add(v);
		return set;
	}

	@Override public int size() { return set.size(); }
	@Override public boolean isEmpty() { return set.isEmpty(); }
	@Override public boolean contains(long key) { return set.contains(key); }
	@Override public boolean add(long key) { return set.add(key); }
	@Override public boolean remove(long key) { return set.remove(key); }
	@Override public void clear() { set.clear(); }

	@Override
	public PrimitiveIterator.OfLong iterator() {
		Iterator<Long> it = set.iterator();
		return new PrimitiveIterator.OfLong() {
			@Override public long nextLong() { return it.next(); }
			@Override public boolean hasNext() { return it.hasNext(); }
		};
	}

	@Override
	public long[] toLongArray() {
		long[] result = new long[set.size()];
		int i = 0;
		for (Long v : set) result[i++] = v;
		return result;
	}

	public Set<Long> asSet() { return set; }

	@Override public String toString() { return set.toString(); }
}
