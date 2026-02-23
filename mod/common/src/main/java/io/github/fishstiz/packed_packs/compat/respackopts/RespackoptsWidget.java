package io.github.fishstiz.packed_packs.compat.respackopts;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import io.github.fishstiz.fidgetz.gui.components.Fidgetz;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuProvider;
import io.github.fishstiz.packed_packs.api.PreferenceRegistry;
import io.github.fishstiz.packed_packs.compat.Mod;
import io.github.fishstiz.packed_packs.compat.PackWrapperDelegatorAbstractionEpicModelEntry;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.gui.components.PreferenceToggle;
import io.gitlab.jfronny.libjf.entrywidgets.api.v0.ResourcePackEntryWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class RespackoptsWidget extends AbstractButton implements ContextMenuProvider, Fidgetz {
    private final ResourcePackEntryWidget wrapped;
    private final PackSelectionModel.Entry model;
    private final LayoutElement container;
    private final @Nullable PreferenceToggle toggle;

    private RespackoptsWidget(PreferenceRegistry.Key<Boolean> prefKey, LayoutElement container, ResourcePackEntryWidget wrapped, PackSelectionModel.Entry model) {
        super(0, 0, 0, 0, Component.literal(Mod.RESPACKOPTS.getId()));
        this.toggle = Config.get().isDevMode() ? PreferenceToggle.fromKey(prefKey) : null;
        this.container = container;
        this.wrapped = wrapped;
        this.model = model;
    }

    public static @Nullable RespackoptsWidget create(PreferenceRegistry.Key<Boolean> prefKey, LayoutElement container, Pack pack) {
        var widgets = ResourcePackEntryWidget.WIDGETS;
        if (!widgets.isEmpty()) {
            PackSelectionModel.Entry model = new PackWrapperDelegatorAbstractionEpicModelEntry(pack);
            for (ResourcePackEntryWidget widget : widgets) {
                if (widget.isVisible(model, isSelectable(pack))) {
                    return new RespackoptsWidget(prefKey, container, widget, model);
                }
            }
        }
        return null;
    }

    @Override
    public void onPress(@NonNull InputWithModifiers inputWithModifiers) {
        this.wrapped.onClick(this.model);
    }

    @Override
    protected void renderContents(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int width = this.wrapped.getWidth(this.model);
        int height = this.wrapped.getHeight(this.model, this.container.getHeight());
        int marginRight = this.wrapped.getXMargin(this.model);

        this.setWidth(width);
        this.setHeight(height);
        this.setX((this.container.getX() + this.container.getWidth()) - width - marginRight);
        this.setY(this.container.getY() + (this.container.getHeight() - height) / 2);

        this.isHovered = this.isHovered && Fidgetz.super.isMouseOver(mouseX, mouseY);

        this.wrapped.render(this.model, guiGraphics, this.getX(), this.getY(), this.isHovered, partialTick);

        if (this.toggle != null) {
            this.toggle.render(guiGraphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), partialTick);
        }

        if (this.isHovered()) {
            guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    @Override
    protected void updateWidgetNarration(@NonNull NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return super.isMouseOver(mouseX, mouseY) && Fidgetz.super.isMouseOver(mouseX, mouseY);
    }

    /**
     * Copied from {@code TransferableSelectionList.Entry#showHoverOverlay()}
     */
    private static boolean isSelectable(Pack pack) {
        return !pack.isFixedPosition() || !pack.isRequired();
    }

    @Override
    public void buildItems(ContextMenuItemBuilder builder, int mouseX, int mouseY) {
        if (this.toggle != null) {
            builder.separatorIfNonEmpty().add(this.toggle);
        }
    }
}
