package io.github.fishstiz.packed_packs.config;

import net.minecraft.server.packs.PackType;

public record PackConfigs(PackType packType, Config.Packs user, DevConfig.Packs dev, ProfileManager profiles) {
    public static PackConfigs get(PackType packType) {
        return new PackConfigs(
                packType,
                Config.packs(packType),
                DevConfig.packs(packType),
                ProfileManager.get(packType)
        );
    }
}
