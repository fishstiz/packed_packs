package io.github.fishstiz.fidgetz.util.lang;

@FunctionalInterface
public interface ObjectIntBiConsumer<T> {
    void accept(T obj, int value);
}