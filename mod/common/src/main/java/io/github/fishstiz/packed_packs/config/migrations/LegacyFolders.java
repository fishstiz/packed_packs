package io.github.fishstiz.packed_packs.config.migrations;

import io.github.fishstiz.packed_packs.config.JsonLoader;
import io.github.fishstiz.packed_packs.platform.Services;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Set;

public final class LegacyFolders {
    private static final LegacyFolders INSTANCE = JsonLoader.loadOrDefault(getPath(), LegacyFolders.class, LegacyFolders::new);
    private @Nullable Boolean migratedResources;
    private @Nullable Set<String> migratedData;
    private transient boolean dirty;

    private static Path getPath() {
        // avoid loading PackedPacks
        return Services.PLATFORM.getConfigDir()
                .resolve("packed_packs")
                .resolve("migrations")
                .resolve("legacyfolders.json");
    }

    public static boolean isResourcePacksMigrated() {
        return Boolean.TRUE.equals(INSTANCE.migratedResources);
    }

    public static boolean flagResourcePacksMigrated() {
        Boolean migrated = INSTANCE.migratedResources;
        INSTANCE.migratedResources = true;
        boolean changed = migrated == null || !migrated;
        INSTANCE.dirty = changed;
        return changed;
    }

    public static synchronized boolean flagDataPackMigrated(String pack) {
        if (INSTANCE.migratedData == null) {
            INSTANCE.migratedData = new ObjectLinkedOpenHashSet<>();
        }

        boolean added = INSTANCE.migratedData.add(pack);
        INSTANCE.dirty = added;
        return added;
    }

    public static void save() {
        if (INSTANCE.dirty) {
            JsonLoader.saveJson(INSTANCE, getPath());
        }
    }
}