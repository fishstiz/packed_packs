package io.github.fishstiz.packed_packs.platform.services;

import io.github.fishstiz.packed_packs.api.PackedPacksInitializer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;

public class NeoForgePlatformHelper implements PlatformHelper {
    @Override
    public String getPlatform() {
        return "neoforge";
    }

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean isModLoaded(String id) {
        return ModList.get().isLoaded(id);
    }

    @Override
    public List<PackedPacksInitializer> getModExtensions() {
        return ServiceLoader.load(PackedPacksInitializer.class).stream().map(ServiceLoader.Provider::get).toList();
    }
}
