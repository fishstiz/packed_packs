package io.github.fishstiz.packed_packs.gui.components;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import io.github.fishstiz.fidgetz.v0.gui.components.*;
import io.github.fishstiz.fidgetz.v0.gui.components.events.FZHoverableElement;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZFlexLayout;
import io.github.fishstiz.fidgetz.v0.gui.renderables.RenderableRectangle;
import io.github.fishstiz.fidgetz.v0.gui.renderables.Renderables;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.config.DevConfig;
import io.github.fishstiz.packed_packs.config.DevConfig.ResourcePacks.LoadDefaultCondition;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.gui.UiEffect;
import io.github.fishstiz.packed_packs.gui.actions.intents.Intent;
import io.github.fishstiz.packed_packs.gui.actions.intents.ProfileIntent;
import io.github.fishstiz.packed_packs.gui.layouts.ProfileTitleLayout;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksContext;
import io.github.fishstiz.packed_packs.gui.states.PackedPacksState;
import io.github.fishstiz.packed_packs.gui.states.ProfilesState;
import io.github.fishstiz.packed_packs.util.GuiUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.CommonColors;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.github.fishstiz.packed_packs.util.GuiUtils.*;

public class ProfileList extends FZAbstractListWidget<ProfileList.Entry> implements Layout {
    private static final Component EMPTY_TEXT = Component.translatable("packed_packs.profile.empty");
    private final Consumer<? super Intent> dispatcher;
    private final Consumer<UiEffect> effectDispatcher;
    private final PackType packType;
    private ProfilesState state = ProfilesState.empty();
    private boolean devMode;
    private @Nullable Entry dragged;

