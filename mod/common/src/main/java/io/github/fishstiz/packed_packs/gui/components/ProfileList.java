package io.github.fishstiz.packed_packs.gui.components;

import io.github.fishstiz.fidgetz.v0.gui.components.*;
import io.github.fishstiz.fidgetz.v0.gui.components.events.FZHoverableElement;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZFlexLayout;
import io.github.fishstiz.fidgetz.v0.gui.state.FZRef;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.config.DevConfig;
import io.github.fishstiz.packed_packs.config.DevConfig.ResourcePacks.LoadDefaultCondition;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.gui.actions.intents.Intent;
import io.github.fishstiz.packed_packs.gui.states.PackedPacksState;
import io.github.fishstiz.packed_packs.gui.states.ProfilesState;
import io.github.fishstiz.packed_packs.gui.actions.intents.ProfileIntent;
import io.github.fishstiz.packed_packs.util.GuiUtils;
import net.minecraft.client.gui.GuiGraphics;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.github.fishstiz.packed_packs.util.GuiUtils.*;

public class ProfileList extends FZAbstractListWidget<ProfileList.Entry> implements Layout {
    private static final Component EMPTY_TEXT = Component.translatable("packed_packs.profile.empty");
    private final Consumer<? super Intent> dispatcher;
    private final PackType packType;
    private ProfilesState state = ProfilesState.empty();
    private boolean devMode;

    public ProfileList(FZRef<PackedPacksState> state, Consumer<? super Intent> dispatcher, PackType packType) {
        this.dispatcher = dispatcher;
        this.packType = packType;
        onStateChanged(state.value());
        state.subscribe("ProfileList", this::onStateChanged);
    }

    @Override
    protected int maxContentWidth() {
        return 0;
    }

    private void onStateChanged(PackedPacksState newState) {
        ProfilesState prev = this.state;
        boolean devModeChanged = devMode != newState.devMode();

        if (prev == newState.profiles() && !devModeChanged) {
            return;
        }

        this.state = newState.profiles();
        this.devMode = newState.devMode();

        if (prev.profiles() != this.state.profiles() || prev.defaultProfile() != this.state.defaultProfile()) {
            rebuildEntries();
        } else {
            for (Entry entry : children()) {
                entry.onStateChanged(devModeChanged);
            }
        }
    }

