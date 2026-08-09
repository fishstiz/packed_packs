package io.github.fishstiz.packed_packs.gui.components;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import io.github.fishstiz.fidgetz.v0.gui.components.*;
import io.github.fishstiz.fidgetz.v0.gui.components.events.FZHoverableElement;
import io.github.fishstiz.fidgetz.v0.gui.renderables.RenderableRectangle;
import io.github.fishstiz.fidgetz.v0.gui.renderables.Renderables;
import io.github.fishstiz.fidgetz.v0.utils.CollectionUtils;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.api.events.ContextMenuEvent;
import io.github.fishstiz.packed_packs.api.events.InitializePackEntryEvent;
import io.github.fishstiz.packed_packs.api.gui.ElementSink;
import io.github.fishstiz.packed_packs.config.Preferences;
import io.github.fishstiz.packed_packs.gui.ContainerEventHandlerPatch;
import io.github.fishstiz.packed_packs.gui.FocusPathProvider;
import io.github.fishstiz.packed_packs.gui.FocusTarget;
import io.github.fishstiz.packed_packs.gui.model.PackListKey;
import io.github.fishstiz.packed_packs.gui.model.PackListUtils;
import io.github.fishstiz.packed_packs.gui.model.PackListViewModel;
import io.github.fishstiz.packed_packs.gui.states.ActiveAction;
import io.github.fishstiz.packed_packs.impl.PackedPacksApiImpl;
import io.github.fishstiz.packed_packs.impl.events.ContextMenuEventImpl;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.packed_packs.util.Colors;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.SelectableEntry;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.Nullable;

import java.util.*;

import static io.github.fishstiz.packed_packs.util.InputUtil.*;
import static io.github.fishstiz.packed_packs.util.GuiUtils.*;

public class PackList extends FZAbstractListWidget<PackList.Entry> implements FocusPathProvider, ContainerEventHandlerPatch {
    private static final int INNER_ITEM_PADDING = 2;
    private static final int INNER_ITEM_HEIGHT = 32;
    private static final int ITEM_HEIGHT = INNER_ITEM_HEIGHT + INNER_ITEM_PADDING * 2;
    private static final int DROP_INDEX_PADDING = 3;
    private static final double SCROLL_RATE = (double) ITEM_HEIGHT / 2;
    private final ScreenContext screenContext;
    private final PackListViewModel listModel;
    private int dropColor;
    private int dropRectColor;
    private int scrollColorFrom;
    private int scrollColorTo;
    private boolean scrolling;
    private boolean initialized;

    public PackList(ScreenContext screenContext, PackListViewModel listModel) {
        this.screenContext = screenContext;
        this.listModel = listModel;
        this.applyTheme();
        this.listModel.subscribe(PackListViewModel.Property.PACKS, this::refreshEntries);
    }

    public PackListKey key() {
        return listModel.key();
    }

    @Override
    protected int maxContentWidth() {
        return 0;
    }

    @Override
    protected int rowSpacing() {
        return -1;
    }

    @Override
    public double scrollRate() {
        return SCROLL_RATE;
    }

    private void applyTheme() {
        if (listModel.supportsReordering()) {
            this.dropColor = Colors.GREEN_500;
            this.scrollColorFrom = Colors.alpha(dropColor, 0.75f);
            this.scrollColorTo = Colors.alpha(dropColor, 0);
        } else {
            this.dropColor = Colors.RED_700;
            this.dropRectColor = Colors.alpha(Colors.RED_700, 0.25f);
        }
    }

    void initializeEntries() {
        if (!this.initialized) {
            refreshEntries();
            this.initialized = true;
        }
    }

    private void refreshEntries() {
        Entry previousFocused = this.getFocused();
        double previousScrollAmount = scrollAmount();
        clearEntries();
        listModel.forEachEntry((entry, i) -> {
            Entry listEntry = new Entry(entry, i);
            addEntry(listEntry);
            if (previousFocused != null && previousFocused.pack().equals(entry.pack())) {
                setFocused(listEntry);
            }
        });
        repositionEntries();
        setScrollAmount(previousScrollAmount);
    }

    public void scrollToTop() {
        this.setScrollAmount(0);
    }

