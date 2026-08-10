package io.github.fishstiz.packed_packs.gui.components;

import io.github.fishstiz.fidgetz.v0.gui.components.*;
import io.github.fishstiz.fidgetz.v0.gui.components.events.FZHoverableElement;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZFlexLayout;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.config.DevConfig;
import io.github.fishstiz.packed_packs.config.DevConfig.ResourcePacks.LoadDefaultCondition;
import io.github.fishstiz.packed_packs.gui.model.ProfilesViewModel;
import io.github.fishstiz.packed_packs.util.GuiUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.github.fishstiz.packed_packs.util.GuiUtils.*;

public class ProfileList extends FZAbstractListWidget<ProfileList.Entry> implements Layout {
    private static final Component EMPTY_TEXT = Component.translatable("packed_packs.profile.empty");
    private final ScreenContext context;
    private final ProfilesViewModel model;

    public ProfileList(ScreenContext context, ProfilesViewModel model) {
        this.context = context;
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

        model.forEachEntry((entryModel, _) -> {
            Entry entry = new Entry(context, entryModel);
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
    protected void extractEntriesRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractEntriesRenderState(graphics, mouseX, mouseY, partialTick);

        if (children().isEmpty()) {
            extractScrollingStringOverContents(
                    graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE),
                    EMPTY_TEXT,
                    0
            );
        }
    }

    @Override
    protected void extractFocusedRenderState(GuiGraphicsExtractor graphics, Entry focused) {
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
        private static final Identifier STAR_OUTLINE_SPRITE = PackedPacks.id("icon/star_outline");
        private static final WidgetSprites LOCK_SPRITES = new WidgetSprites(
                LOCK_SPRITE,
                Identifier.withDefaultNamespace("widget/locked_button_highlighted")
        );
        private static final WidgetSprites UNLOCK_SPRITES = new WidgetSprites(
                Identifier.withDefaultNamespace("widget/unlocked_button"),
                Identifier.withDefaultNamespace("widget/unlocked_button_highlighted")
        );
        private final List<AbstractWidget> children = new ArrayList<>();
        private final FZFlexLayout layout;
        private final ProfilesViewModel.Entry model;
        private final ScreenContext context;

        private Entry(ScreenContext context, ProfilesViewModel.Entry model) {
            this.model = model;
            this.context = context;
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

            for (AbstractWidget child : children) {
                if (child instanceof FZHoverableElement hoverableElement) {
                    hoverableElement.fidgetz$setHovered(fidgetz$isHovered());
                }
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
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            for (AbstractWidget child : children) {
                child.extractRenderState(graphics, mouseX, mouseY, partialTick);
            }
        }

        private static WidgetElements createIcon(Supplier<Identifier> sprite) {
            return padded16Rect(GuiUtils.createRect(sprite));
        }

        @Override
        public void fidgetz$updateContextEntries(double x, double y, FZContextMenu.Collector collector) {
            if (!Config.get().isDevMode()) return;

            collector.nextSection();

            collector.addEntry(builder -> buildDevEntry(builder)
                    .message(Component.translatable("packed_packs.profile.default." + (model.isDefault() ? "unset" : "set")))
                    .icon(createIcon(() -> model.isDefault() ? STAR_SPRITE : STAR_OUTLINE_SPRITE))
                    .onPress(model::toggleDefault));

            collector.addEntry(builder -> buildDevEntry(builder
                    .message(Component.translatable("packed_packs.profile." + (model.isLocked() ? "unlock" : "lock")))
                    .icon(createIcon(() -> model.isLocked() ? LOCK_SPRITE_SMALL : UNLOCK_SPRITE_SMALL))
                    .onPress(model::toggleLock)));

            if (context.isServerData()) {
                return;
            }

            DevConfig.ResourcePacks config = DevConfig.get().getResourcepacks();

            FZPopoverMenuItem.Builder loadConditionEntry = buildDevEntry(FZPopoverMenuItem.builder())
                    .message(Component.translatable("packed_packs.profile.default.resource_pack.load"))
                    .tooltip(Tooltip.create(Component.translatable("packed_packs.profile.default.resource_pack.load.info")));

            loadConditionEntry.child(GuiUtils.buildDevEntry(FZPopoverMenuItem.builder())
                    .message(Component.translatable("packed_packs.profile.default.resource_pack.load.no_options_or_version"))
                    .tooltip(Tooltip.create(Component.translatable("packed_packs.profile.default.resource_pack.load.no_options_or_version.info")))
                    .icon(GuiUtils.toggleRect(() -> config.getLoadDefaultCondition() == LoadDefaultCondition.NO_OPTIONS_OR_VERSION_FILE))
                    .onPress(() -> DevConfig.get().getResourcepacks().setLoadDefaultCondition(LoadDefaultCondition.NO_OPTIONS_OR_VERSION_FILE))
                    .closeOnInteraction(false)
                    .build());

            loadConditionEntry.child(GuiUtils.buildDevEntry(FZPopoverMenuItem.builder())
                    .message(Component.translatable("packed_packs.profile.default.resource_pack.load.no_options"))
                    .tooltip(Tooltip.create(Component.translatable("packed_packs.profile.default.resource_pack.load.no_options.info")))
                    .icon(GuiUtils.toggleRect(() -> config.getLoadDefaultCondition() == LoadDefaultCondition.NO_OPTIONS_FILE))
                    .onPress(() -> DevConfig.get().getResourcepacks().setLoadDefaultCondition(LoadDefaultCondition.NO_OPTIONS_FILE))
                    .closeOnInteraction(false)
                    .build());

            collector.addEntry(loadConditionEntry.build());
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
