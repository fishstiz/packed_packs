package io.github.fishstiz.packed_packs.config;

import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.Pack;

public interface PackOptions {
    boolean isHidden(Pack pack);

    boolean isRequired(Pack pack);

    boolean isFixed(Pack pack);

    Pack.Position getPosition(Pack pack);

    PackSelectionConfig getSelectionConfig(Pack pack);
}
