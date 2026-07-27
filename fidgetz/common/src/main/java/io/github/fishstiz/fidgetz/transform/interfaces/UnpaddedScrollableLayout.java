package io.github.fishstiz.fidgetz.transform.interfaces;

public interface UnpaddedScrollableLayout {
    default void fidgetz$setUnpadded(boolean unpadded) {
    }

    default boolean fidgetz$unpadded() {
        return false;
    }
}