    public void scrollToLastSelected() {
        Entry selected = getSelected();
        if (selected != null) scrollToEntry(selected);
    }

    private int getDropIndex(double mouseY) {
        if (children().isEmpty()) return -1;

        Entry entry = getHovered();
        if (entry == null) return -1;

        int index = entry.index;

        int centerY = entry.getY() + (entry.getHeight() / 2);
        if (mouseY >= centerY) {
            int next = index + 1;
            return next < children().size() ? next : -1;
        }

        return index;
    }

    private boolean isMouseOverSelectionEntry(SequencedCollection<Pack> selection, double mouseX, double mouseY, int index) {
        if (index < 0 || index >= children().size()) return false;
        Entry entry = children().get(index);
        return entry.isMouseOver(mouseX, mouseY) && selection.contains(entry.pack());
    }

    private boolean isMouseOverSelection(SequencedCollection<Pack> selection, double mouseX, double mouseY, int index) {
        return isMouseOverSelectionEntry(selection, mouseX, mouseY, index - 1) || isMouseOverSelectionEntry(selection, mouseX, mouseY, index);
    }

    private boolean canDropAt(ActiveAction.Dragging dragging, int mouseX, int mouseY, int index) {
        if (this.scrolling || (dragging.target() == key() && isMouseOverSelection(dragging.payload(), mouseX, mouseY, index))) {
            return false;
        }
        return listModel.canDrop(dragging.target(), dragging.ctx().pack(), dragging.payload(), index);
    }

    private void renderDroppableSlots(GuiGraphicsExtractor graphics, ActiveAction.Dragging dragging, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int width = scrollbarVisible() ? getWidth() - scrollbarWidth() : getWidth();
        int height = getHeight();
        int bottom = getBottom();

        if (isHovered()) {
            double scrollAmount = scrollAmount();

            int scrollDownY = bottom - INNER_ITEM_HEIGHT;
            if (scrollAmount < maxScrollAmount() && mouseY >= scrollDownY) {
                graphics.fillGradient(x, scrollDownY, x + width, scrollDownY + INNER_ITEM_HEIGHT, scrollColorTo, scrollColorFrom);
                scroll(scrollRate() * partialTick);
                this.scrolling = true;
            } else if (scrollAmount > 0 && mouseY <= y + INNER_ITEM_HEIGHT) {
                graphics.fillGradient(x, y, x + width, y + INNER_ITEM_HEIGHT, scrollColorFrom, scrollColorTo);
                scroll(-(scrollRate() * partialTick));
                this.scrolling = true;
            } else {
                this.scrolling = false;
            }

            int index = getDropIndex(mouseY);
            if (canDropAt(dragging, mouseX, mouseY, index)) {
                int rowTop;
                if (index != -1) {
                    rowTop = children().get(index).getY();
                } else {
                    Entry last = children().isEmpty() ? null : children().getLast();
                    rowTop = last == null ? getBottom() : last.getY() + last.getHeight();
                }

                rowTop = Math.clamp(rowTop, getY() + DROP_INDEX_PADDING, getBottom() - DROP_INDEX_PADDING);
                int indexY = rowTop - DROP_INDEX_PADDING;

                graphics.enableScissor(getX(), getY(), getRight(), getBottom());
                graphics.fill(x, indexY, x + width, indexY + (rowTop - indexY + DROP_INDEX_PADDING), dropColor);
                graphics.disableScissor();
            }
        }

        graphics.outline(x, y, width, height, dropColor);
    }

    private void renderDroppableRect(GuiGraphicsExtractor graphics, ActiveAction.Dragging dragging, int mouseX, int mouseY) {
        if (canDropAt(dragging, mouseX, mouseY, 0)) {
            if (isHovered()) {
                graphics.fill(getX(), getY(), getRight(), getBottom(), dropRectColor);
            }
            graphics.outline(getX(), getY(), getWidth(), getHeight(), dropColor);
        }
    }

    public void renderDroppableZone(GuiGraphicsExtractor guiGraphics, ActiveAction.Dragging dragging, int mouseX, int mouseY, float partialTick) {
        if (!listModel.locked() && PackListUtils.canInteract(dragging.target(), key())) {
            if (listModel.supportsReordering()) {
                renderDroppableSlots(guiGraphics, dragging, mouseX, mouseY, partialTick);
            } else {
                renderDroppableRect(guiGraphics, dragging, mouseX, mouseY);
            }
        }
    }