    public ProfileList(PackedPacksContext context) {
        this.dispatcher = context::dispatch;
        this.effectDispatcher = context::dispatch;
        this.packType = context.packType();
        onStateChanged(context.state().value());
        context.state().subscribe("ProfileList", this::onStateChanged);
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

    @Override
    protected void clearEntries() {
        this.dragged = null;
        super.clearEntries();
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

    private int draggedTop(Entry entry, int mouseY) {
        int height = entry.getHeight();
        return Math.clamp(mouseY - height / 2, getY(), getBottom() - height);
    }

    private int dropIndex(Entry entry, int mouseY) {
        Entry first = children().getFirst();
        int height = entry.getHeight();
        int dropIndex = Math.floorDiv(draggedTop(entry, mouseY) + height / 2 - first.getY(), height);
        return Math.clamp(dropIndex, first.isDefault() ? 1 : 0, children().size() - 1);
    }

    private void drop(Entry dragged, int mouseY) {
        this.dragged = null;
        int index = dropIndex(dragged, mouseY);

        List<Entry> entries = children();
        if (index < 0 || index >= entries.size()) {
            return;
        }

        int from = dragged.getIndex();
        int to = state.profiles().indexOf(entries.get(index).profile);
        if (to == -1 || from == to) {
            return;
        }

        dispatcher.accept(new ProfileIntent.Move(dragged.profile, index));
    }

    private void extractDraggedRenderState(Entry dragged, GuiGraphicsExtractor graphics, int mouseY, float partialTick) {
        int from = dragged.getIndex();
        int height = dragged.getHeight();

        if (mouseY < getY() + height / 2) {
            scroll(-scrollRate() * partialTick);
        } else if (mouseY > getBottom() - height / 2) {
            scroll(scrollRate() * partialTick);
        }

        int dropIndex = dropIndex(dragged, mouseY);
        int start = dropIndex < from ? dropIndex : from + 1;
        int end = dropIndex < from ? from : dropIndex + 1;
        int shift = dropIndex < from ? height : -height;

        List<Entry> entries = children();
        ScreenRectangle bounds = getRectangle();

        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            if (entry != dragged && (i < start || i >= end) && entry.getRectangle().overlaps(bounds)) {
                entry.extractRenderState(graphics, -1, -1, partialTick);
            }
        }

        ScreenRectangle shiftedBounds = new ScreenRectangle(bounds.left(), bounds.top() - shift, bounds.width(), bounds.height());
        graphics.pose().pushMatrix();
        graphics.pose().translate(0, shift);
        for (int i = start; i < end; i++) {
            Entry entry = entries.get(i);
            if (entry.getRectangle().overlaps(shiftedBounds)) {
                entry.extractRenderState(graphics, -1, -1, partialTick);
            }
        }
        graphics.pose().popMatrix();

        graphics.pose().pushMatrix();
        graphics.pose().translate(0, draggedTop(dragged, mouseY) - dragged.getY());
        dragged.extractRenderState(graphics, -1, -1, partialTick);
        graphics.pose().popMatrix();

        graphics.requestCursor(CursorTypes.RESIZE_NS);
    }

    @Override
    protected void extractEntriesRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (dragged != null && getFocused() == dragged) {
            setHovered(null);
            extractDraggedRenderState(dragged, graphics, mouseY, partialTick);
        } else {
            super.extractEntriesRenderState(graphics, mouseX, mouseY, partialTick);
        }

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

    @Override
    public void removeChildren() {
        clearEntries();
    }

    protected final class Entry extends FZAbstractListWidget.Entry implements FZContextMenu.Source {
        private static final RenderableRectangle HANDLE_SPRITE = Renderables.sprite(PackedPacks.id("icon/ellipsis_vertical"));
        private static final RenderableRectangle INACTIVE_HANDLE_SPRITE = Renderables.sprite(
                PackedPacks.id("icon/ellipsis_vertical"),
                CommonColors.GRAY
        );
        private static final Identifier STAR_OUTLINE_SPRITE = PackedPacks.id("icon/star_outline");
        private final List<AbstractWidget> children = new ArrayList<>();
        private final FZFlexLayout layout;
        private final Profile profile;
        private final int index;
        private FZIconButton dragHandle;
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

        private void updateTooltip() {
            String desc = profile.getDescription();
            if (desc.isEmpty()) {
                selectButton.setTooltip(null);
            } else {
                selectButton.setTooltip(Tooltip.create(Component.empty()
                        .append(Component.literal(profile.getName()).withStyle(ChatFormatting.BOLD))
                        .append(CommonComponents.NEW_LINE)
                        .append(Component.literal(desc))
                ));
            }
        }

        private void buildWidgets(boolean isDefault, boolean isLocked) {
            boolean deleteActive = !isLocked && !isDefault;

            this.dragHandle = addChild(layout.child(FZIconButton.builder()
                    .size(16, 20)
                    .id("DragHandle")
                    .icon(new WidgetElements(isDefault ? INACTIVE_HANDLE_SPRITE : HANDLE_SPRITE, 16, 16))
                    .active(!isDefault)
                    .build()));

            this.selectButton = addChild(layout.child(FZButton.builder()
                    .id("SelectButton")
                    .message(Component.literal(profile.getName()))
                    .onPress(() -> dispatcher.accept(new ProfileIntent.Select(profile)))
                    .active(state.selectedProfile() != profile)
                    .build(), layout.flexChildHorizontalSettings()));
            updateTooltip();

            if (!isLocked) {
                addChild(layout.child(FZIconButton.builder()
                        .id("EditButton")
                        .square()
                        .icon(new WidgetElements(PackedPacks.id("icon/edit"), 16, 16))
                        .onPress(() -> effectDispatcher.accept(new UiEffect.OpenDialog((container, closer) ->
                                FZModal.builder(container, ProfileTitleLayout.createForm(profile, dispatcher, () -> {
                                            updateTooltip();
                                            closer.run();
                                        }))
                                        .id("ProfileEditDialog")
                                        .margin(SPACING)
                                        .padding((int) (SPACING * 1.5))
                                        .captureClick()
                                        .captureFocus()
                                        .buildAndOpen()
                        )))
                        .build()));
            }

            addChild(layout.child(FZIconButton.builder()
                    .square()
                    .id("DeleteButton")
                    .message(Component.translatable("packed_packs.profile.delete"))
                    .tooltip(deleteActive ? Component.translatable("packed_packs.profile.delete.info") : null)
                    .icon(getDeleteIcon(isDefault, isLocked))
                    .onPress(() -> dispatcher.accept(new ProfileIntent.Delete(profile)))
                    .active(deleteActive)
                    .build()));

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
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            for (AbstractWidget child : children) {
                child.extractRenderState(graphics, mouseX, mouseY, partialTick);
            }

            if (this.dragHandle.isActive() && this.dragHandle.isHovered()) {
                graphics.requestCursor(CursorTypes.RESIZE_NS);
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
        public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
            if (getFocused() == this.dragHandle) {
                dragged = this;
                return true;
            }

            return super.mouseDragged(event, dx, dy);
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            if (getFocused() == this.dragHandle) {
                drop(this, (int) event.y());
            }

            return super.mouseReleased(event);
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