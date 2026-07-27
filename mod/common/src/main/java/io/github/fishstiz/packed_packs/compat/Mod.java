package io.github.fishstiz.packed_packs.compat;

import io.github.fishstiz.packed_packs.platform.Services;

public enum Mod implements ModContext {
    MINECRAFT_CURSOR("minecraft-cursor"),
    RESOURCIFY("resourcify"),
    RESPACKOPTS("respackopts"),
    ETF("entity_texture_features");

    private final String id;
    private final boolean loaded;

    Mod(String id) {
        this.id = Services.PLATFORM.getPlatform().equals("fabric") ? id : id.replaceAll("-", "_");
        this.loaded = Services.PLATFORM.isModLoaded(this.id);
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
