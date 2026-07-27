package io.github.fishstiz.packed_packs.transform.interfaces;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public interface FilePack {
    default boolean packed_packs$nestedPack() {
        return false;
    }

    default void packed_packs$setNestedPack(boolean nested) {
    }

    default void packed_packs$setPath(Path path) {
    }

    default @Nullable Path packed_packs$getPath() {
        return null;
    }
}