    private void rebuildEntries() {
        Entry focused = getFocused();
        Profile previousFocused = focused == null ? null : focused.profile;
        double scrollAmount = scrollAmount();

        clearEntries();

        Profile defaultProfile = state.defaultProfile();
        List<Profile> profiles = new ObjectArrayList<>(state.profiles());

        int i = 0;
        if (defaultProfile != null) {
            Entry entry = new Entry(defaultProfile, i++);
            addEntry(entry);
            profiles.remove(defaultProfile);
            if (previousFocused != null
                && (previousFocused == defaultProfile || previousFocused.getId().equals(defaultProfile.getId()))) {
                setFocused(entry);
            }
        }

        for (Profile profile : profiles) {
            Entry entry = new Entry(profile, i++);
            addEntry(entry);
            if (previousFocused != null && previousFocused.getId().equals(profile.getId())) {
                setFocused(entry);
            }
        }

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
            renderScrollingStringOverContents(
                    graphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE),
                    EMPTY_TEXT,
                    0
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

    protected final class Entry extends FZAbstractListWidget.Entry implements FZContextMenu.Source {
        private static final Identifier STAR_OUTLINE_SPRITE = PackedPacks.id("icon/star_outline");
        private final List<AbstractWidget> children = new ArrayList<>();
        private final FZFlexLayout layout;
        private final Profile profile;
        private final int index;
        private FZButton selectButton;
        private boolean prevDefault;
        private boolean prevLocked;

        private Entry(Profile profile, int index) {
            this.index = index;
            this.profile = profile;
            this.layout = FZFlexLayout.horizontal();

            buildWidgets(state.defaultProfile() == profile, profile.isLocked());

            layout.arrangeElements();
        }

        private <T extends AbstractWidget> T addChild(T widget) {
            children.add(widget);
            if (widget instanceof FZHoverableElement hoverableElement) {
                hoverableElement.fidgetz$setHovered(fidgetz$isHovered());
            }
            return widget;
        }

        private void buildWidgets(boolean isDefault, boolean isLocked) {
            boolean deleteActive = !isLocked && !isDefault;

            addChild(layout.child(FZIconButton.builder()
                    .square()
                    .id("DeleteButton")
                    .message(Component.translatable("packed_packs.profile.delete"))
                    .tooltip(deleteActive ? Component.translatable("packed_packs.profile.delete.info") : null)
                    .icon(getDeleteIcon(isDefault, isLocked))
                    .onPress(() -> dispatcher.accept(new ProfileIntent.Delete(profile)))
                    .active(deleteActive)
                    .build()));

            this.selectButton = addChild(layout.child(FZButton.builder()
                    .id("SelectButton")
                    .message(Component.literal(profile.getName()))
                    .onPress(() -> dispatcher.accept(new ProfileIntent.Select(profile)))
                    .active(state.selectedProfile() != profile)
                    .build(), layout.flexChildHorizontalSettings()));

            if (devMode) {
                addChild(layout.child(FZIconButton.builder()
                        .id("DefaultButton")
                        .square()
                        .icon(new WidgetElements(isDefault ? STAR_SPRITE : STAR_OUTLINE_SPRITE, 16, 16))
                        .tooltip(isDefault
                                ? Component.translatable("packed_packs.profile.default.unset")
                                : Component.translatable("packed_packs.profile.default.set"))
                        .onPress(this::toggleDefault)
                        .build()));

                addChild(layout.child(FZIconButton.builder(isLocked ? LOCK_SPRITES : UNLOCK_SPRITES)
                        .id("LockButton")
                        .square()
                        .tooltip(isLocked
                                ? Component.translatable("packed_packs.profile.unlock")
                                : Component.translatable("packed_packs.profile.lock"))
                        .onPress(this::toggleLock)
                        .build()));
            }

            this.prevDefault = isDefault;
            this.prevLocked = isLocked;
        }

        private void rebuildWidgets(boolean isDefault, boolean isLocked) {
            children.clear();
            layout.removeChildren();

            GuiEventListener focused = getFocused();
            setFocused(null);
            buildWidgets(isDefault, isLocked);

            if (focused instanceof FZComponent previousFocusedComponent) {
                String id = previousFocusedComponent.fidgetz$componentId();
                for (AbstractWidget child : children) {
                    if (child instanceof FZComponent component && Objects.equals(id, component.fidgetz$componentId())) {
                        setFocused(child);
                    }
                }
            }

            layout.arrangeElements();
            layout.fidgetz$setWidth(getWidth());
            layout.setPosition(getX(), getY());
        }

        private void onStateChanged(boolean devModeChanged) {
            boolean isDefault = isDefault();
            boolean isLocked = profile.isLocked();

            if (devModeChanged || isDefault != prevDefault || isLocked != prevLocked) {
                rebuildWidgets(isDefault, isLocked);
                return;
            }

            String name = profile.getName();
            if (!name.equals(this.selectButton.getMessage().getString())) {
                this.selectButton.setMessage(Component.literal(name));
            }

            this.selectButton.active = state.selectedProfile() != profile;
        }

        @Override
        public int getIndex() {
            return index;
        }

        private boolean isDefault() {
            return state.defaultProfile() == profile;
        }

        private void toggleDefault() {
            dispatcher.accept(new ProfileIntent.SetDefault(isDefault() ? null : profile));
        }

        private void toggleLock() {
            dispatcher.accept(new ProfileIntent.ToggleLock(profile));
        }

        private static WidgetElements getDeleteIcon(boolean isDefault, boolean isLocked) {
            if (isDefault) {
                return new WidgetElements(STAR_SPRITE, 16, 16);
            } else if (isLocked) {
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

        private static WidgetElements createIcon(Supplier<Identifier> sprite) {
            return padded16Rect(GuiUtils.lazySprite(sprite));
        }

        @Override
        public void fidgetz$updateContextEntries(double x, double y, FZContextMenu.Collector collector) {
            if (!devMode) return;

            collector.nextSection();

            collector.addEntry(builder -> buildDevEntry(builder)
                    .message(Component.translatable("packed_packs.profile.default." + (isDefault() ? "unset" : "set")))
                    .icon(createIcon(() -> isDefault() ? STAR_SPRITE : STAR_OUTLINE_SPRITE))
                    .onPress(this::toggleDefault));

            collector.addEntry(builder -> buildDevEntry(builder
                    .message(Component.translatable("packed_packs.profile." + (state.isLocked() ? "unlock" : "lock")))
                    .icon(createIcon(() -> state.isLocked() ? LOCK_SPRITE_SMALL : UNLOCK_SPRITE_SMALL))
                    .onPress(this::toggleLock)));

            if (!isDefault() || packType == PackType.SERVER_DATA) {
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
