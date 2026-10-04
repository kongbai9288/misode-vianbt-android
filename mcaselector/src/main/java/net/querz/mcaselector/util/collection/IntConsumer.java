package net.querz.mcaselector.util.collection;

/**
 * Dependency-free replacement for {@code it.unimi.dsi.fastutil.ints.IntConsumer}.
 */
@FunctionalInterface
public interface IntConsumer {

	void accept(int value);
}
