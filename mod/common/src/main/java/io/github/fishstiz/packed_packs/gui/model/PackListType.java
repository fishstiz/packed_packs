package io.github.fishstiz.packed_packs.gui.model;

public enum PackListType {
    AVAILABLE,
    ENABLED;

    public boolean available() {
        return this == AVAILABLE;
    }

    public boolean enabled() {
        return this == ENABLED;
    }

    public PackListType other() {
        return this == AVAILABLE ? ENABLED : AVAILABLE;
    }
}
