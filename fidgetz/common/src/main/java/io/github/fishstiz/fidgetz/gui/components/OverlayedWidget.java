package io.github.fishstiz.fidgetz.gui.components;

import io.github.fishstiz.fidgetz.gui.renderables.RenderableRect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;

public class OverlayedWidget extends ContainedWidget {
    protected final RenderableRect overlay;

    public OverlayedWidget(RenderableRect overlay, AbstractWidget widget) {
        super(widget);
        this.overlay = overlay;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        this.overlay.render(guiGraphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), partialTick);
    }
}
