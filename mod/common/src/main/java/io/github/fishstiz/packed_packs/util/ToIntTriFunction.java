package io.github.fishstiz.packed_packs.util;

@FunctionalInterface
public interface ToIntTriFunction<T, U, V> {
    int applyAsInt(T t, U u, V v);
}
