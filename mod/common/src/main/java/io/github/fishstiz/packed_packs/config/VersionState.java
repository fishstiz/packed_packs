package io.github.fishstiz.packed_packs.config;

import io.github.fishstiz.packed_packs.PackedPacks;

import java.nio.file.Path;

public final class VersionState {
    public static final int CURRENT_VERSION = 1;
    private static final String FILENAME = "__version.json";
    private static final VersionState INSTANCE = JsonLoader.loadOrCreate(getPath(), VersionState.class, () -> {
        VersionState version = new VersionState();
        version.previousVersion = 0;
        return version;
    });

    private int version = CURRENT_VERSION;
    private transient int previousVersion = version;

    private static Path getPath() {
        return PackedPacks.getConfigDir().resolve(FILENAME);
    }

    public static int getVersion() {
        return INSTANCE.version;
    }

    public static boolean uninitialized() {
        return INSTANCE.previousVersion == 0;
    }

    public static void update() {
        if (INSTANCE.version != CURRENT_VERSION) {
            INSTANCE.previousVersion = INSTANCE.version;
            INSTANCE.version = CURRENT_VERSION;
            JsonLoader.saveJson(INSTANCE, getPath());
        }
    }

    private VersionState() {
    }
}
