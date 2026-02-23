package io.github.fishstiz.fidgetz.util;

import io.github.fishstiz.fidgetz.gui.renderables.sprites.GuiSprite;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class DrawUtil {
    public static final Sprite DEMO_BACKGROUND = new GuiSprite(ResourceLocation.withDefaultNamespace("popup/background"), 236, 34);
    public static final ResourceLocation SHADOW_SPRITE = ResourceLocation.fromNamespaceAndPath("fidgetz", "drop_shadow");
    private static final int SHADOW_BORDER = 32;

    private DrawUtil() {
    }

    public static int renderScrollingStringLeftAlign(
            GuiGraphics guiGraphics,
            Font font,
            Component text,
            int startX,
            int startY,
            int endX,
            int endY,
            int color,
            boolean shadow
    ) {
        int textWidth = font.width(text);
        int textY = (startY + endY - 9) / 2 + 1;
        int availableWidth = endX - startX;

        if (textWidth > availableWidth) {
            int overflowWidth = textWidth - availableWidth;
            double timeSeconds = Util.getMillis() / 1000.0;
            double scrollDuration = Math.max(overflowWidth * 0.5, 3.0);
            double scrollFactor = Math.sin((Math.PI / 2) * Math.cos((Math.PI * 2) * timeSeconds / scrollDuration)) / 2.0 + 0.5;
            double scrollOffset = Mth.lerp(scrollFactor, 0.0, overflowWidth);

            guiGraphics.enableScissor(startX, startY, endX, endY);
            int width = guiGraphics.drawString(font, text, startX - (int) scrollOffset, textY, color, shadow);
            guiGraphics.disableScissor();

            return width;
        } else {
            return guiGraphics.drawString(font, text, startX, textY, color, shadow);
        }
    }

    public static int renderScrollingStringLeftAlign(
            GuiGraphics guiGraphics,
            Font font,
            Component text,
            int startX,
            int startY,
            int endX,
            int endY,
            int color
    ) {
        return renderScrollingStringLeftAlign(guiGraphics, font, text, startX, startY, endX, endY, color, true);
    }

    public static void renderDropShadow(GuiGraphics guiGraphics, int x, int y, int width, int height, int shadowSize) {
        float scale = (float) shadowSize / SHADOW_BORDER;
        int offset = Math.round(SHADOW_BORDER * scale);
        renderSprite(
                guiGraphics,
                SHADOW_SPRITE,
                x - offset,
                y - offset,
                width + offset * 2,
                height + offset * 2
        );
    }

    public static void renderTexture(GuiGraphics guiGraphics, ResourceLocation texture, int textureWidth, int textureHeight, int x, int y, int width, int height) {
        guiGraphics.blit(
                RenderType::guiTextured,
                texture,
                x, y,
                0, 0,
                width, height,
                textureWidth, textureHeight,
                textureWidth, textureHeight
        );
    }

    public static void renderSprite(GuiGraphics guiGraphics, ResourceLocation sprite, int x, int y, int width, int height) {
        guiGraphics.blitSprite(RenderType::guiTextured, sprite, x, y, width, height);
    }
}
