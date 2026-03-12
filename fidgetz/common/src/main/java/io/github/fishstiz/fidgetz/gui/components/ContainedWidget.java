package io.github.fishstiz.fidgetz.gui.components;

import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class ContainedWidget extends AbstractWidget implements Fidgetz, ContainerEventHandler {
    protected final AbstractWidget widget;
    private final List<AbstractWidget> children;
    private @Nullable GuiEventListener focused;
    private boolean dragging;

    public ContainedWidget(AbstractWidget widget) {
        super(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), widget.getMessage());
        this.children = Collections.singletonList(widget);
        this.widget = widget;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.widget.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.widget.updateNarration(narrationElementOutput);
    }

    @Override
    public List<AbstractWidget> children() {
        return this.children;
    }

    @Override
    public boolean isDragging() {
        return this.dragging;
    }

    @Override
    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    @Override
    public @Nullable GuiEventListener getFocused() {
        return this.focused;
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        this.focused = focused;
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        this.widget.setX(x);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        this.widget.setY(y);
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        this.widget.setWidth(width);
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        this.widget.setHeight(height);
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        this.widget.setSize(width, height);
    }

    @Override
    public int getHeight() {
        return this.widget.getHeight();
    }

    @Override
    public int getWidth() {
        return this.widget.getWidth();
    }

    @Override
    public int getX() {
        return this.widget.getX();
    }

    @Override
    public int getY() {
        return this.widget.getY();
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent focusNavigationEvent) {
        return ContainerEventHandler.super.nextFocusPath(focusNavigationEvent);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
        return ContainerEventHandler.super.mouseClicked(mouseButtonEvent, doubleClicked);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        return ContainerEventHandler.super.mouseReleased(mouseButtonEvent);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
        return ContainerEventHandler.super.mouseDragged(mouseButtonEvent, dragX, dragY);
    }

    @Override
    public boolean isFocused() {
        return ContainerEventHandler.super.isFocused();
    }
}
