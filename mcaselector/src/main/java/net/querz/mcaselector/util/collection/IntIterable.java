package net.querz.mcaselector.util.collection;

/** Dependency-free replacement for {@code it.unimi.dsi.fastutil.ints.IntIterable}. */
public interface IntIterable {

	IntIterator iterator();

	default void forEach(IntConsumer action) {
		IntIterator it = iterator();
		while (it.hasNext()) {
			action.accept(it.nextInt());
		}
	}
}
