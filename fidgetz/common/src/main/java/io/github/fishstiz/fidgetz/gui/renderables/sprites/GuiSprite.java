package io.github.fishstiz.fidgetz.gui.renderables.sprites;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class GuiSprite extends Sprite {
    public GuiSprite(Identifier location, int width, int height) {
        super(location, width, height);
    }

    public GuiSprite(Identifier location, int size) {
        this(location, size, size);
    }

    public static GuiSprite of32(Identifier location) {
        return new GuiSprite(location, 32);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int width, int height, float partialTick) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.location, x, y, width, height);
    }
}
