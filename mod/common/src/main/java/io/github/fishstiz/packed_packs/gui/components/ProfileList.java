package io.github.fishstiz.packed_packs.gui.components;

import io.github.fishstiz.fidgetz.v0.gui.components.*;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZFlexLayout;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.gui.model.ProfilesViewModel;
import io.github.fishstiz.packed_packs.util.Colors;
import io.github.fishstiz.packed_packs.util.GuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.github.fishstiz.packed_packs.util.GuiUtils.*;

public class ProfileList extends FZAbstractListWidget<ProfileList.Entry> implements Layout {
    private static final Component EMPTY_TEXT = Component.translatable("packed_packs.profile.empty");
    private final ProfilesViewModel model;

    public ProfileList(ProfilesViewModel model) {
        this.model = model;
        refreshEntries();
        model.subscribe(ProfilesViewModel.Property.ALL, this::refreshEntries);
    }

    @Override
    protected int maxContentWidth() {
        return 0;
    }

    private void refreshEntries() {
        Entry focused = getFocused();
        String focusedId = focused == null ? null : focused.model.id();
        double scrollAmount = scrollAmount();

        clearEntries();

        model.forEachEntry((entryModel, ignored) -> {
            Entry entry = new Entry(entryModel);
            addEntry(entry);
            if (entryModel.id().equals(focusedId)) {
                setFocused(entry);
            }
        });

        repositionEntries();
        setScrollAmount(scrollAmount);
    }

    @Override
    public void setHeight(int height) {
        int previousHeight = getHeight();
        super.setHeight(height);
        if (previousHeight != getHeight()) {
            repositionEntries();
        }
    }

    @Override
    public void setSize(int width, int height) {
        int previousHeight = getHeight();
        super.setSize(width, height);
        if (previousHeight != getHeight()) {
            repositionEntries();
        }
    }

    @Override
    protected void extractEntriesRenderState(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.extractEntriesRenderState(graphics, mouseX, mouseY, partialTick);

        if (children().isEmpty()) {
            renderScrollingString(
                    graphics,
                    Minecraft.getInstance().font,
                    EMPTY_TEXT,
                    getX() + SPACING,
                    getY() + SPACING,
                    getRight() - SPACING,
                    getBottom() - SPACING,
                    Colors.WHITE
            );
        }
    }

    @Override
    protected void extractFocusedRenderState(GuiGraphics graphics, Entry focused) {
    }

    @Override
    public void visitChildren(Consumer<LayoutElement> layoutElementVisitor) {
        layoutElementVisitor.accept(this);
    }

    @Override
    public void arrangeElements() {
        repositionEntries();
    }

    protected static final class Entry extends FZAbstractListWidget.Entry implements FZContextMenu.Source {
        private static final ResourceLocation STAR_OUTLINE_SPRITE = PackedPacks.id("icon/star_outline");
        private static final WidgetSprites LOCK_SPRITES = new WidgetSprites(
                LOCK_SPRITE,
                ResourceLocation.withDefaultNamespace("widget/locked_button_highlighted")
        );
        private static final WidgetSprites UNLOCK_SPRITES = new WidgetSprites(
                ResourceLocation.withDefaultNamespace("widget/unlocked_button"),
                ResourceLocation.withDefaultNamespace("widget/unlocked_button_highlighted")
        );
        private final List<AbstractWidget> children = new ArrayList<>();
        private final FZFlexLayout layout;
        private final ProfilesViewModel.Entry model;

        private Entry(ProfilesViewModel.Entry model) {
            this.model = model;
            this.layout = FZFlexLayout.horizontal();

            boolean deleteActive = !model.isLocked() && !model.isDefault();
            FZIconButton delete = layout.child(FZIconButton.builder()
                    .square()
                    .message(Component.translatable("packed_packs.profile.delete"))
                    .tooltip(deleteActive ? Component.translatable("packed_packs.profile.delete.info") : null)
                    .icon(getDeleteIcon(model))
                    .onPress(model::delete)
                    .active(deleteActive)
                    .build());

            FZButton select = layout.child(FZButton.builder()
                    .message(model.name())
                    .onPress(model::select)
                    .active(!model.isSelected())
                    .build(), layout.flexChildHorizontalSettings());

            children.add(select);
            children.add(delete);

            if (Config.get().isDevMode()) {
                children.add(layout.child(FZIconButton.builder()
                        .square()
                        .icon(new WidgetElements(model.isDefault() ? STAR_SPRITE : STAR_OUTLINE_SPRITE, 16, 16))
                        .tooltip(model.isDefault()
                                ? Component.translatable("packed_packs.profile.default.unset")
                                : Component.translatable("packed_packs.profile.default.set"))
                        .onPress(model::toggleDefault)
                        .build()));

                children.add(layout.child(FZIconButton.builder(model.isLocked() ? LOCK_SPRITES : UNLOCK_SPRITES)
                        .square()
                        .tooltip(model.isLocked()
                                ? Component.translatable("packed_packs.profile.unlock")
                                : Component.translatable("packed_packs.profile.lock"))
                        .onPress(model::toggleLock)
                        .build()));
            }

            layout.arrangeElements();
        }

        private static WidgetElements getDeleteIcon(ProfilesViewModel.Entry entry) {
            if (entry.isDefault()) {
                return new WidgetElements(STAR_SPRITE, 16, 16);
            } else if (entry.isLocked()) {
                return new WidgetElements(LOCK_SPRITE_DISABLED, 20, 20);
            } else {
                return new WidgetElements(TRASH_SPRITE, 16, 16);
            }
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            for (AbstractWidget child : children) {
                child.render(graphics, mouseX, mouseY, partialTick);
            }
        }

        private static WidgetElements createIcon(Supplier<ResourceLocation> sprite) {
            return padded16Rect(GuiUtils.createRect(sprite));
        }

        @Override
        public void fidgetz$updateContextEntries(double x, double y, FZContextMenu.Collector collector) {
            if (!Config.get().isDevMode()) return;

            collector.nextSection();

            collector.addEntry(builder -> buildDevEntry(builder)
                    .message(Component.translatable("packed_packs.profile.default." + (model.isDefault() ? "set" : "unset")))
                    .icon(createIcon(() -> model.isDefault() ? STAR_SPRITE : STAR_OUTLINE_SPRITE))
                    .onPress(model::toggleDefault));

            collector.addEntry(builder -> buildDevEntry(builder
                    .message(Component.translatable("packed_packs.profile." + (model.isLocked() ? "lock" : "unlock")))
                    .icon(createIcon(() -> model.isLocked() ? LOCK_SPRITE_SMALL : UNLOCK_SPRITE_SMALL))
                    .onPress(model::toggleLock)));
        }

        @Override
        public void setX(int x) {
            super.setX(x);
            layout.setX(x);
        }

        @Override
        public void setY(int y) {
            super.setY(y);
            layout.setY(y);
        }

        @Override
        protected void setWidth(int width) {
            super.setWidth(width);
            layout.fidgetz$setWidth(getWidth());
        }

        @Override
        protected void setBounds(int x, int y, int width, int height) {
            super.setBounds(x, y, width, height);
            layout.setPosition(x, y);
            layout.fidgetz$setWidth(width);
        }

        @Override
        public int getHeight() {
            return layout.getHeight();
        }

        @Override
        public List<AbstractWidget> children() {
            return children;
        }
    }
}
