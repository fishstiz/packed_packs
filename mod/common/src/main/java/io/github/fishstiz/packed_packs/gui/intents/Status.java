package io.github.fishstiz.packed_packs.gui.intents;

public enum Status {
    LOADING,
    SUCCESS,
    FAILURE;

    public boolean loading() {
        return this == LOADING;
    }

    public boolean success() {
        return this == SUCCESS;
    }

    public boolean failure() {
        return this == FAILURE;
    }
}
