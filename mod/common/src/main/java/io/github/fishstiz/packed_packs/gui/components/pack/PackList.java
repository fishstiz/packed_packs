package io.github.fishstiz.packed_packs.gui.components.pack;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import io.github.fishstiz.fidgetz.gui.components.*;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuContainer;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.renderables.ColoredRect;
import io.github.fishstiz.fidgetz.gui.renderables.GradientRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.GuiSprite;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.util.DrawUtil;
import io.github.fishstiz.fidgetz.util.GuiUtil;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.api.events.ContextMenuEvent;
import io.github.fishstiz.packed_packs.api.events.InitializePackEntryEvent;
import io.github.fishstiz.packed_packs.config.Preferences;
import io.github.fishstiz.packed_packs.gui.FocusPathProvider;
import io.github.fishstiz.packed_packs.gui.FocusTarget;
import io.github.fishstiz.packed_packs.gui.components.MouseSelectionHandler;
import io.github.fishstiz.packed_packs.gui.components.PreferenceToggle;
import io.github.fishstiz.packed_packs.gui.components.contextmenu.PackMenuHeader;
import io.github.fishstiz.packed_packs.gui.model.PackListKey;
import io.github.fishstiz.packed_packs.gui.model.PackListUtils;
import io.github.fishstiz.packed_packs.gui.model.PackListViewModel;
import io.github.fishstiz.packed_packs.gui.states.ActiveAction;
import io.github.fishstiz.packed_packs.impl.PackedPacksApiImpl;
import io.github.fishstiz.packed_packs.impl.events.ContextMenuEventImpl;
import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.SelectableEntry;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

import static io.github.fishstiz.fidgetz.util.GuiUtil.playClickSound;
import static io.github.fishstiz.packed_packs.util.InputUtil.*;
import static io.github.fishstiz.packed_packs.util.ResourceUtil.getVanilla;
import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.*;
import static io.github.fishstiz.fidgetz.util.lang.ObjectsUtil.*;

public class PackList extends AbstractFixedListWidget<PackList.Entry> implements FocusPathProvider, ContainerEventHandlerPatch, ContextMenuContainer {
    private static final int Y_OFFSET = 1;
    private static final int ITEM_HEIGHT = 35;
    private static final int ROW_GAP = 3;
    private static final double SCROLL_STEP = 10;
    private static final int DROP_INDEX_PADDING = 3;
    private final ScreenContext screenContext;
    private final PackListViewModel viewModel;
    private Theme dropTheme;
    private ColoredRect dropRect;
    private ColoredRect dropIndexRect;
    private GradientRect scrollUpRect;
    private GradientRect scrollDownRect;
    private boolean scrolling;

    public PackList(ScreenContext screenContext, PackListViewModel viewModel) {
        super(ITEM_HEIGHT);
        this.screenContext = screenContext;
        this.viewModel = viewModel;
        this.applyTheme();
        this.refresh();
        this.viewModel.subscribe(PackListViewModel.Property.PACKS, this::refresh);
        this.viewModel.subscribe(PackListViewModel.Property.SELECTION, this::refreshSelected);
    }

    public PackListKey key() {
        return this.viewModel.key();
    }

    private void applyTheme() {
        if (this.viewModel.supportsReordering()) {
            this.dropTheme = Theme.GREEN_500;
            this.dropIndexRect = new ColoredRect(dropTheme.getARGB());
            this.scrollUpRect = GradientRect.fromTop(dropTheme.withAlpha(0.75f), dropTheme.withAlpha(0));
            this.scrollDownRect = this.scrollUpRect.flip();
        } else {
            this.dropTheme = Theme.RED_700;
            this.dropRect = new ColoredRect(dropTheme.withAlpha(0.25f));
        }
    }

