package io.github.fishstiz.packed_packs.compat;

import net.fabricmc.loader.api.FabricLoader;

public enum FabricMod implements ModContext {
    VTD("vt_downloader");

    private final boolean loaded;
    private final String id;

    FabricMod(String id) {
        this.id = id;
        this.loaded = FabricLoader.getInstance().isModLoaded(id);
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public boolean isLoaded() {
        return this.loaded;
    }
}
