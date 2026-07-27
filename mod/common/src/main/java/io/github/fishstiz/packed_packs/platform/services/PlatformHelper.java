package io.github.fishstiz.packed_packs.platform.services;

import io.github.fishstiz.packed_packs.api.PackedPacksInitializer;
import net.minecraft.server.packs.repository.Pack;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public interface PlatformHelper {
    String getPlatform();

    Path getConfigDir();

    boolean isModLoaded(String id);

    default List<PackedPacksInitializer> getModExtensions() {
        return Collections.emptyList();
    }

    default boolean isBuiltInPack(Pack pack) {
        return false;
    }
}
