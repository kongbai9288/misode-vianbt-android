package net.querz.mcaselector.util.collection;

/** Dependency-free replacement for {@code it.unimi.dsi.fastutil.shorts.ShortPredicate}. */
@FunctionalInterface
public interface ShortPredicate {
	boolean test(short value);
}