    private void refresh() {
        Entry focused = this.getFocused();

        this.setFocused(null);
        this.setSelected(null);

        this.clearEntries();

        this.viewModel.forEachEntry((entry, i) -> {
            Entry listEntry = new Entry(entry, i);
            this.addEntry(listEntry);

            if (entry.selectedLast()) {
                this.setSelected(listEntry);
            }
            if (focused != null && focused.pack().equals(entry.pack())) {
                this.setFocused(listEntry);
            }
        });

        this.clampScrollAmount();
    }

    private void refreshSelected() {
        Entry selected = this.selected;
        if (selected != null && !selected.viewModel.selectedLast()) {
            this.selected = null;
        }
    }

    @Override
    public void setSelected(@Nullable Entry selected) {
        if (selected == null || selected.viewModel.selectedLast()) {
            this.selected = selected;
        }
    }

    @Override
    public @Nullable Entry getSelected() {
        Entry selected = super.getSelected();
        if (selected == null && !this.viewModel.hasSelection()) {
            return this.selected = null;
        }
        if (selected != null && selected.viewModel.selectedLast()) {
            return selected;
        }
        for (Entry entry : this.children()) {
            if (entry.viewModel.selectedLast()) {
                return this.selected = entry;
            }
        }
        return selected;
    }

    public void scrollToTop() {
        this.setScrollAmount(0);
    }

    public void scrollToLastSelected() {
        ifPresent(this.getSelected(), this::scrollToEntry);
    }

    private void scrollStep(boolean up, float partialTick) {
        double scrollAmount = this.scrollAmount();
        if (up) {
            scrollAmount -= SCROLL_STEP * partialTick;
        } else {
            scrollAmount += SCROLL_STEP * partialTick;
        }

        this.scrolling = true;
        this.setClampedScrollAmount(scrollAmount);
    }

    private int getDropIndex(double mouseY) {
        if (this.children().isEmpty()) return -1;

        int index = this.getRowIndex(mouseY);
        if (index == -1) return -1;

        Entry entry = this.getEntry(index);
        int centerY = entry.getY() + (entry.getHeight() / 2);

        if (mouseY >= centerY) {
            int next = index + 1;
            return next < this.children().size() ? next : -1;
        }
        return index;
    }

    private boolean isMouseOverSelectionEntry(SequencedCollection<Pack> selection, double mouseX, double mouseY, int index) {
        if (index < 0 || index >= this.children().size()) return false;

        Entry entry = this.children().get(index);
        if (entry == null) return false;

        return entry.isMouseOver(mouseX, mouseY) && selection.contains(entry.pack());
    }

    private boolean isMouseOverSelection(SequencedCollection<Pack> selection, double mouseX, double mouseY, int index) {
        return this.isMouseOverSelectionEntry(selection, mouseX, mouseY, index - 1) || this.isMouseOverSelectionEntry(selection, mouseX, mouseY, index);
    }

    private boolean canDropAt(ActiveAction.Dragging dragging, int mouseX, int mouseY, int index) {
        if (this.scrolling || (dragging.target() == this.key() && this.isMouseOverSelection(dragging.payload(), mouseX, mouseY, index))) {
            return false;
        }
        return this.viewModel.canDrop(dragging.target(), dragging.ctx().pack(), dragging.payload(), index);
    }

    private void renderDropIndex(GuiGraphics guiGraphics, int x, int width, int index) {
        int rowTop = Math.clamp(
                this.getRowTop(index != -1 ? index : this.children().size()),
                this.getY() + this.offsetY + DROP_INDEX_PADDING,
                this.getBottom() - this.rowGap - DROP_INDEX_PADDING
        );
        int indexY = rowTop - this.rowGap - DROP_INDEX_PADDING;

        guiGraphics.enableScissor(this.getX(), this.getY(), this.getRight(), this.getBottom());
        this.dropIndexRect.render(guiGraphics, x, indexY, width, rowTop - indexY + DROP_INDEX_PADDING);
        guiGraphics.disableScissor();
    }

