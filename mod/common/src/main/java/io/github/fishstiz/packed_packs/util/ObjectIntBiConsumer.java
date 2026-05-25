package io.github.fishstiz.packed_packs.util;

@FunctionalInterface
public interface ObjectIntBiConsumer<T> {
    void accept(T obj, int num);
}