    public void onDrop(ActiveAction.Dragging dragging, int mouseX, int mouseY) {
        int index = getDropIndex(mouseY);
        if (canDropAt(dragging, mouseX, mouseY, index)) {
            listModel.applyDrop(dragging.target(), dragging.ctx(), dragging.payload(), index);
        } else {
            listModel.cancelDrop(dragging.target(), dragging.ctx(), dragging.payload());
        }
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        Entry previous = getFocused();
        if (previous != focused) {
            super.setFocused(focused);
            if (previous != null && previous.packWidget != null) {
                previous.packWidget.checkCompatibility(false);
            }
            if (focused instanceof Entry entry && entry.packWidget != null) {
                entry.packWidget.checkCompatibility(true);
            }
        }
    }

    private @Nullable Entry getSelected() {
        for (Entry entry : children()) {
            if (entry.entryModel.selectedLast()) {
                return entry;
            }
        }
        return null;
    }

    private @Nullable Entry getFocusedOrSelected() {
        Entry focused = getFocused();
        return focused == null ? getSelected() : focused;
    }

    private @Nullable Entry getEntry(String packId) {
        for (Entry entry : this.children()) {
            if (entry.pack().getId().equals(packId)) {
                return entry;
            }
        }
        return null;
    }

    private @Nullable Entry getNextEntryAt(ScreenDirection direction) {
        if (children().isEmpty()) return null;
        Entry selectedEntry = getFocusedOrSelected();
        return switch (direction) {
            case UP -> Objects.equals(selectedEntry, children().getFirst())
                    ? null
                    : getPreviousEntry(selectedEntry);
            case DOWN -> Objects.equals(selectedEntry, children().getLast())
                    ? null
                    : getNextEntry(selectedEntry);
            case LEFT -> key().type().available() ? selectedEntry : null;
            case RIGHT -> key().type().enabled() ? selectedEntry : null;
        };
    }

    @Override
    public @Nullable ComponentPath getFocusPath(FocusTarget target) {
        if (this.children().isEmpty()) return null;

        Entry targetEntry = switch (target) {
            case FocusTarget.LastSelected ignored -> getSelected();
            case FocusTarget.PackEntry entry -> getEntry(entry.packId());
        };

        return targetEntry == null ? null : new ListPath(this, targetEntry, target.scroll(), false);
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
        if (children().isEmpty()) return null;

        Entry next = switch (event) {
            case FocusNavigationEvent.InitialFocus ignored -> getFocusedOrSelected();
            case FocusNavigationEvent.TabNavigation ignored -> isFocused()
                    ? null
                    : Objects.requireNonNullElse(getSelected(), children().getFirst());
            case FocusNavigationEvent.ArrowNavigation(ScreenDirection direction, _) -> isFocused()
                    ? getNextEntryAt(direction)
                    : Objects.requireNonNullElse(getSelected(), children().getFirst());
            default -> null;
        };

        return next == null ? null : ListPath.path(this, next);
    }

    record ListPath(PackList component, Entry child, boolean scroll, boolean select) implements ComponentPath {
        static ListPath path(PackList component, Entry child) {
            return new ListPath(component, child, true, true);
        }

        @Override
        public void applyFocus(boolean focused) {
            this.child.setFocused(focused);
            if (!focused) {
                this.component.setFocused(null);
                return;
            }
            this.component.setFocused(this.child);
            if (this.scroll) {
                this.component.scrollToEntry(this.child);
            }
            if (this.select && !this.child.entryModel.selectedLast()) {
                this.child.entryModel.selectExclusive();
            }
        }

        @Override
        public GuiEventListener leafComponent() {
            return child;
        }
    }

