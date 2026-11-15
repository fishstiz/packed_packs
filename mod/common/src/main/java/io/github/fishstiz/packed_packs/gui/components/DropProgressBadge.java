package io.github.fishstiz.packed_packs.gui.components;

import io.github.fishstiz.fidgetz.v0.gui.renderables.RenderableRectangle;
import io.github.fishstiz.packed_packs.util.Colors;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;

public class DropProgressBadge implements DropCandidate, RenderableRectangle {
    private static final int ICON_PADDING = 2;
    private final RenderableRectangle icon;
    private final float progress;

    public DropProgressBadge(RenderableRectangle icon, float progress) {
        this.icon = icon.shrink(ICON_PADDING);
        this.progress = progress;
    }

    @Override
    public boolean valid() {
        return true;
    }

    @Override
    public RenderableRectangle badge() {
        return this;
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int left,
            int top,
            int width,
            int height,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        ScreenRectangle bounds = new ScreenRectangle(left, top, width, height);
        graphics.fill(left, top, bounds.right(), bounds.bottom(), Colors.GRAY_800);
        icon.extractRenderState(graphics, left, top, width, height, mouseX, mouseY, partialTick);
        graphics.outline(left, top, width, height, Colors.WHITE);

        if (progress > 0.0f) {
            float perimeter = 2f * width + 2f * height;
            float filledLength = perimeter * Math.clamp(progress, 0.0f, 1.0f);

            int right = left + width;
            int bottom = top + height;
            float remaining = filledLength;

            float bottomLen = Math.min(remaining, width);
            if (bottomLen > 0) {
                graphics.fill(left, bottom - 1, left + (int) bottomLen, bottom, Colors.GREEN_500);
                remaining -= bottomLen;
            }
            if (remaining > 0) {
                float rightLen = Math.min(remaining, height);
                graphics.fill(right - 1, bottom - (int) rightLen, right, bottom, Colors.GREEN_500);
                remaining -= rightLen;
            }
            if (remaining > 0) {
                float topLen = Math.min(remaining, width);
                graphics.fill(right - (int) topLen, top, right, top + 1, Colors.GREEN_500);
                remaining -= topLen;
            }
            if (remaining > 0) {
                float leftLen = Math.min(remaining, height);
                graphics.fill(left, top, left + 1, top + (int) leftLen, Colors.GREEN_500);
            }
        }
    }
}
