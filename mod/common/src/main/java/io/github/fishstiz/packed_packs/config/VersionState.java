package io.github.fishstiz.packed_packs.config;

import io.github.fishstiz.packed_packs.platform.Services;
import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public final class VersionState {
    /**
     * <ol>
     *     <li>1 - Added version state file.</li>
     *     <li>2 - Added FolderPackMeta#module and removed folder id prefix in nested regular packs.</li>
     * </ol>
     */
    private static final int CURRENT_VERSION = 2;
    private static final String FILENAME = "__version.json";
    private static final VersionState INSTANCE;

    static {
        INSTANCE = JsonLoader.loadOrDefault(getPath(), VersionState.class, VersionState::new);
        INSTANCE.previousVersion = INSTANCE.version;
        INSTANCE.version = CURRENT_VERSION;

        if (INSTANCE.previousVersion == 1 && INSTANCE.migrateLegacyFolders == null) {
            LoggerFactory.getLogger("packed_packs").info(
                    "[packed_packs] Detecting an update from legacy folder packs. Enabling auto migration..."
            );
            INSTANCE.migrateLegacyFolders = true;
        }

        if (INSTANCE.previousVersion != CURRENT_VERSION) {
            JsonLoader.saveJson(INSTANCE, getPath());
        }
    }

    private int version;
    private transient int previousVersion;
    private @Nullable Boolean migrateLegacyFolders;

    private static Path getPath() {
        // avoid loading PackedPacks
        return Services.PLATFORM.getConfigDir().resolve("packed_packs").resolve(FILENAME);
    }

    public static int getPreviousVersion() {
        return INSTANCE.previousVersion;
    }

    public static int getVersion() {
        return INSTANCE.version;
    }

    public static boolean shouldMigrateLegacyFolders() {
        return Boolean.TRUE.equals(INSTANCE.migrateLegacyFolders);
    }

    private VersionState() {
    }
}
