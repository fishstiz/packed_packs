package io.github.fishstiz.packed_packs.transform.interfaces;

import io.github.fishstiz.packed_packs.pack.FolderLocationInfo;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

public interface FilePack {
    default @Nullable FolderLocationInfo packed_packs$getParent() {
        return null;
    }

    default void packed_packs$setParent(FolderLocationInfo parent) {
    }

    default void packed_packs$setPath(Path path) {
    }

    default @Nullable Path packed_packs$getPath() {
        return null;
    }
}
