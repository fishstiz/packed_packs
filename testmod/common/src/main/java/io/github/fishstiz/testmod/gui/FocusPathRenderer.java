package io.github.fishstiz.testmod.gui;

import io.github.fishstiz.testmod.TestFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

public class FocusPathRenderer extends TestFeatureRenderer {
    private static final String LABEL = "Focus Path: ";
    private final Minecraft minecraft;

    public FocusPathRenderer(TestFeature feature) {
        super(feature);
        this.minecraft = Minecraft.getInstance();
    }

    @Override
    protected int renderFeature(GuiGraphics guiGraphics, int mouseX, int mouseY, int y, float partialTick) {
        Screen screen = minecraft.screen;
        if (screen == null) return y;

        guiGraphics.fill(0, y, minecraft.font.width(LABEL), y + minecraft.font.lineHeight, 0x7FFF0000);
        guiGraphics.drawString(minecraft.font, "Focus Path: ", 0, y, 0xFFFFFFFF);
        return renderFocusPath(guiGraphics, screen.getFocused(), y + minecraft.font.lineHeight, minecraft.font);
    }

    private static int renderFocusPath(GuiGraphics guiGraphics, @Nullable GuiEventListener listener, int y, Font font) {
        if (listener == null) return y;

        String name = listener.getClass().getName();
        guiGraphics.fill(0, y, font.width(name), y + font.lineHeight, 0x7FFF0000);
        guiGraphics.drawString(font, name, 0, y, 0xFFFFFFFF);

        if (listener instanceof ContainerEventHandler container) {
            return renderFocusPath(guiGraphics, container.getFocused(), y + font.lineHeight, font);
        }

        return y + font.lineHeight;
    }
}