package io.github.fishstiz.fidgetz.util.lang;

@FunctionalInterface
public interface ToIntTriFunction<T, U, V> {
    int applyAsInt(T t, U u, V v);
}
