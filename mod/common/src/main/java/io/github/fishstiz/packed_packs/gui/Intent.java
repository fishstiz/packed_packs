package io.github.fishstiz.packed_packs.gui;

public interface Intent {
    boolean pushState();

    default boolean resetHistory() {
        return false;
    }
}
