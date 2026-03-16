package io.github.fishstiz.testmod.gui;

import com.mojang.blaze3d.platform.Window;
import io.github.fishstiz.fidgetz.util.GuiUtil;
import io.github.fishstiz.testmod.TestFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;


public class HoveredElementRenderer extends TestFeatureRenderer {
    private static final int COLOR = 0xFF00FF00;
    private final Minecraft minecraft;

    public HoveredElementRenderer(TestFeature feature) {
        super(feature);
        this.minecraft = Minecraft.getInstance();
    }

    @Override
    protected int renderFeature(GuiGraphics guiGraphics, int mouseX, int mouseY, int y, float partialTick) {
        Screen screen = minecraft.screen;
        if (screen == null) return y;

        GuiEventListener hovered = GuiUtil.findHovered(screen, mouseX, mouseY);
        hovered = hovered != null ? hovered : screen;

        ScreenRectangle bounds = getBounds(hovered);
        String label = hovered.getClass().getName();
        int labelX = clampX(minecraft.getWindow(), minecraft.font, bounds.left() + 2, label);
        int labelY = bounds.top() + 2;

        guiGraphics.renderOutline(bounds.left(), bounds.top(), bounds.width(), bounds.height(), COLOR);
        guiGraphics.drawString(minecraft.font, label, labelX, labelY, COLOR);

        return y;
    }

    private static int clampX(Window window, Font font, int x, String label) {
        int textWidth = font.width(label);
        int screenWidth = window.getGuiScaledWidth();
        if (x + textWidth > screenWidth) {
            x = screenWidth - textWidth - 2;
        }
        return Math.max(0, x);
    }

    private static ScreenRectangle getBounds(@Nullable GuiEventListener element) {
        if (element instanceof LayoutElement layoutElement) return layoutElement.getRectangle();
        return element != null ? element.getRectangle() : ScreenRectangle.empty();
    }
}
