package io.github.fishstiz.fidgetz.util;

import io.github.fishstiz.fidgetz.gui.renderables.sprites.GuiSprite;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import net.minecraft.util.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class DrawUtil {
    public static final Sprite DEMO_BACKGROUND = new GuiSprite(Identifier.withDefaultNamespace("popup/background"), 236, 34);
    public static final Identifier SHADOW_SPRITE = Identifier.fromNamespaceAndPath("fidgetz", "drop_shadow");
    private static final int SHADOW_BORDER = 32;

    private DrawUtil() {
    }

    public static void renderScrollingStringLeftAlign(
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
            guiGraphics.drawString(font, text, startX - (int) scrollOffset, textY, color, shadow);
            guiGraphics.disableScissor();
        } else {
            guiGraphics.drawString(font, text, startX, textY, color, shadow);
        }
    }

    public static void renderScrollingStringLeftAlign(
            GuiGraphics guiGraphics,
            Font font,
            Component text,
            int startX,
            int startY,
            int endX,
            int endY,
            int color
    ) {
        renderScrollingStringLeftAlign(guiGraphics, font, text, startX, startY, endX, endY, color, true);
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

    public static void renderOutline(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        guiGraphics.fill(x, y, x + width, y + 1, color);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, color);
        guiGraphics.fill(x, y + 1, x + 1, y + height - 1, color);
        guiGraphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    public static void renderTexture(GuiGraphics guiGraphics, Identifier texture, int textureWidth, int textureHeight, int x, int y, int width, int height) {
        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                texture,
                x, y,
                0, 0,
                width, height,
                textureWidth, textureHeight,
                textureWidth, textureHeight
        );
    }

    public static void renderSprite(GuiGraphics guiGraphics, Identifier sprite, int x, int y, int width, int height) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height);
    }
}
