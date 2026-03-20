package io.github.fishstiz.fidgetz.gui.renderables;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public record ColoredRect(int value) implements RenderableRect {
    @Override
    public void render(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, float partialTick) {
        guiGraphics.fill(x, y, x + width, y + height, this.value);
    }
}
