package io.github.fishstiz.fidgetz.gui.renderables;

import net.minecraft.client.gui.GuiGraphics;

public interface RenderableRect {
    void render(GuiGraphics guiGraphics, int x, int y, int width, int height, float partialTick);

    default void render(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        this.render(guiGraphics, x, y, width, height, 0);
    }
}
