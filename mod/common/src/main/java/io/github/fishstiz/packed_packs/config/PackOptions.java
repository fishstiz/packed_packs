package io.github.fishstiz.packed_packs.config;

import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.Pack;

public interface PackOptions {
    boolean isHidden(String packId);

    boolean isRequired(String packId);

    boolean isFixed(String packId);

    Pack.Position getPosition(String packId);

    PackSelectionConfig getSelectionConfig(String packId);
}
