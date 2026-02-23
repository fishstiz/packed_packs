package io.github.fishstiz.fidgetz.gui.components;

import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.LayoutElement;

public class AnchoredWidget extends ContainedWidget implements ContextMenuContainer {
    private final LayoutElement container;
    private final float rowAnchor;
    private final float colAnchor;
    private float alignX;
    private float alignY;
    private int offsetX;
    private int offsetY;

    public AnchoredWidget(AbstractWidget widget, LayoutElement container, float rowAnchor, float colAnchor, float alignX, float alignY, int offsetX, int offsetY) {
        super(widget);
        this.container = container;
        this.rowAnchor = rowAnchor;
        this.colAnchor = colAnchor;
        this.alignX = alignX;
        this.alignY = alignY;
        this.offsetX = offsetX;
        this.offsetY = offsetY;

        this.updatePosition();
    }

    public AnchoredWidget(AbstractWidget widget, LayoutElement container, float rowAnchor, float colAnchor, float alignX, float alignY) {
        this(widget, container, rowAnchor, colAnchor, alignX, alignY, 0, 0);
    }

    public void setOffsetX(int offsetX) {
        this.offsetX = offsetX;
    }

    public void setOffsetY(int offsetY) {
        this.offsetY = offsetY;
    }

    public void setOffset(int offsetX, int offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public void setAlignX(float alignX) {
        this.alignX = alignX;
    }

    public void setAlignY(float alignY) {
        this.alignY = alignY;
    }

    public void setAlignment(float hAlign, float vAlign) {
        this.alignX = hAlign;
        this.alignY = vAlign;
    }

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public float getAlignX() {
        return alignX;
    }

    public float getAlignY() {
        return alignY;
    }

    public void updatePosition() {
        float anchorX = this.container.getX() + (this.container.getWidth() * this.colAnchor);
        float anchorY = this.container.getY() + (this.container.getHeight() * this.rowAnchor);

        int x = Math.round(anchorX - (this.getWidth() * this.alignX)) + this.offsetX;
        int y = Math.round(anchorY - (this.getHeight() * this.alignY)) + this.offsetY;

        this.setPosition(x, y);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.updatePosition();
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
    }
}
