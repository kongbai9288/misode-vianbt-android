package net.querz.mcaselector.util.collection;

/**
 * Dependency-free replacement for {@code it.unimi.dsi.fastutil.ints.IntIterator}.
 *
 * Deliberately does not extend {@link java.util.PrimitiveIterator.OfInt}: that
 * forces {@code forEachRemaining(java.util.function.IntConsumer)}, which clashes
 * with the fastutil flavour of IntConsumer used across the code base.
 */
public interface IntIterator {

	boolean hasNext();

	int nextInt();

	default Integer next() {
		return nextInt();
	}

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
