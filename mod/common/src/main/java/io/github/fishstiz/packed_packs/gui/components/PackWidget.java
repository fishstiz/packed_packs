package io.github.fishstiz.packed_packs.gui.components;

import io.github.fishstiz.fidgetz.v0.gui.components.FZText;
import io.github.fishstiz.fidgetz.v0.utils.GuiGraphicsUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.util.CommonColors;

import java.util.function.Consumer;

class PackWidget implements Renderable, LayoutElement {
    static final int ICON_SIZE = 32;
    private static final Component INCOMPATIBLE_TITLE = Component.translatable("pack.incompatible");
    private static final int DESCRIPTION_LINES = 2;
    private static final int SPACING = 2;
    private final Font font;
    private final PackList.Entry entry;
    private final FZText nameWidget;
    private final MultiLineTextWidget descriptionWidget;
    private int cachedBodyWidth;
    private boolean warningShown;
    private int x;
    private int y;
    private int width;
    private int height;

    PackWidget(PackList.Entry entry, int height) {
        this.entry = entry;
        this.font = Minecraft.getInstance().font;
        this.nameWidget = FZText.builder(entry.pack.title()).build();
        this.height = height;
        this.descriptionWidget = new MultiLineTextWidget(getExtendedDescription(), font);
        repositionElements();
    }

    private Component getExtendedDescription() {
        return ComponentUtils.mergeStyles(entry.pack.decoratedDescription(), Style.EMPTY.withColor(CommonColors.GRAY));
    }

    private void repositionHorizontal() {
        int bodyX = x + ICON_SIZE + SPACING * 2;
        int bodyWidth = (x + width) - SPACING * 2 - bodyX;

        nameWidget.setX(bodyX);
        nameWidget.setWidth(bodyWidth);

        descriptionWidget.setMaxRows(DESCRIPTION_LINES);
        descriptionWidget.setCentered(false);
        descriptionWidget.setX(nameWidget.getX());
        descriptionWidget.setMaxWidth(bodyWidth);

        this.cachedBodyWidth = bodyWidth;
    }

    private void repositionVertical() {
        int lineHeight = font.lineHeight;
        int totalContentHeight = lineHeight + SPACING + (lineHeight * DESCRIPTION_LINES);
        int startY = y + (height - totalContentHeight) / 2;
        nameWidget.setY(startY);
        this.descriptionWidget.setY(startY + lineHeight + SPACING);
    }

    private void repositionElements() {
        repositionHorizontal();
        repositionVertical();
    }

    public void setWidth(int width) {
        this.width = width;
        repositionElements();
    }

    public void setHeight(int height) {
        this.height = height;
        repositionVertical();
    }

    @Override
    public void setX(int x) {
        this.x = x;
        repositionHorizontal();
    }

    @Override
    public void setY(int y) {
        this.y = y;
        repositionVertical();
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void visitWidgets(Consumer<AbstractWidget> widgetVisitor) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiGraphicsUtils.texture(graphics, entry.getPackIcon(), x + SPACING, y, ICON_SIZE, ICON_SIZE);
        nameWidget.render(graphics, mouseX, mouseY, partialTick);
        this.descriptionWidget.render(graphics, mouseX, mouseY, partialTick);
    }

    void checkCompatibility(boolean showWarning) {
        if (showWarning) {
            if (!this.warningShown && !entry.pack.compatibility().isCompatible() && !entry.isIncompatibleWarningsHidden()) {
                nameWidget.setMessage(INCOMPATIBLE_TITLE);
                this.descriptionWidget.setMessage(entry.pack.compatibility().getDescription());
                this.warningShown = true;
            }
        } else if (this.warningShown) {
            nameWidget.setMessage(entry.pack.title());
            nameWidget.setWidth(this.cachedBodyWidth);
            this.descriptionWidget.setMessage(getExtendedDescription());
            this.warningShown = false;
        }
    }
}
