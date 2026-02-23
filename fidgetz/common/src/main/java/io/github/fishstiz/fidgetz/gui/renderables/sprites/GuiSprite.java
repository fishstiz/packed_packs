package io.github.fishstiz.fidgetz.gui.renderables.sprites;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class GuiSprite extends Sprite {
    public GuiSprite(ResourceLocation location, int width, int height) {
        super(location, width, height);
    }

    public GuiSprite(ResourceLocation location, int size) {
        this(location, size, size);
    }

    public static GuiSprite of32(ResourceLocation location) {
        return new GuiSprite(location, 32);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int width, int height, float partialTick) {
        guiGraphics.blitSprite(this.location, x, y, width, height);
    }
}
