package io.github.fishstiz.fidgetz.gui.shapes;

import io.github.fishstiz.fidgetz.util.GuiUtil;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;

public interface GuiRectangle {
    static GuiRectangle viewOf(LayoutElement element) {
        return new GuiRectangle() {
            @Override
            public int getX() {
                return element.getX();
            }

            @Override
            public int getY() {
                return element.getY();
            }

            @Override
            public int getWidth() {
                return element.getWidth();
            }

            @Override
            public int getHeight() {
                return element.getHeight();
            }
        };
    }

    int getX();

    int getY();

    int getWidth();

    int getHeight();

    default int getRight() {
        return this.getX() + this.getWidth();
    }

    default int getBottom() {
        return this.getY() + this.getHeight();
    }

    default int getFractionX(float fraction) {
        return this.getX() + Math.round(this.getWidth() * Math.clamp(fraction, 0f, 1f));
    }

    default int getFractionY(float fraction) {
        return this.getY() + Math.round(this.getHeight() * Math.clamp(fraction, 0f, 1f));
    }

    default int getMidX() {
        return this.getX() + this.getWidth() / 2;
    }

    default int getMidY() {
        return this.getY() + this.getHeight() / 2;
    }

    default boolean containsPoint(int px, int py) {
        return px >= this.getX() && px < this.getRight() &&
               py >= this.getY() && py < this.getBottom();
    }

    default boolean containsPoint(double px, double py) {
        return this.containsPoint((int) px, (int) py);
    }

    default boolean contains(int x, int y, int right, int bottom) {
        return x >= this.getX() &&
               y >= this.getY() &&
               right <= this.getRight() &&
               bottom <= this.getBottom();
    }

    default boolean contains(GuiRectangle rectangle) {
        return this.contains(rectangle.getX(), rectangle.getY(), rectangle.getRight(), rectangle.getBottom());
    }

    default boolean contains(ScreenRectangle rectangle) {
        return this.contains(rectangle.left(), rectangle.top(), rectangle.right(), rectangle.bottom());
    }

    default boolean contains(LayoutElement element) {
        return this.contains(element.getX(), element.getY(), GuiUtil.getRight(element), GuiUtil.getBottom(element));
    }

    default boolean intersects(int left, int top, int right, int bottom) {
        return this.getX() < right &&
               this.getRight() > left &&
               this.getY() < bottom &&
               this.getBottom() > top;
    }

    default boolean intersects(GuiRectangle rectangle) {
        return this.intersects(rectangle.getX(), rectangle.getY(), rectangle.getRight(), rectangle.getBottom());
    }

    default boolean intersects(ScreenRectangle rectangle) {
        return this.intersects(rectangle.left(), rectangle.top(), rectangle.right(), rectangle.bottom());
    }

    default boolean intersects(LayoutElement element) {
        return this.intersects(element.getX(), element.getY(), GuiUtil.getRight(element), GuiUtil.getBottom(element));
    }

    default ScreenRectangle getScreenRectangle() {
        return new ScreenRectangle(this.getX(), this.getY(), this.getWidth(), this.getHeight());
    }
}
