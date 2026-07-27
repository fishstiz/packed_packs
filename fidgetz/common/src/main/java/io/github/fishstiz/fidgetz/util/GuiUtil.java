package io.github.fishstiz.fidgetz.util;

import io.github.fishstiz.fidgetz.gui.components.HoverStateHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public class GuiUtil {
    private GuiUtil() {
    }

    public static boolean isDescendant(ContainerEventHandler container, GuiEventListener listener) {
        for (GuiEventListener child : container.children()) {
            if (child == listener) {
                return true;
            }
            if (child instanceof ContainerEventHandler parent && isDescendant(parent, listener)) {
                return true;
            }
        }
        return false;
    }

    public static boolean deepChildHovered(ContainerEventHandler container, double mouseX, double mouseY) {
        for (GuiEventListener child : container.children()) {
            if (child instanceof ContainerEventHandler nestedContainer && deepChildHovered(nestedContainer, mouseX, mouseY)) {
                return true;
            }
            if (child.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    public static GuiEventListener findHovered(ContainerEventHandler container, double mouseX, double mouseY) {
        for (GuiEventListener child : container.children()) {
            if (child.isMouseOver(mouseX, mouseY)) {
                if (child instanceof ContainerEventHandler nestedContainer) {
                    GuiEventListener hoveredElement = findHovered(nestedContainer, mouseX, mouseY);
                    if (hoveredElement != null) {
                        return hoveredElement;
                    }
                }
                return child;
            }
        }
        return null;
    }

    public static boolean isHovered(ContainerEventHandler container, GuiEventListener listener, double mouseX, double mouseY) {
        if (container.isMouseOver(mouseX, mouseY)) {
            if (container instanceof HoverStateHandler hoverStateHandler) {
                return hoverStateHandler.getHovered() == listener;
            }

            for (GuiEventListener child : container.children()) {
                if (child instanceof ContainerEventHandler nestedContainer && isHovered(nestedContainer, listener, mouseX, mouseY)) {
                    return true;
                }
                if (child.isMouseOver(mouseX, mouseY)) {
                    return child == listener;
                }
            }
        }
        return false;
    }

    public static boolean isHovered(GuiEventListener listener, double mouseX, double mouseY) {
        Screen screen = Minecraft.getInstance().screen;
        return screen != null && isHovered(screen, listener, mouseX, mouseY);
    }

    public static void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    public static boolean containsPoint(int x, int y, int width, int height, double px, double py) {
        return px >= x && px < (x + width) && py >= y && py < (y + height);
    }

    public static boolean containsPoint(int x, int y, int width, int height, int px, int py) {
        return containsPoint(x, y, width, height, (double) px, py);
    }

    public static boolean containsPoint(LayoutElement element, int px, int py) {
        return containsPoint(element.getX(), element.getY(), element.getWidth(), element.getHeight(), px, py);
    }

    public static boolean containsPoint(LayoutElement element, double px, double py) {
        return containsPoint(element, (int) px, (int) py);
    }

    public static boolean contains(ScreenRectangle container, ScreenRectangle rectangle) {
        return rectangle.left() >= container.left() &&
               rectangle.top() >= container.top() &&
               rectangle.right() <= container.right() &&
               rectangle.bottom() <= container.bottom();
    }

    public static boolean contains(LayoutElement container, ScreenRectangle rectangle) {
        return rectangle.left() >= container.getX() &&
               rectangle.top() >= container.getY() &&
               rectangle.right() <= getRight(container) &&
               rectangle.bottom() <= getBottom(container);
    }

    public static boolean contains(ScreenRectangle container, LayoutElement element) {
        return element.getX() >= container.left() &&
               element.getY() >= container.top() &&
               getRight(element) <= container.right() &&
               getBottom(element) <= container.bottom();
    }

    public static boolean contains(LayoutElement container, LayoutElement element) {
        return element.getX() >= container.getX() &&
               element.getY() >= container.getY() &&
               getRight(element) <= getRight(container) &&
               getBottom(element) <= getBottom(container);
    }

    public static boolean intersects(ScreenRectangle first, ScreenRectangle second) {
        return first.left() < second.right() &&
               first.right() > second.left() &&
               first.top() < second.bottom() &&
               first.bottom() > second.top();
    }

    public static boolean intersects(LayoutElement first, ScreenRectangle second) {
        return first.getX() < second.right() &&
               getRight(first) > second.left() &&
               first.getY() < second.bottom() &&
               getBottom(first) > second.top();
    }

    public static boolean intersects(ScreenRectangle first, LayoutElement second) {
        return first.left() < getRight(second) &&
               first.right() > second.getX() &&
               first.top() < getBottom(second) &&
               first.bottom() > second.getY();
    }

    public static boolean intersects(LayoutElement first, LayoutElement second) {
        return first.getX() < getRight(second) &&
               getRight(first) > second.getX() &&
               first.getY() < getBottom(second) &&
               getBottom(first) > second.getY();
    }

    public static int getRight(LayoutElement element) {
        return element.getX() + element.getWidth();
    }

    public static int getBottom(LayoutElement element) {
        return element.getY() + element.getHeight();
    }
}