    private void selectOnKeyPress(@Nullable Entry entry) {
        if (entry == null) return;
        if (isRangeModifierActive()) {
            entry.entryModel.selectRange();
        } else {
            entry.entryModel.selectExclusive();
        }
        setFocused(entry);
        scrollToEntry(entry);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (isSelectAll(keyEvent)) {
            listModel.selectAll();
            return true;
        }
        if (super.keyPressed(keyEvent)) {
            return true;
        }
        if (isUp(keyEvent) || isDown(keyEvent)) {
            Entry nextEntry = getNextEntryAt(isUp(keyEvent) ? ScreenDirection.UP : ScreenDirection.DOWN);
            selectOnKeyPress(nextEntry);
            return nextEntry != null;
        }
        if (isHome(keyEvent) || isEnd(keyEvent)) {
            if (children().isEmpty()) return true;
            selectOnKeyPress(isHome(keyEvent) ? children().getFirst() : children().getLast());
            return true;
        }
        if (isPageUp(keyEvent) || isPageDown(keyEvent)) {
            if (children().isEmpty()) return true;
            int pageSize = Math.max(1, getHeight() / ITEM_HEIGHT);
            Entry selected = getFocusedOrSelected();
            int currentIndex = selected != null ? children().indexOf(selected) : 0;
            int targetIndex = isPageUp(keyEvent)
                    ? Math.max(0, currentIndex - pageSize)
                    : Math.min(children().size() - 1, currentIndex + pageSize);
            Entry entry = children().get(targetIndex);
            selectOnKeyPress(entry);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
        boolean scrolling = updateScrolling(mouseButtonEvent);
        return ContainerEventHandlerPatch.super.mouseClicked(mouseButtonEvent, doubleClicked) || scrolling;
    }

    @Override
    protected void extractEntriesRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!listModel.isFolderOpened()) {
            super.extractEntriesRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }

    public class Entry extends FZAbstractListWidget.Entry implements SelectableEntry, ContainerEventHandlerPatch, FZContextMenu.Source, ElementSink {
        private static final int ICON_SIZE = 32;
        private static final Tooltip FOLDER_OPEN_INFO = Tooltip.create(FolderPack.FOLDER_OPEN_TEXT);
        private static final RenderableRectangle SELECTED_OVERLAY = Renderables.fill(Colors.alpha(Colors.BLUE_500, 0.25f));
        private static final WidgetRenderables SELECT_SPRITES = new WidgetRenderables(
                Renderables.sprite(Identifier.withDefaultNamespace("transferable_list/select")),
                Renderables.sprite(Identifier.withDefaultNamespace("transferable_list/select_highlighted"))
        );
        private static final WidgetRenderables UNSELECT_SPRITES = new WidgetRenderables(
                Renderables.sprite(Identifier.withDefaultNamespace("transferable_list/unselect")),
                Renderables.sprite(Identifier.withDefaultNamespace("transferable_list/unselect_highlighted"))
        );
        private static final WidgetRenderables MOVE_UP_SPRITES = new WidgetRenderables(
                Renderables.sprite(Identifier.withDefaultNamespace("transferable_list/move_up")),
                Renderables.sprite(Identifier.withDefaultNamespace("transferable_list/move_up_highlighted"))
        );
        private static final WidgetRenderables MOVE_DOWN_SPRITES = new WidgetRenderables(
                Renderables.sprite(Identifier.withDefaultNamespace("transferable_list/move_down")),
                Renderables.sprite(Identifier.withDefaultNamespace("transferable_list/move_down_highlighted"))
        );
        private final int index;
        private final PackListViewModel.Entry entryModel;
        private final MouseStateHandler mouseStateHandler;
        private final List<GuiEventListener> children = new ObjectArrayList<>();
        private final List<Renderable> renderables = new ObjectArrayList<>();
        private List<LayoutElement> rightElements = Collections.emptyList();
        private @Nullable PackWidget packWidget;
        private @Nullable AbstractWidget folderWidget;
        private @Nullable PackListDevMenu devMenu;
        private boolean initialized;

        Entry(PackListViewModel.Entry entryModel, int index) {
            super(ITEM_HEIGHT);
            this.index = index;
            this.entryModel = entryModel;
            this.mouseStateHandler = new MouseStateHandler(this, entryModel);
        }

