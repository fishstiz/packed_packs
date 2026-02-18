package io.github.fishstiz.packed_packs.platform.services;

import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.api.PackedPacksInitializer;
import net.fabricmc.fabric.impl.resource.pack.ModResourcePackCreator;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.packs.repository.Pack;

import java.nio.file.Path;
import java.util.List;

public class FabricPlatformHelper implements PlatformHelper {
    @Override
    public String getPlatform() {
        return "fabric";
    }

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean isModLoaded(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    @Override
    public List<PackedPacksInitializer> getModExtensions() {
        return FabricLoader.getInstance().getEntrypoints(PackedPacks.MOD_ID, PackedPacksInitializer.class);
    }

    @Override
    public boolean isBuiltInPack(Pack pack) {
        return pack.getPackSource() == ModResourcePackCreator.RESOURCE_PACK_SOURCE;
    }
}