    private void renderDroppableSlots(GuiGraphics guiGraphics, ActiveAction.Dragging dragging, int mouseX, int mouseY, float partialTick) {
        int x = this.getX();
        int y = this.getY();
        int width = this.scrollbarVisible() ? this.getWidth() - this.scrollbarOffset : this.getWidth();
        int height = this.getHeight();
        int bottom = this.getBottom();

        if (this.isMouseOver(mouseX, mouseY)) {
            double scrollAmount = this.scrollAmount();

            int scrollDownY = bottom - this.getItemHeight();
            if (scrollAmount < this.maxScrollAmount() && mouseY >= scrollDownY) {
                this.scrollDownRect.render(guiGraphics, x, scrollDownY, width, this.getItemHeight());
                this.scrollStep(false, partialTick);
            } else if (scrollAmount > 0 && mouseY <= y + this.getItemHeight()) {
                this.scrollUpRect.render(guiGraphics, x, y, width, this.getItemHeight());
                this.scrollStep(true, partialTick);
            } else {
                this.scrolling = false;
            }

            int index = this.getDropIndex(mouseY);
            if (this.canDropAt(dragging, mouseX, mouseY, index)) {
                this.renderDropIndex(guiGraphics, x, width, index);
            }
        }

        DrawUtil.renderOutline(guiGraphics, x, y, width, height, dropTheme.getARGB());
    }

    private void renderDroppableRect(GuiGraphics guiGraphics, ActiveAction.Dragging dragging, int mouseX, int mouseY, float partialTick) {
        if (this.canDropAt(dragging, mouseX, mouseY, 0)) {
            if (this.isMouseOver(mouseX, mouseY)) {
                this.dropRect.render(guiGraphics, this.getX(), this.getY(), width, this.getHeight(), partialTick);
            }
            DrawUtil.renderOutline(guiGraphics, this.getX(), this.getY(), width, this.getHeight(), this.dropTheme.getARGB());
        }
    }

    public void renderDroppableZone(GuiGraphics guiGraphics, ActiveAction.Dragging dragging, int mouseX, int mouseY, float partialTick) {
        if (!this.viewModel.locked() && PackListUtils.canInteract(dragging.target(), this.key())) {
            if (this.viewModel.supportsReordering()) {
                this.renderDroppableSlots(guiGraphics, dragging, mouseX, mouseY, partialTick);
            } else {
                this.renderDroppableRect(guiGraphics, dragging, mouseX, mouseY, partialTick);
            }
        }
    }

    public void onDrop(ActiveAction.Dragging dragging, int mouseX, int mouseY) {
        int index = this.getDropIndex(mouseY);
        if (this.canDropAt(dragging, mouseX, mouseY, index)) {
            this.viewModel.applyDrop(dragging.target(), dragging.ctx(), dragging.payload(), index);
        } else {
            this.viewModel.cancelDrop(dragging.target(), dragging.ctx(), dragging.payload());
        }
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
        if (this.children().isEmpty()) return null;

        Entry selectedEntry = firstNonNull(this.getFocused(), this.getSelected());
        return switch (direction) {
            case UP -> selectedEntry != null && this.children().getFirst().equals(selectedEntry)
                    ? selectedEntry
                    : this.getPreviousEntry(selectedEntry);
            case DOWN -> selectedEntry != null && this.children().getLast().equals(selectedEntry)
                    ? selectedEntry
                    : this.getNextEntry(selectedEntry);
            case LEFT -> this.key().type().available() ? selectedEntry : null;
            case RIGHT -> this.key().type().enabled() ? selectedEntry : null;
        };
    }

