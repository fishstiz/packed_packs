package io.github.fishstiz.packed_packs.gui.components.pack;

import io.github.fishstiz.fidgetz.gui.components.AbstractLayoutElement;
import io.github.fishstiz.fidgetz.gui.components.FidgetzText;
import io.github.fishstiz.packed_packs.gui.model.PackListViewModel;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.util.CommonColors;
import org.jspecify.annotations.NonNull;

class PackWidget extends AbstractLayoutElement implements Renderable {
    private static final Component INCOMPATIBLE_TITLE = Component.translatable("pack.incompatible");
    private static final int DESCRIPTION_LINES = 2;
    private final Minecraft minecraft;
    private final PackListViewModel.Entry viewModel;
    private final int spacing;
    private final FidgetzText<Void> nameWidget;
    private MultiLineTextWidget descriptionWidget;
    private int cachedBodyWidth;
    private boolean warningShown;

    PackWidget(Minecraft minecraft, PackListViewModel.Entry viewModel, int height, int spacing) {
        this.minecraft = minecraft;
        this.viewModel = viewModel;
        this.nameWidget = FidgetzText.<Void>builder()
                .setMessage(viewModel.pack().getTitle())
                .setHeight(minecraft.font.lineHeight)
                .setColor(ChatFormatting.WHITE.getColor())
                .setShadow(true)
                .build();
        this.spacing = spacing;
        this.setHeight(height);
        this.updateDescriptionWidget();
    }

    private int getIconSize() {
        return this.getHeight();
    }

    private Component getExtendedDescription() {
        return ComponentUtils.mergeStyles(
                this.viewModel.pack().getPackSource().decorate(this.viewModel.pack().getDescription()),
                Style.EMPTY.withColor(CommonColors.GRAY)
        );
    }

    private void updateDescriptionWidget() {
        this.descriptionWidget = new MultiLineTextWidget(this.getExtendedDescription(), this.minecraft.font);
        this.descriptionWidget.setMaxRows(DESCRIPTION_LINES);
        this.descriptionWidget.setMaxWidth(this.cachedBodyWidth);
        this.descriptionWidget.setCentered(false);
        this.warningShown = false;
    }

    @Override
    public void setWidth(int width) {
        if (this.getWidth() == width) return;
        super.setWidth(width);

        int bodyX = this.spacing * 2 + this.getX() + this.getIconSize();
        int bodyWidth = this.getRight() - this.spacing * 2 - bodyX;
        this.cachedBodyWidth = bodyWidth;

        this.nameWidget.setX(bodyX);
        this.nameWidget.setWidth(bodyWidth);
        this.updateDescriptionWidget();
    }

    public int getContentLeft() {
        return this.nameWidget.getX();
    }

    protected void renderSprite(GuiGraphics guiGraphics) {
        int x = this.getX() + this.spacing;
        int y = this.getY();
        int size = this.getIconSize();
        this.viewModel.sprite().render(guiGraphics, x, y, size, size);
    }

    @Override
    public void render(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderSprite(guiGraphics);

        int lineHeight = this.minecraft.font.lineHeight;
        int totalContentHeight = lineHeight + spacing + (lineHeight * DESCRIPTION_LINES);
        int startY = this.getY() + (this.getHeight() - totalContentHeight) / 2;

        this.nameWidget.setY(startY);
        this.nameWidget.renderWidget(guiGraphics, mouseX, mouseY, partialTick);

        this.descriptionWidget.setPosition(this.nameWidget.getX(), startY + lineHeight + this.spacing);
        this.descriptionWidget.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    public void checkCompatibility(boolean showWarning) {
        if (showWarning) {
            if (!this.warningShown && !this.viewModel.pack().getCompatibility().isCompatible() && !this.viewModel.incompatibleWarningsHidden()) {
                this.nameWidget.setMessage(INCOMPATIBLE_TITLE);
                this.descriptionWidget.setMessage(this.viewModel.pack().getCompatibility().getDescription());
                this.warningShown = true;
            }
        } else if (this.warningShown) {
            this.nameWidget.setMessage(this.viewModel.pack().getTitle());
            this.nameWidget.setWidth(this.cachedBodyWidth);
            this.descriptionWidget.setMessage(this.getExtendedDescription());
            this.warningShown = false;
        }
    }
}
