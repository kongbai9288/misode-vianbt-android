package net.querz.mcaselector.util.collection;

import java.util.PrimitiveIterator;

/**
 * Dependency-free replacement for {@code it.unimi.dsi.fastutil.ints.IntIterator}.
 *
 * Extends {@link PrimitiveIterator.OfInt} so the {@code for (int i : chunkSet)}
 * loops used by the selection code keep compiling through auto-unboxing.
 * {@code forEachRemaining} is overloaded for the fastutil style IntConsumer.
 */
public interface IntIterator extends PrimitiveIterator.OfInt {

	int nextInt();

	default int skip(int n) {
		int remaining = n;
		while (remaining-- > 0 && hasNext()) {
			nextInt();
		}
		return n - remaining - 1;
	}

	default void forEachRemaining(IntConsumer action) {
		while (hasNext()) {
			action.accept(nextInt());
		}
	}
}