        private void init() {
            if (this.initialized) return;

            this.packWidget = new PackWidget(entryModel, INNER_ITEM_HEIGHT);
            repositionPackWidget();

            entryModel.folder().flatMap(_ -> PreferenceHelper.wrap(Preferences.FOLDER_PACK_WIDGET, FZIconButton.builder()
                            .size(INNER_ITEM_HEIGHT / 3, INNER_ITEM_HEIGHT / 3)
                            .icon(new WidgetElements(HAMBURGER_RECT, 8, 8))
                            .tooltip(FOLDER_OPEN_INFO)
                            .focusOnInteraction(false)
                            .onPress(entryModel::openFolder)
                            .build()))
                    .ifPresent(widget -> {
                        int x = getX() + PackWidget.ICON_SIZE + INNER_ITEM_PADDING * 2;
                        int y = getY() + getHeight() - widget.getHeight() - INNER_ITEM_PADDING;
                        widget.setPosition(x, y);
                        acceptWidget(widget);
                        acceptRenderable(widget);
                        this.folderWidget = widget;
                    });

            this.devMenu = screenContext.devMode() ? entryModel.createDevMenu() : null;

            this.initialized = true;

            InitializePackEntryEvent event = new InitializePackEntryEvent(screenContext, entryModel, packWidget, this);
            PackedPacksApiImpl.getInstance().eventBus().post(event);
        }

