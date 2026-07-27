package io.github.fishstiz.packed_packs.gui.metadata;

import net.minecraft.client.gui.layouts.Layout;

public record GridWrapper<T extends Layout>(T layout, int spacing) {
}
