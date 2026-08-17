package io.github.fishstiz.packed_packs.gui;

// an intent may result in a state mutation
public interface Intent {
    boolean pushState();

    default boolean resetHistory() {
        return false;
    }
}
