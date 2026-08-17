package io.github.fishstiz.packed_packs.gui.states;

public enum ProfileScope {
    NONE,
    LOCAL,
    GLOBAL,
    COMPOSITE;

    public boolean exists() {
        return this != NONE;
    }

    public boolean global() {
        return this == GLOBAL || this == COMPOSITE;
    }
}
