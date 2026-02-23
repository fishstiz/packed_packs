package io.github.fishstiz.packed_packs.gui;

public sealed interface FocusTarget {
    boolean scroll();

    record LastSelected(boolean scroll) implements FocusTarget {
    }

    record PackEntry(String packId, boolean scroll) implements FocusTarget {
    }
}
