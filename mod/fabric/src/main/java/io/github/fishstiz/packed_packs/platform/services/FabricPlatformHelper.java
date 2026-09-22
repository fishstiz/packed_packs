package io.github.fishstiz.packed_packs.platform.services;

import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.api.PackedPacksInitializer;
import net.fabricmc.fabric.impl.resource.pack.ModResourcePackCreator;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

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
    public boolean isDev() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public <T> List<T> getServices(String key, Class<T> type) {
        return FabricLoader.getInstance().getEntrypoints(key, type);
    }

    @Override
    public <T> Predicate<T> getPredicate(Class<T> type) {
        return obj -> obj instanceof PackSource packSource && packSource == ModResourcePackCreator.RESOURCE_PACK_SOURCE;
    }
}
