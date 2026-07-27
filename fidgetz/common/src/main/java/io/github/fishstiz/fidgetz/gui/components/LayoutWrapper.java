package io.github.fishstiz.fidgetz.gui.components;

import io.github.fishstiz.fidgetz.gui.shapes.Padding;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class LayoutWrapper<T extends Layout> extends AbstractWidget implements Layout {
    private T layout;
    private int minWidth;
    private int minHeight;
    private int paddingTop;
    private int paddingRight;
    private int paddingBottom;
    private int paddingLeft;

    public LayoutWrapper(T layout, int minWidth, int minHeight) {
        super(0, 0, 0, 0, CommonComponents.EMPTY);

        this.layout = layout;
        this.minWidth = minWidth;
        this.minHeight = minHeight;

        this.active = false;
    }

    public LayoutWrapper(T layout) {
        this(layout, 0, 0);
    }

    public void setLayout(@NotNull T layout) {
        this.layout = layout;
    }

    public T layout() {
        return this.layout;
    }

    public void setPadding(int top, int right, int bottom, int left) {
        this.paddingTop = top;
        this.paddingRight = right;
        this.paddingBottom = bottom;
        this.paddingLeft = left;
    }

    public void setPadding(Padding padding) {
        this.setPadding(padding.top(), padding.right(), padding.bottom(), padding.left());
    }

    public void setPadding(int padding) {
        this.setPadding(padding, padding, padding, padding);
    }

    @Override
    public void setWidth(int width) {
        this.width = Math.max(this.minWidth, width);
    }

    @Override
    public void setHeight(int height) {
        this.height = Math.max(this.minHeight, height);
    }

    public void setMinWidth(int minWidth) {
        this.minWidth = minWidth;
        this.setWidth(this.width);
    }

    public void setMinHeight(int minHeight) {
        this.minHeight = minHeight;
        this.setHeight(this.height);
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public void setX(int x) {
        this.layout.setX(x + this.paddingLeft);
    }

    @Override
    public void setY(int y) {
        this.layout.setY(y + this.paddingTop);
    }

    @Override
    public int getX() {
        return this.layout.getX() - this.paddingLeft;
    }

    @Override
    public int getY() {
        return this.layout.getY() - this.paddingTop;
    }

    @Override
    public int getWidth() {
        return this.width + this.paddingLeft + this.paddingRight;
    }

    @Override
    public int getHeight() {
        return this.height + this.paddingTop + this.paddingBottom;
    }

    public Padding getPadding() {
        return new Padding(this.paddingTop, this.paddingRight, this.paddingBottom, this.paddingLeft);
    }

    @Override
    public void visitChildren(Consumer<LayoutElement> visitor) {
        this.layout.visitChildren(visitor);
    }

    @Override
    public void visitWidgets(Consumer<AbstractWidget> consumer) {
        this.layout.visitWidgets(consumer);
    }

    @Override
    public void arrangeElements() {
        this.layout.arrangeElements();
        this.setPosition(this.layout.getX(), this.layout.getY());
        this.setWidth(this.layout.getWidth());
        this.setHeight(this.layout.getHeight());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    protected final void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    protected final void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.layout.visitWidgets(widget -> widget.updateNarration(narrationElementOutput));
    }

    @Override
    public final void playDownSound(SoundManager handler) {
    }
}