    @Override
    public @Nullable ComponentPath getFocusPath(FocusTarget target) {
        if (this.children().isEmpty()) return null;

        Entry targetEntry = switch (target) {
            case FocusTarget.LastSelected ignored -> this.getSelected();
            case FocusTarget.PackEntry entry -> this.getEntry(entry.packId());
        };

        return targetEntry == null ? null : new ListPath(this, targetEntry, target.scroll(), false);
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(@NonNull FocusNavigationEvent event) {
        if (this.children().isEmpty()) return null;

        Entry next = switch (event) {
            case FocusNavigationEvent.InitialFocus ignored -> firstNonNull(this.getFocused(), this.getSelected());
            case FocusNavigationEvent.TabNavigation ignored -> this.isFocused()
                    ? null
                    : Objects.requireNonNullElse(this.getSelected(), this.children().getFirst());
            case FocusNavigationEvent.ArrowNavigation(ScreenDirection direction) -> this.isFocused()
                    ? this.getNextEntryAt(direction)
                    : Objects.requireNonNullElse(this.getSelected(), this.children().getFirst());
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
            if (this.select && !this.child.viewModel.selectedLast()) {
                this.child.viewModel.select();
            }
        }
    }

    private void selectOnKeyPress(@Nullable Entry entry) {
        if (entry == null) return;
        if (isRangeModifierActive()) {
            entry.viewModel.selectRange();
        } else {
            entry.viewModel.selectExclusive();
        }
        this.setFocused(entry);
        this.scrollToEntry(entry);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent keyEvent) {
        if (isSelectAll(keyEvent)) {
            this.viewModel.selectAll();
            return true;
        }
        if (super.keyPressed(keyEvent)) {
            return true;
        }
        if (isUp(keyEvent) || isDown(keyEvent)) {
            this.selectOnKeyPress(this.getNextEntryAt(isUp(keyEvent) ? ScreenDirection.UP : ScreenDirection.DOWN));
            return true;
        }
        if (isHome(keyEvent) || isEnd(keyEvent)) {
            if (this.children().isEmpty()) return true;
            this.selectOnKeyPress(isHome(keyEvent) ? this.children().getFirst() : this.children().getLast());
            return true;
        }
        if (isPageUp(keyEvent) || isPageDown(keyEvent)) {
            if (this.children().isEmpty()) return true;
            int pageSize = Math.max(1, this.getHeight() / this.getItemHeight());
            Entry selected = firstNonNull(this.getFocused(), this.getSelected());
            int currentIndex = selected != null ? this.children().indexOf(selected) : 0;
            int targetIndex = isPageUp(keyEvent)
                    ? Math.max(0, currentIndex - pageSize)
                    : Math.min(this.children().size() - 1, currentIndex + pageSize);
            Entry entry = this.children().get(targetIndex);
            this.selectOnKeyPress(entry);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
        boolean scrolling = this.updateScrolling(mouseButtonEvent);
        return ContainerEventHandlerPatch.super.mouseClicked(mouseButtonEvent, doubleClicked) || scrolling;
    }

    @Override
    public @NonNull List<Entry> children() {
        return this.children;
    }

    @Override
    protected void renderListItems(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderListItems(guiGraphics, mouseX, mouseY, partialTick);

        Entry focused = this.getFocused();
        if (focused != null && focused.isFocused() && this.children().contains(focused)) {
            int outlineTop = focused.getY();
            int outlineHeight = focused.getHeight() + Y_OFFSET;
            DrawUtil.renderOutline(guiGraphics, focused.getX(), outlineTop, focused.getWidth(), outlineHeight, Theme.WHITE.getARGB());
        }
    }

    @Override
    protected void renderItem(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, Entry item) {
        item.ensureInitialized();
        super.renderItem(guiGraphics, mouseX, mouseY, partialTick, item);
    }

    @Override
    public int maxScrollAmount() {
        int maxScrollAmount = super.maxScrollAmount();
        return maxScrollAmount > 0 ? maxScrollAmount + Y_OFFSET : maxScrollAmount;
    }

    public class Entry extends AbstractFixedListWidget<Entry>.Entry implements SelectableEntry, ContainerEventHandlerPatch, ContextMenuContainer {
        private static final int V_MARGIN = ROW_GAP / 2 + Y_OFFSET;
        private static final int BACKGROUND_MARGIN = 1;
        private static final int H_SPACING = 2;
        private static final int ICON_SIZE = ITEM_HEIGHT - ROW_GAP;
        private static final Tooltip FOLDER_OPEN_INFO = Tooltip.create(FolderPack.FOLDER_OPEN_TEXT);
        private static final ColoredRect SELECTED_OVERLAY = new ColoredRect(Theme.BLUE_500.withAlpha(0.25F));
        private static final Sprite SELECT_HIGHLIGHTED_SPRITE = GuiSprite.of32(getVanilla("transferable_list/select_highlighted"));
        private static final Sprite SELECT_SPRITE = GuiSprite.of32(getVanilla("transferable_list/select"));
        private static final Sprite UNSELECT_HIGHLIGHTED_SPRITE = GuiSprite.of32(getVanilla("transferable_list/unselect_highlighted"));
        private static final Sprite UNSELECT_SPRITE = GuiSprite.of32(getVanilla("transferable_list/unselect"));
        private static final Sprite MOVE_UP_HIGHLIGHTED_SPRITE = GuiSprite.of32(getVanilla("transferable_list/move_up_highlighted"));
        private static final Sprite MOVE_UP_SPRITE = GuiSprite.of32(getVanilla("transferable_list/move_up"));
        private static final Sprite MOVE_DOWN_HIGHLIGHTED_SPRITE = GuiSprite.of32(getVanilla("transferable_list/move_down_highlighted"));
        private static final Sprite MOVE_DOWN_SPRITE = GuiSprite.of32(getVanilla("transferable_list/move_down"));
        private final PackListViewModel.Entry viewModel;
        private final MouseSelectionHandler selectionHandler;
        private final List<GuiEventListener> children = new ObjectArrayList<>();
        private final List<Renderable> renderables = new ObjectArrayList<>();
        private final List<Renderable> topRenderables = new ObjectArrayList<>();
        private @Nullable PackWidget packWidget;
        private @Nullable FidgetzButton<Void> folderWidget;
        private @Nullable PackListDevMenu devMenu;
        private boolean initialized;

        Entry(PackListViewModel.Entry viewModel, int index) {
            super(index);
            this.viewModel = viewModel;
            this.selectionHandler = new MouseSelectionHandler(this, viewModel::selected, viewModel::selectedLast, viewModel::selectedExclusive);
        }

        private void ensureInitialized() {
            if (!this.initialized) this.init();
        }

        private void init() {
            this.packWidget = this.addRenderableOnly(new PackWidget(PackList.this.minecraft, this.viewModel, ITEM_HEIGHT - ROW_GAP, H_SPACING));
            this.folderWidget = this.addTopLayer(this.viewModel.folder().flatMap(pack ->
                    PreferenceToggle.bind(Preferences.FOLDER_PACK_WIDGET, FidgetzButton.<Void>builder())
                            .map(bound -> bound.value().setTooltip(FOLDER_OPEN_INFO)
                                    .setHeight(this.packWidget.getHeight() / 3)
                                    .makeSquare()
                                    .setContextMenuBuilder(bound.apply(toggle -> (btn, b) -> toggle.updateBuilder(b)))
                                    .setForeground(bound.toggle())
                                    .setSprite(HAMBURGER_SPRITE)
                                    .setOnPress(this.viewModel::openFolder)
                                    .build())).orElse(null));

            if (PackList.this.screenContext.devMode()) {
                this.devMenu = this.viewModel.devMenu(PackList.this.minecraft);
            }

            this.initialized = true;

            InitializePackEntryEvent event = new InitializePackEntryEvent(PackList.this.screenContext, this.viewModel, this, this::addTopLayer);
            PackedPacksApiImpl.getInstance().eventBus().post(event);
        }

        public Pack pack() {
            return this.viewModel.pack();
        }

        public <T extends Renderable> T addRenderableOnly(T renderable) {
            if (renderable == null) return null;
            this.renderables.add(renderable);
            return renderable;
        }

        public <T extends GuiEventListener> T prependWidget(T widget) {
            if (widget == null) return null;
            this.children.addFirst(widget);
            return widget;
        }

        public <T extends Renderable> T addTopRenderableOnly(T renderable) {
            if (renderable == null) return null;
            this.topRenderables.add(renderable);
            return renderable;
        }

        public <T extends GuiEventListener & Renderable> T addTopLayer(T widget) {
            if (widget == null) return null;
            return this.addTopRenderableOnly(this.prependWidget(widget));
        }

        private boolean handleMouseAction(MouseSelectionHandler.Action action) {
            if (!action.shouldDispatch()) return false;

            switch (action) {
                case SELECT -> this.viewModel.select();
                case SELECT_TOGGLE -> this.viewModel.selectToggle();
                case SELECT_EXCLUSIVE -> this.viewModel.selectExclusive();
                case SELECT_RANGE -> this.viewModel.selectRange();
                case DRAG -> this.viewModel.drag();
                case TRANSFER -> {
                    this.viewModel.transfer();
                    return false;
                }
            }

            return true;
        }

        private boolean isFocusedOrSelected() {
            return PackList.this.getFocused() == null ? this.viewModel.selectedLast() : this.isFocused();
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return PackList.this.beforeScrollbarX(mouseX) && super.isMouseOver(mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(@NonNull MouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
            if (ContainerEventHandlerPatch.super.mouseClicked(mouseButtonEvent, doubleClicked) || isRightClick(mouseButtonEvent)) {
                if (!PackList.this.viewModel.isFolderOpened()) {
                    return true;
                }
                if (this.folderWidget != null) {
                    // ideally folderWidget should return false on #shouldTakeFocusAfterInteraction,
                    // but that does not exist on older versions
                    this.folderWidget.setFocused(false);
                }
                return false;
            }

            if (isLeftClick(mouseButtonEvent)) {
                int relativeX = (int) mouseButtonEvent.x() - (this.getX() + H_SPACING);
                int relativeY = (int) mouseButtonEvent.y() - (this.getY() + V_MARGIN);

                if (this.viewModel.canEnable() && this.mouseOverIcon(relativeX, relativeY, ICON_SIZE)) {
                    this.viewModel.enable();
                    playClickSound();
                    return false;
                }

                if (this.viewModel.canDisable() && this.mouseOverLeftHalf(relativeX, relativeY, ICON_SIZE)) {
                    this.viewModel.disable();
                    playClickSound();
                    return false;
                }

                if (this.viewModel.canMoveUp() && this.mouseOverTopRightQuarter(relativeX, relativeY, ICON_SIZE)) {
                    this.viewModel.moveUp();
                    playClickSound();
                    return false;
                }

                if (this.viewModel.canMoveDown() && this.mouseOverBottomRightQuarter(relativeX, relativeY, ICON_SIZE)) {
                    this.viewModel.moveDown();
                    playClickSound();
                    return false;
                }
            }

            return this.handleMouseAction(this.selectionHandler.mouseClicked(mouseButtonEvent));
        }

        @Override
        public boolean mouseReleased(@NonNull MouseButtonEvent mouseButtonEvent) {
            return this.handleMouseAction(this.selectionHandler.mouseReleased(mouseButtonEvent));
        }

        @Override
        public boolean mouseDragged(@NonNull MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
            return this.handleMouseAction(this.selectionHandler.mouseDragged(mouseButtonEvent, dragX, dragY));
        }

        @Override
        public boolean keyPressed(@NonNull KeyEvent keyEvent) {
            if (super.keyPressed(keyEvent)) {
                return true;
            }
            if (isExpandFolder(keyEvent) && this.viewModel.folder().isPresent() && this.viewModel.selectedExclusive()) {
                this.viewModel.openFolder();
                return true;
            }
            if (isTransfer(keyEvent) && this.viewModel.canTransfer()) {
                this.viewModel.transfer();
                return true;
            }
            // we only check #supportsReordering rather than #canMoveDown or #canMoveUp
            // as the entry may not be movable, but the selection might.
            if (isMoveDown(keyEvent) && PackList.this.viewModel.supportsReordering()) {
                this.viewModel.moveDown();
                return true;
            }
            if (isMoveUp(keyEvent) && PackList.this.viewModel.supportsReordering()) {
                this.viewModel.moveUp();
                return true;
            }
            if (isOpenFile(keyEvent)) {
                PackUtil.openPack(this.pack());
                return true;
            }
            if (isOpenFolder(keyEvent)) {
                PackUtil.openParent(this.pack());
                return true;
            }
            if (isDelete(keyEvent) && this.viewModel.fileModifiable()) {
                this.viewModel.delete();
                return true;
            }
            if (isRename(keyEvent) && this.viewModel.fileModifiable()) {
                this.viewModel.openRename();
                return true;
            }
            return false;
        }

        public void renderBack(GuiGraphics guiGraphics, int top, int left, int width, int height) {
            if (!this.pack().getCompatibility().isCompatible() && !this.viewModel.incompatibleWarningsHidden()) {
                int backgroundLeft = left + BACKGROUND_MARGIN;
                int backgroundTop = top + BACKGROUND_MARGIN;
                int backgroundRight = backgroundLeft + width - BACKGROUND_MARGIN * 2;
                int backgroundBottom = backgroundTop + height - BACKGROUND_MARGIN;

                guiGraphics.fill(backgroundLeft, backgroundTop, backgroundRight, backgroundBottom, Theme.RED_900.getARGB());
            }
        }

        private void updateCursor(GuiGraphics guiGraphics, boolean hovered) {
            if (hovered) {
                guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
            }
        }

        private void renderForeground(GuiGraphics guiGraphics, int top, int left, int mouseX, int mouseY, boolean hovering) {
            if (!hovering && !this.viewModel.selectedLast()) return;

            int x = left + H_SPACING;
            int relativeX = mouseX - x;
            int relativeY = mouseY - top;

            WHITE_OVERLAY.render(guiGraphics, x, top, ICON_SIZE, ICON_SIZE);

            if (this.viewModel.canEnable()) {
                boolean hovered = hovering && this.mouseOverIcon(relativeX, relativeY, ICON_SIZE);
                pick(hovered, SELECT_HIGHLIGHTED_SPRITE, SELECT_SPRITE).render(guiGraphics, x, top);
                this.updateCursor(guiGraphics, hovered);
            }

            if (this.viewModel.canDisable()) {
                boolean hovered = hovering && this.mouseOverLeftHalf(relativeX, relativeY, ICON_SIZE);
                pick(hovered, UNSELECT_HIGHLIGHTED_SPRITE, UNSELECT_SPRITE).render(guiGraphics, x, top);
                this.updateCursor(guiGraphics, hovered);
            }

            if (this.viewModel.canMoveUp()) {
                boolean hovered = hovering && this.mouseOverTopRightQuarter(relativeX, relativeY, ICON_SIZE);
                pick(hovered, MOVE_UP_HIGHLIGHTED_SPRITE, MOVE_UP_SPRITE).render(guiGraphics, x, top);
                this.updateCursor(guiGraphics, hovered);
            }

            if (this.viewModel.canMoveDown()) {
                boolean hovered = hovering && this.mouseOverBottomRightQuarter(relativeX, relativeY, ICON_SIZE);
                pick(hovered, MOVE_DOWN_HIGHLIGHTED_SPRITE, MOVE_DOWN_SPRITE).render(guiGraphics, x, top);
                this.updateCursor(guiGraphics, hovered);
            }
        }

        private void renderSelection(GuiGraphics guiGraphics, int top, int left, int width, int height) {
            if (this.viewModel.selected()) {
                pick(this.viewModel.selectedLast(), WHITE_OVERLAY, SELECTED_OVERLAY).render(guiGraphics, left, top, width, height);
                DrawUtil.renderOutline(guiGraphics, left, top, width, height, Theme.BLUE_500.getARGB());
            }
        }

        private void renderTop(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            if (this.folderWidget != null) {
                int folderWidgetY = this.getBottom() - this.folderWidget.getHeight() - BACKGROUND_MARGIN;
                this.folderWidget.setPosition(this.packWidget.getContentLeft(), folderWidgetY);
            }

            for (Renderable renderable : this.topRenderables) {
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }

        @Override
        public void renderContent(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovering, float partialTick) {
            hovering = hovering && PackList.this.beforeScrollbarX(mouseX) && GuiUtil.isHovered(this, mouseX, mouseY);

            int left = this.getX();
            int top = this.getY();
            int width = this.getWidth();
            int height = this.getHeight();
            int innerTop = top + V_MARGIN;
            int innerHeight = height - ROW_GAP;

            this.packWidget.setPosition(left, innerTop);
            this.packWidget.setWidth(width);

            this.renderBack(guiGraphics, top, left, width, height);

            this.packWidget.checkCompatibility(hovering || this.isFocusedOrSelected());

            for (Renderable renderable : this.renderables) {
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            }

            this.renderSelection(guiGraphics, top, left, width, height + Y_OFFSET);
            this.renderForeground(guiGraphics, innerTop, left, mouseX, mouseY, hovering);
            this.renderTop(guiGraphics, mouseX, mouseY, partialTick);

            if (this.devMenu != null) {
                this.devMenu.render(guiGraphics, innerTop, left, width, innerHeight, partialTick);
            }
        }

        @Override
        public void buildItems(ContextMenuItemBuilder builder, int mouseX, int mouseY) {
            if (!this.initialized) return;

            var extensions = ContextMenuEventImpl.postPackEntry(PackList.this.screenContext, this.viewModel);

            ContextMenuContainer.super.buildItems(builder
                            .whenNonNull(extensions.getItems(ContextMenuEvent.PackEntry.Pos.BEFORE_HEADER))
                            .ifTrue((items, b) -> b.addAll(items))
                            .add(new PackMenuHeader(this.pack(), this.viewModel.sprite()))
                            .whenNonNull(extensions.getItems(ContextMenuEvent.PackEntry.Pos.AFTER_HEADER))
                            .ifTrue((items, b) -> b.addAll(items))
                            .whenNonNull(this.devMenu)
                            .ifTrue((menu, b) -> menu.buildItems(b, mouseX, mouseY))
                            .whenNonNull(extensions.getItems(ContextMenuEvent.PackEntry.Pos.AFTER_DEV))
                            .ifTrue((items, b) -> b.addAll(items))
                            .whenNonNull(this.folderWidget)
                            .ifTrue(b -> b
                                    .simpleItem(FolderPack.FOLDER_OPEN_TEXT, this.viewModel::openFolder)
                                    .separator()
                            )
                            .when(this.viewModel.fileModifiable())
                            .ifTrue(b -> b
                                    .simpleItem(RENAME_FILE_TEXT, this.viewModel::fileModifiable, this.viewModel::openRename)
                                    .simpleItem(DELETE_FILE_TEXT, this.viewModel::fileModifiable, this.viewModel::delete)
                                    .simpleItem(OPEN_FILE_TEXT, () -> PackUtil.openPack(this.pack()))
                                    .simpleItem(OPEN_PARENT_TEXT, () -> PackUtil.openParent(this.pack()))
                            )
                            .whenNonNull(extensions.getItems(ContextMenuEvent.PackEntry.Pos.AFTER_HEADER))
                            .ifTrue((items, b) -> b.addAll(items)),
                    mouseX,
                    mouseY
            );
        }

        @Override
        public @NonNull List<GuiEventListener> children() {
            return this.children;
        }

        @Override
        public @NonNull List<NarratableEntry> narratables() {
            return Collections.emptyList();
        }
    }
}
