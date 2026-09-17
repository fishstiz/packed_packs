package io.github.fishstiz.packed_packs.config;

import io.github.fishstiz.packed_packs.PackedPacks;

import java.nio.file.Path;

public final class VersionState {
    public static final int CURRENT_VERSION = 1;
    private static final String FILENAME = "__version.json";
    private static final VersionState INSTANCE;

    static {
        INSTANCE = JsonLoader.loadOrDefault(getPath(), VersionState.class, VersionState::new);
        INSTANCE.previousVersion = INSTANCE.version;
        INSTANCE.version = CURRENT_VERSION;

        if (INSTANCE.previousVersion != CURRENT_VERSION) {
            JsonLoader.saveJson(INSTANCE, getPath());
        }
    }

    private int version;
    private transient int previousVersion;

    private static Path getPath() {
        return PackedPacks.getConfigDir().resolve(FILENAME);
    }

    public static int getPreviousVersion() {
        return INSTANCE.previousVersion;
    }

    public static int getVersion() {
        return INSTANCE.version;
    }

    private VersionState() {
    }
}
