package io.github.fishstiz.packed_packs.gui.components;

import io.github.fishstiz.fidgetz.v0.gui.components.FZText;
import io.github.fishstiz.fidgetz.v0.utils.GuiGraphicsUtils;
import io.github.fishstiz.packed_packs.gui.model.PackListViewModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.util.CommonColors;

import java.util.function.Consumer;

class PackWidget implements Renderable, LayoutElement {
    static final int ICON_SIZE = 32;
    private static final Component INCOMPATIBLE_TITLE = Component.translatable("pack.incompatible");
    private static final int DESCRIPTION_LINES = 2;
    private static final int SPACING = 2;
    private final Font font;
    private final PackListViewModel.Entry model;
    private final FZText nameWidget;
    private final MultiLineTextWidget descriptionWidget;
    private int cachedBodyWidth;
    private boolean warningShown;
    private int x;
    private int y;
    private int width;
    private int height;

    PackWidget(PackListViewModel.Entry model, int height) {
        this.model = model;
        this.font = Minecraft.getInstance().font;
        this.nameWidget = FZText.builder(model.pack().getTitle()).build();
        this.height = height;
        this.descriptionWidget = new MultiLineTextWidget(getExtendedDescription(model.pack()), font);
        repositionElements();
    }

    private static Component getExtendedDescription(Pack pack) {
        return ComponentUtils.mergeStyles(pack.getPackSource().decorate(pack.getDescription()), Style.EMPTY.withColor(CommonColors.GRAY));
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiGraphicsUtils.texture(graphics, model.icon(), x + SPACING, y, ICON_SIZE, ICON_SIZE);
        nameWidget.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.descriptionWidget.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    public void checkCompatibility(boolean showWarning) {
        if (showWarning) {
            if (!this.warningShown && !model.pack().getCompatibility().isCompatible() && !model.incompatibleWarningsHidden()) {
                nameWidget.setMessage(INCOMPATIBLE_TITLE);
                this.descriptionWidget.setMessage(model.pack().getCompatibility().getDescription());
                this.warningShown = true;
            }
        } else if (this.warningShown) {
            nameWidget.setMessage(model.pack().getTitle());
            nameWidget.setWidth(this.cachedBodyWidth);
            this.descriptionWidget.setMessage(getExtendedDescription(model.pack()));
            this.warningShown = false;
        }
    }
}
