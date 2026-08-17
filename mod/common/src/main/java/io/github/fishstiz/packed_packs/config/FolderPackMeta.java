package io.github.fishstiz.packed_packs.config;

import java.util.Collections;
import java.util.List;

public record FolderPackMeta(boolean module, List<String> packIds) {
    public static final String FILENAME = "packed_packs.folderpack.json";

    public FolderPackMeta {
        packIds = List.copyOf(packIds);
    }

    public FolderPackMeta() {
        this(true, Collections.emptyList());
    }
}
