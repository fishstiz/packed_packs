package io.github.fishstiz.packed_packs.config.migrations;

import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.config.FolderPackMeta;
import io.github.fishstiz.packed_packs.config.JsonLoader;
import io.github.fishstiz.packed_packs.config.ProfileManager;
import io.github.fishstiz.packed_packs.config.VersionState;
import io.github.fishstiz.packed_packs.pack.FolderLocationInfo;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.server.packs.PackType;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class LegacyFoldersHelper {
    private static final Map<Path, FolderPackMeta> METADATA = new Object2ObjectOpenHashMap<>();
    private static final ConcurrentHashMap<String, String> RESOURCE_PACKS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> DATA_PACKS = new ConcurrentHashMap<>();

    // resource packs can be migrated once on load
    // data packs need to be migrated per level, so there's no end to the migration
    // it may be possible to keep track of existing levels on first update but seems like too much work
    public static void addLegacyPack(PackType packType, FolderLocationInfo folder, String packId) {
        if (!VersionState.shouldMigrateLegacyFolders() || folder.parent() != null) { // legacy folders exist at base dir only
            return;
        }

        if (packType == PackType.CLIENT_RESOURCES && LegacyFolders.isResourcePacksMigrated()) {
            return;
        }

        String legacyId = folder.location().id() + "/" + packId.replaceFirst("^file/", "");

        boolean seen = switch (packType) {
            case CLIENT_RESOURCES -> RESOURCE_PACKS.putIfAbsent(legacyId, packId) == null;
            case SERVER_DATA -> {
                DATA_PACKS.putIfAbsent(legacyId, packId);
                yield LegacyFolders.flagDataPackMigrated(legacyId);
            }
        };

        if (!seen) return;

        ProfileManager.get(packType).remapAndSavePackIds(legacyId, packId);
        Path metadataPath = folder.path().resolve(FolderPackMeta.FILENAME).toAbsolutePath().normalize();

        synchronized (METADATA) {
            FolderPackMeta metadata = METADATA.computeIfAbsent(
                    metadataPath,
                    path -> JsonLoader.loadOrDefault(
                            path,
                            FolderPackMeta.class,
                            () -> new FolderPackMeta(false) // don't need to remap if there's no config
                    )
            );

            if (!metadata.module() || !metadata.packIds().contains(legacyId)) {
                return;
            }

            FolderPackMeta newMetadata = new FolderPackMeta(true, switch (packType) {
                case CLIENT_RESOURCES -> metadata.packIds().stream()
                        .map(id -> {
                            if (!RESOURCE_PACKS.containsKey(id)) {
                                return id;
                            }
                            String remapped = RESOURCE_PACKS.get(id);
                            PackedPacks.LOGGER.info(
                                    "[packed_packs] Migrating legacy resource pack id '{}' to '{}' from folder '{}'.",
                                    id,
                                    folder.path(),
                                    remapped
                            );
                            return remapped;

                        })
                        .toList();
                case SERVER_DATA -> metadata.packIds().stream()
                        .map(id -> {
                            if (!DATA_PACKS.containsKey(id)) {
                                return id;
                            }

                            String remapped = DATA_PACKS.get(id);
                            PackedPacks.LOGGER.info(
                                    "[packed_packs] Migrating legacy data pack id '{}' to '{}' from folder '{}'.",
                                    id,
                                    folder.path(),
                                    remapped
                            );
                            return remapped;
                        })
                        .toList();
            });

            METADATA.put(metadataPath, newMetadata);
            if (!Objects.equals(metadata, newMetadata)) {
                JsonLoader.saveJson(newMetadata, metadataPath);
            }
        }
    }

    public static List<String> remapLegacyDataPackIdsForLevel(List<String> packIds) {
        return packIds.stream()
                .map(id -> {
                    if (!DATA_PACKS.containsKey(id)) {
                        return id;
                    }

                    String remapped = DATA_PACKS.get(id);
                    PackedPacks.LOGGER.info(
                            "[packed_packs] Migrating legacy data pack id '{}' to '{}' from current level.",
                            id,
                            remapped
                    );
                    return remapped;
                })
                .collect(Collectors.toCollection(ArrayList::new)); // data packs are usually immutable, but just in case for compat
    }

    public static void remapLegacyResourcePackIdsForOptions(List<String> packIds) {
        packIds.replaceAll(id -> {
            if (!RESOURCE_PACKS.containsKey(id)) {
                return id;
            }

            String remapped = RESOURCE_PACKS.get(id);
            PackedPacks.LOGGER.info(
                    "[packed_packs] Migrating legacy resource pack id '{}' to '{}' from options file.",
                    id,
                    remapped
            );
            return remapped;
        });
    }

    private LegacyFoldersHelper() {
    }
}
