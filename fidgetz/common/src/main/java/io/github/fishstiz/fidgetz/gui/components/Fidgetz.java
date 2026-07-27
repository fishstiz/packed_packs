package io.github.fishstiz.fidgetz.gui.components;

import io.github.fishstiz.fidgetz.gui.shapes.GuiRectangle;
import io.github.fishstiz.fidgetz.util.GuiUtil;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.NotNull;

public interface Fidgetz extends GuiEventListener, GuiRectangle, LayoutElement {
    default boolean isHovered(double mouseX, double mouseY) {
        return GuiUtil.isHovered(this, mouseX, mouseY);
    }

    @Override
    default boolean isMouseOver(double mouseX, double mouseY) {
        return this.containsPoint(mouseX, mouseY);
    }

    @Override
    default @NotNull ScreenRectangle getRectangle() {
        return LayoutElement.super.getRectangle();
    }
}