        public Pack pack() {
            return entryModel.pack();
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public void acceptWidget(GuiEventListener widget) {
            if (widget instanceof FZHoverableElement hoverable && widget instanceof AbstractWidget abstractWidget) {
                hoverable.fidgetz$setHovered(abstractWidget.isHovered());
            }
            children.addFirst(widget);
        }

        @Override
        public void acceptRenderable(Renderable renderable) {
            renderables.add(renderable);
        }

        @Override
        public void acceptElement(LayoutElement element) {
            this.rightElements = CollectionUtils.addLast(rightElements, element);
        }

        @Override
        public void setFocused(boolean focused) {
            super.setFocused(focused);
            if (!focused && getFocused() != null) {
                getFocused().setFocused(false);
            }
        }

        private boolean isFocusedOrSelected() {
            return PackList.this.getFocused() == null ? entryModel.selectedLast() : isFocused();
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
            if (ContainerEventHandlerPatch.super.mouseClicked(mouseButtonEvent, doubleClicked) || isRightClick(mouseButtonEvent)) {
                if (!listModel.isFolderOpened()) return true;
                // ideally folderWidget should return false on #shouldTakeFocusAfterInteraction,
                // #shouldTakeFocusAfterInteraction also does not bubble up,
                // we want to return false regardless so that the folder can be focused
                this.setFocused(null);
                return false;
            }

            if (isLeftClick(mouseButtonEvent) && fidgetz$getHovered() == null) {
                int relativeX = (int) mouseButtonEvent.x() - (getX() + INNER_ITEM_PADDING);
                int relativeY = (int) mouseButtonEvent.y() - (getY() + INNER_ITEM_PADDING);

                if (entryModel.canEnable() && mouseOverIcon(relativeX, relativeY, ICON_SIZE)) {
                    entryModel.enable();
                    return false;
                }
                if (entryModel.canDisable() && mouseOverLeftHalf(relativeX, relativeY, ICON_SIZE)) {
                    entryModel.disable();
                    return false;
                }
                if (entryModel.canMoveUp() && mouseOverTopRightQuarter(relativeX, relativeY, ICON_SIZE)) {
                    entryModel.moveUp();
                    return false;
                }
                if (entryModel.canMoveDown() && mouseOverBottomRightQuarter(relativeX, relativeY, ICON_SIZE)) {
                    this.entryModel.moveDown();
                    return false;
                }
            }

            return mouseStateHandler.mouseClicked(mouseButtonEvent);
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
            return mouseStateHandler.mouseReleased(mouseButtonEvent);
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
            return mouseStateHandler.mouseDragged(mouseButtonEvent, dragX, dragY);
        }

        @Override
        public boolean keyPressed(KeyEvent keyEvent) {
            if (super.keyPressed(keyEvent)) {
                return true;
            }
            if (isExpandFolder(keyEvent) && entryModel.folder().isPresent() && entryModel.selectedExclusive()) {
                entryModel.openFolder();
                return true;
            }
            if (isTransfer(keyEvent) && listModel.supportsTransferring()) {
                entryModel.transfer();
                return true;
            }
            if (isMoveDown(keyEvent) && listModel.supportsReordering()) {
                entryModel.moveDown();
                return true;
            }
            if (isMoveUp(keyEvent) && listModel.supportsReordering()) {
                entryModel.moveUp();
                return true;
            }
            if (isOpenFile(keyEvent)) {
                PackUtil.openPack(pack());
                return true;
            }
            if (isOpenFolder(keyEvent)) {
                PackUtil.openParent(pack());
                return true;
            }
            if (isDelete(keyEvent) && entryModel.fileModifiable()) {
                entryModel.delete();
                return true;
            }
            if (isRename(keyEvent) && entryModel.fileModifiable()) {
                entryModel.openRename();
                return true;
            }
            return false;
        }

        public void renderBack(GuiGraphicsExtractor graphics, int top, int left, int width, int height) {
            if (!pack().getCompatibility().isCompatible() && !entryModel.incompatibleWarningsHidden()) {
                int margin = INNER_ITEM_PADDING / 2;
                int backgroundLeft = left + margin;
                int backgroundTop = top + margin;
                int backgroundRight = (left + width) - margin;
                int backgroundBottom = (top + height) - margin;
                graphics.fill(backgroundLeft, backgroundTop, backgroundRight, backgroundBottom, Colors.RED_900);
            }
        }

        private void renderWidgetSprites(GuiGraphicsExtractor graphics, WidgetRenderables sprites, int left, int top, boolean hovered) {
            RenderableRectangle sprite = hovered ? sprites.enabledFocused() : sprites.enabled();
            sprite.extractRenderState(graphics, left, top, ICON_SIZE, ICON_SIZE, 0, 0, 0);
            if (hovered) {
                graphics.requestCursor(CursorTypes.POINTING_HAND);
            }
        }

        private void renderWidgetSprites(GuiGraphicsExtractor graphics, int top, int left, int mouseX, int mouseY) {
            boolean hovered = isHovered() && fidgetz$getHovered() == null;
            if (!hovered && !this.entryModel.selectedLast()) return;

            int relX = mouseX - left;
            int relY = mouseY - top;

            WHITE_OVERLAY.extractRenderState(graphics, left, top, ICON_SIZE, ICON_SIZE, 0, 0, 0);

            if (entryModel.canEnable()) {
                renderWidgetSprites(graphics, SELECT_SPRITES, left, top, hovered && mouseOverIcon(relX, relY, ICON_SIZE));
            }
            if (entryModel.canDisable()) {
                renderWidgetSprites(graphics, UNSELECT_SPRITES, left, top, hovered && mouseOverLeftHalf(relX, relY, ICON_SIZE));
            }
            if (entryModel.canMoveUp()) {
                renderWidgetSprites(graphics, MOVE_UP_SPRITES, left, top, hovered && mouseOverTopRightQuarter(relX, relY, ICON_SIZE));
            }
            if (entryModel.canMoveDown()) {
                renderWidgetSprites(graphics, MOVE_DOWN_SPRITES, left, top, hovered && mouseOverBottomRightQuarter(relX, relY, ICON_SIZE));
            }
        }

        private void renderSelection(GuiGraphicsExtractor graphics, int top, int left, int width, int height) {
            if (entryModel.selected()) {
                RenderableRectangle overlay = entryModel.selectedLast() ? WHITE_OVERLAY : SELECTED_OVERLAY;
                overlay.extractRenderState(graphics, left, top, width, height, 0, 0, 0);
                graphics.outline(left, top, width, height, Colors.BLUE_500);
            }
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            init();
            int left = getX();
            int top = getY();
            int width = getWidth();
            int height = getHeight();
            int innerTop = top + INNER_ITEM_PADDING;
            int innerLeft = left + INNER_ITEM_PADDING;

            renderBack(graphics, top, left, width, height);

            Objects.requireNonNull(packWidget).checkCompatibility(isHovered() || isFocusedOrSelected());
            packWidget.extractRenderState(graphics, mouseX, mouseY, partialTick);

            renderSelection(graphics, top, left, width, height);
            renderWidgetSprites(graphics, innerTop, innerLeft, mouseX, mouseY);

            for (Renderable renderable : this.renderables) {
                renderable.extractRenderState(graphics, mouseX, mouseY, partialTick);
            }

            if (devMenu != null) {
                devMenu.render(graphics, innerTop, left, width);
            }
        }

        @Override
        public void fidgetz$updateContextEntries(double x, double y, FZContextMenu.Collector collector) {
            if (!this.initialized) return;

            ContextMenuEventImpl<ContextMenuEvent.PackEntry.Pos> extensions = ContextMenuEventImpl.postPackEntry(
                    screenContext,
                    entryModel
            );

            extensions.entries(ContextMenuEvent.PackEntry.Pos.BEFORE_HEADER).forEach(collector::addEntry);

            collector.addEntry(builder -> builder
                    .message(pack().getTitle())
                    .icon(padded16Rect(Renderables.texture(entryModel.icon(), 32, 32)))
                    .background(Renderables.fill(Colors.GRAY_500))
                    .onPress(e -> {
                        e.context().closeMenu();
                        return false;
                    })
                    .allowAutoDivideAfterEntry(false)
                    .applyCursorChangeWhenActive(false));

            collector.nextSection();

            extensions.entries(ContextMenuEvent.PackEntry.Pos.AFTER_HEADER).forEach(collector::addEntry);

            if (devMenu != null) {
                devMenu.updateContextEntries(collector);
                collector.nextSection();
            }

            extensions.entries(ContextMenuEvent.PackEntry.Pos.AFTER_DEV).forEach(collector::addEntry);

            if (entryModel.folder().isPresent()) {
                collector.addEntry(builder -> builder
                        .message(FolderPack.FOLDER_OPEN_TEXT)
                        .onPress(entryModel::openFolder));
                collector.nextSection();
            }

            if (entryModel.fileModifiable()) {
                collector.addEntry(builder -> builder
                        .message(RENAME_FILE_TEXT)
                        .active(entryModel::fileModifiable)
                        .onPress(entryModel::openRename));
                collector.addEntry(builder -> builder
                        .message(DELETE_FILE_TEXT)
                        .active(entryModel::fileModifiable)
                        .onPress(entryModel::delete));
            }

            if (PackUtil.validatePackPath(pack()) != null) {
                collector.addEntry(builder -> builder.message(OPEN_FILE_TEXT).onPress(() -> PackUtil.openPack(pack())));
                collector.addEntry(builder -> builder.message(OPEN_PARENT_TEXT).onPress(() -> PackUtil.openParent(pack())));
            }

            extensions.entries(ContextMenuEvent.PackEntry.Pos.AFTER_PACK).forEach(collector::addEntry);

            FZContextMenu.Source.super.fidgetz$updateContextEntries(x, y, collector);
        }

        @Override
        public List<GuiEventListener> children() {
            return this.children;
        }

        @Override
        public boolean isHovered() {
            return super.isHovered() && fidgetz$isHovered();
        }

        private void repositionPackWidget() {
            if (packWidget != null) {
                packWidget.setPosition(getX(), getY() + INNER_ITEM_PADDING);
                packWidget.setWidth(getWidth());
            }
        }

        @Override
        public void setX(int x) {
            rightElements.forEach(child -> child.setX(child.getX() + x - getX()));
            if (folderWidget != null) folderWidget.setX(folderWidget.getX() + x - getX());
            super.setX(x);
            repositionPackWidget();
        }

        @Override
        public void setY(int y) {
            rightElements.forEach(child -> child.setY(child.getY() + y - getY()));
            if (folderWidget != null) folderWidget.setY(folderWidget.getY() + y - getY());
            super.setY(y);
            repositionPackWidget();
        }

        @Override
        protected void setWidth(int width) {
            rightElements.forEach(child -> child.setX(child.getX() + width - getWidth()));
            super.setWidth(width);
            repositionPackWidget();
        }

        @Override
        protected void setHeight(int height) {
            super.setHeight(height);
            repositionPackWidget();
        }

        @Override
        protected void setBounds(int x, int y, int width, int height) {
            rightElements.forEach(child -> child.setPosition((child.getX() + x - getX()) + width - getWidth(), child.getY() + y - getY()));
            if (folderWidget != null) {
                folderWidget.setPosition(folderWidget.getX() + x - getX(), folderWidget.getY() + y - getY());
            }
            super.setBounds(x, y, width, height);
            repositionPackWidget();
        }
    }
}
