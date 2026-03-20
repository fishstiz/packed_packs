package io.github.fishstiz.fidgetz.gui.renderables;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public interface RenderableRect {
    void render(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, float partialTick);

    default void render(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height) {
        this.render(guiGraphics, x, y, width, height, 0);
    }
}
