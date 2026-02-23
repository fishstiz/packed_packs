package io.github.fishstiz.packed_packs.impl.context;

import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.packed_packs.api.context.PackContext;
import net.minecraft.resources.ResourceLocation;

public interface PackEntryContext extends PackContext {
    Sprite sprite();

    @Override
    default ResourceLocation icon() {
        return this.sprite().location;
    }
}
