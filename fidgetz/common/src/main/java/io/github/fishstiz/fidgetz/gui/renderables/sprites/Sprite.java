package io.github.fishstiz.fidgetz.gui.renderables.sprites;

import io.github.fishstiz.fidgetz.gui.renderables.RenderableRect;
import io.github.fishstiz.fidgetz.gui.shapes.Line;
import io.github.fishstiz.fidgetz.gui.shapes.Size;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;

public class Sprite implements RenderableRect {
    public final ResourceLocation location;
    public final int width;
    public final int height;
    public final int uOffset;
    public final int vOffset;
    public final int uWidth;
    public final int vHeight;

    public Sprite(ResourceLocation location, int width, int height, int uOffset, int vOffset, int uWidth, int vHeight) {
        this.location = location;
        this.width = width;
        this.height = height;
        this.uOffset = uOffset;
        this.vOffset = vOffset;
        this.uWidth = uWidth;
        this.vHeight = vHeight;
    }

    public Sprite(ResourceLocation location, int width, int height) {
        this(location, width, height, 0, 0, width, height);
    }

    public Sprite(ResourceLocation location, Size size, Line u, Line v) {
        this(location, size.width(), size.height(), u.start(), v.start(), u.length(), v.length());
    }

    public Sprite(ResourceLocation location, Size size) {
        this(location, size.width(), size.height());
    }

    public static Sprite of32(ResourceLocation location) {
        return new Sprite(location, Size.of32());
    }

    public static Sprite of16(ResourceLocation location) {
        return new Sprite(location, Size.of16());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int width, int height, float partialTick) {
        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                this.location,
                x, y,
                this.uOffset, this.vOffset,
                width, height,
                this.uWidth, this.vHeight,
                this.width, this.height
        );
    }

    public void render(GuiGraphics guiGraphics, int x, int y) {
        this.render(guiGraphics, x, y, this.width, this.height, 0);
    }

    public void renderClamped(GuiGraphics guiGraphics, int x, int y, int width, int height, float partialTick) {
        int drawWidth = Math.min(width, this.width);
        int drawHeight = Math.min(height, this.height);

        int offsetX = (width - drawWidth) / 2;
        int offsetY = (height - drawHeight) / 2;

        this.render(guiGraphics, x + offsetX, y + offsetY, drawWidth, drawHeight, partialTick);
    }
}
