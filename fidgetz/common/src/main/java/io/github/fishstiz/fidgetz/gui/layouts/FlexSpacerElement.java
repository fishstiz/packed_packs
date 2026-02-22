package io.github.fishstiz.fidgetz.gui.layouts;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.LayoutElement;
import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;

public class FlexSpacerElement implements LayoutElement {
    private int x;
    private int y;
    private int width;
    private int height;

    public FlexSpacerElement() {
    }

    public FlexSpacerElement(int width, int height) {
        this(0, 0, width, height);
    }

    public FlexSpacerElement(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public void setX(int x) {
        this.x = x;
    }

    @Override
    public void setY(int y) {
        this.y = y;
    }

    @Override
    public int getX() {
        return this.x;
    }

    @Override
    public int getY() {
        return this.y;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public void visitWidgets(@NonNull Consumer<AbstractWidget> visitor) {
    }
}
