package net.querz.mcaselector.util.collection;

/**
 * Dependency-free replacement for {@code it.unimi.dsi.fastutil.ints.IntIterable}.
 *
 * Extends {@link Iterable} of Integer so enhanced for loops work; the iterator
 * is narrowed to {@link IntIterator} which satisfies both.
 */
public interface IntIterable extends Iterable<Integer> {

	@Override
	IntIterator iterator();

	default void forEach(IntConsumer action) {
		IntIterator it = iterator();
		while (it.hasNext()) {
			action.accept(it.nextInt());
		}
	}
}
