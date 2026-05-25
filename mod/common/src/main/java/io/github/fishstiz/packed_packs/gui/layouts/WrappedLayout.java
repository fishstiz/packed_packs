package io.github.fishstiz.packed_packs.gui.layouts;

import io.github.fishstiz.fidgetz.v0.gui.components.FZText;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZFlexLayout;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZLayout;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class WrappedLayout implements FZLayout {
    private final FZLayout layout;

    protected WrappedLayout(FZLayout layout) {
        this.layout = layout;
    }

    static FZLayout error(Component message) {
        FZFlexLayout layout = FZFlexLayout.vertical().maxWidth(250);
        layout.child(FZText.builder(Component.literal("Error!").withStyle(ChatFormatting.BOLD)).build());
        layout.child(FZText.builder(message).build());
        return layout;
    }

    @Override
    public void visitChildren(Consumer<LayoutElement> consumer) {
        layout.visitChildren(consumer);
    }

    @Override
    public void visitWidgets(Consumer<AbstractWidget> widgetVisitor) {
        layout.visitWidgets(widgetVisitor);
    }

    @Override
    public void arrangeElements() {
        layout.arrangeElements();
    }

    @Override
    public void setX(int i) {
        layout.setX(i);
    }

    @Override
    public void setY(int i) {
        layout.setY(i);
    }

    @Override
    public int getX() {
        return layout.getX();
    }

    @Override
    public int getY() {
        return layout.getY();
    }

    @Override
    public int getWidth() {
        return layout.getWidth();
    }

    @Override
    public int getHeight() {
        return layout.getHeight();
    }

    @Override
    public ScreenRectangle getRectangle() {
        return layout.getRectangle();
    }

    @Override
    public void setPosition(int x, int y) {
        layout.setPosition(x, y);
    }

    @Override
    public void fidgetz$setWidth(int width) {
        layout.fidgetz$setWidth(width);
    }

    @Override
    public void fidgetz$setHeight(int height) {
        layout.fidgetz$setHeight(height);
    }

    @Override
    public void fidgetz$setSize(int width, int height) {
        layout.fidgetz$setSize(width, height);
    }

    @Override
    public boolean fidgetz$isVisible() {
        return layout.fidgetz$isVisible();
    }
}
