package io.github.fishstiz.packed_packs.gui.components;

import io.github.fishstiz.fidgetz.v0.gui.components.*;
import io.github.fishstiz.fidgetz.v0.gui.components.events.FZHoverableElement;
import io.github.fishstiz.fidgetz.v0.gui.renderables.RenderableRectangle;
import io.github.fishstiz.fidgetz.v0.gui.renderables.Renderables;
import io.github.fishstiz.fidgetz.v0.utils.CollectionUtils;
import io.github.fishstiz.packed_packs.api.context.PackContext;
import io.github.fishstiz.packed_packs.api.events.ContextMenuEvent;
import io.github.fishstiz.packed_packs.api.events.InitializePackEntryEvent;
import io.github.fishstiz.packed_packs.api.gui.ElementSink;
import io.github.fishstiz.packed_packs.compat.minecraftcursor.MinecraftCursor;
import io.github.fishstiz.packed_packs.config.Preferences;
import io.github.fishstiz.packed_packs.gui.FocusPathProvider;
import io.github.fishstiz.packed_packs.gui.FocusTarget;
import io.github.fishstiz.packed_packs.gui.states.PackListKey;
import io.github.fishstiz.packed_packs.gui.states.ActiveAction;
import io.github.fishstiz.packed_packs.gui.actions.intents.PackListIntent;
import io.github.fishstiz.packed_packs.gui.states.PackListComputed;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksContext;
import io.github.fishstiz.packed_packs.pack.PackNode;
import io.github.fishstiz.packed_packs.impl.PackedPacksApiImpl;
import io.github.fishstiz.packed_packs.impl.events.ContextMenuEventImpl;
import io.github.fishstiz.packed_packs.util.Colors;
import io.github.fishstiz.packed_packs.util.PackUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import static io.github.fishstiz.packed_packs.util.PackListComputedUtils.sortByOrderOf;
import static io.github.fishstiz.packed_packs.util.GuiUtils.*;
import static io.github.fishstiz.packed_packs.util.InputUtil.*;

public class PackList extends FZAbstractListWidget<PackList.Entry> implements FocusPathProvider, ContainerEventHandlerPatch {
    private static final int INNER_ITEM_PADDING = 2;
    private static final int INNER_ITEM_HEIGHT = 32;
    private static final int ITEM_HEIGHT = INNER_ITEM_HEIGHT + INNER_ITEM_PADDING * 2;
    private static final int DROP_INDEX_PADDING = 3;
    private static final double SCROLL_RATE = (double) ITEM_HEIGHT / 2;
    private final Map<String, Entry> entries = new Object2ObjectOpenHashMap<>();
    private final PackedPacksContext context;
    private final PackListComputed state;
    private boolean scrolling;
    private boolean initialized;

    public PackList(PackedPacksContext context, PackListComputed state) {
        this.context = context;
        this.state = state;
        setScrollRate(SCROLL_RATE);
    }

    PackListKey key() {
        return state.key();
    }

    boolean module() {
        return state.state().module();
    }

    @Override
    protected int maxContentWidth() {
        return 0;
    }

    @Override
    protected int rowSpacing() {
        return -1;
    }

    void initializeEntries() {
        if (!this.initialized) {
            rebuildEntries();
            this.initialized = true;
        }
    }

    void rebuildEntries() {
        Entry previousFocused = this.getFocused();
        double previousScrollAmount = scrollAmount();

        clearEntries();
        entries.clear();

        state.forEachEntry((entryState, i) -> {
            Entry entry = entryState.pack() instanceof PackNode.Leaf leaf
                    ? new LeafEntry(entryState, leaf.pack(), i)
                    : new Entry(entryState, i);

            addEntry(entry);
            entries.put(entryState.pack().id(), entry);
            if (previousFocused != null && previousFocused.pack.equals(entryState.pack())) {
                setFocused(entry);
            }
        });

        repositionEntries();
        setScrollAmount(previousScrollAmount);
    }

    public void scrollToTop() {
        this.setScrollAmount(0);
    }

    public void scrollToLastSelected() {
        Entry selected = getLastSelected();
        if (selected != null) scrollToEntry(selected);
    }

    public void transferAll() {
        if (!state.isLocked()) {
            List<PackNode> packs = state.state().visiblePacks();
            List<PackNode> payload = new ObjectArrayList<>(packs.size());
            for (PackNode pack : packs) {
                Entry entry = entries.get(pack.id());
                if (entry != null && entry.state.canTransfer()) {
                    payload.add(pack);
                }
            }
            if (!payload.isEmpty()) {
                List<PackNode> orderedPayload = sortByOrderOf(packs, payload).reversed();
                context.dispatch(switch (key().type()) {
                    case AVAILABLE -> new PackListIntent.Enable(key(), orderedPayload.getFirst(), orderedPayload);
                    case ENABLED -> new PackListIntent.Disable(key(), orderedPayload.getFirst(), orderedPayload);
                });
            }
        }
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

    private boolean isMouseOverSelectionEntry(SequencedCollection<PackNode> selection, double mouseX, double mouseY, int index) {
        if (index < 0 || index >= children().size()) return false;
        Entry entry = children().get(index);
        return entry.isMouseOver(mouseX, mouseY) && selection.contains(entry.pack);
    }

    private boolean isMouseOverSelection(SequencedCollection<PackNode> selection, double mouseX, double mouseY, int index) {
        return isMouseOverSelectionEntry(selection, mouseX, mouseY, index - 1) || isMouseOverSelectionEntry(selection, mouseX, mouseY, index);
    }

    private boolean canDrop(ActiveAction.Dragging dragging, int mouseX, int mouseY, int index) {
        if (this.scrolling || (dragging.src().equals(key()) && isMouseOverSelection(dragging.packs(), mouseX, mouseY, index))) {
            return false;
        }
        return state.canDrop(index);
    }

    private void renderDroppableSlots(
            GuiGraphics graphics,
            ActiveAction.Dragging dragging,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        int x = getX();
        int y = getY();
        int width = scrollbarVisible() ? getWidth() - scrollbarWidth() : getWidth();
        int height = getHeight();
        int bottom = getBottom();

        if (isHovered()) {
            double scrollAmount = scrollAmount();

            int scrollDownY = bottom - INNER_ITEM_HEIGHT;
            if (scrollAmount < maxScrollAmount() && mouseY >= scrollDownY) {
                graphics.fillGradient(x, scrollDownY, x + width, scrollDownY + INNER_ITEM_HEIGHT, DROP_ENABLED_TO_COLOR, DROP_ENABLED_FROM_COLOR);
                scroll(scrollRate() * partialTick);
                this.scrolling = true;
            } else if (scrollAmount > 0 && mouseY <= y + INNER_ITEM_HEIGHT) {
                graphics.fillGradient(x, y, x + width, y + INNER_ITEM_HEIGHT, DROP_ENABLED_FROM_COLOR, DROP_ENABLED_TO_COLOR);
                scroll(-(scrollRate() * partialTick));
                this.scrolling = true;
            } else {
                this.scrolling = false;
            }

            int index = getDropIndex(mouseY);
            if (canDrop(dragging, mouseX, mouseY, index)) {
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
                graphics.fill(x, indexY, x + width, indexY + (rowTop - indexY + DROP_INDEX_PADDING), DROP_ENABLED_COLOR);
                graphics.disableScissor();
            }
        }

        graphics.renderOutline(x, y, width, height, DROP_ENABLED_COLOR);
    }

    private void renderDroppableRect(GuiGraphics graphics) {
        if (state.isDropCandidate() && state.canDrop(0)) {
            renderDropToDisabledZone(this, graphics);
        }
    }

    boolean extractDropCandidateRenderState(
            GuiGraphics graphics,
            ActiveAction.Dragging dragging,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if (!state.isLocked() && state.isDropCandidate()) {
            if (state.canReorder()) {
                renderDroppableSlots(graphics, dragging, mouseX, mouseY, partialTick);
            } else {
                renderDroppableRect(graphics);
            }
            return true;
        }
        return false;
    }

    public void onDrop(ActiveAction.Dragging dragging, int mouseX, int mouseY) {
        int index = getDropIndex(mouseY);
        if (canDrop(dragging, mouseX, mouseY, index)) {
            context.dispatch(new PackListIntent.Drop(dragging.src(), key(), index));
        } else {
            context.dispatch(new PackListIntent.Drop(dragging.src(), null, 0));
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

    private @Nullable Entry getLastSelected() {
        PackNode selected = state.getSelected();
        return selected == null ? null : entries.get(selected.id());
    }

    private @Nullable Entry getFocusedOrSelected() {
        Entry focused = getFocused();
        return focused == null ? getLastSelected() : focused;
    }

    private @Nullable Entry getEntry(String packId) {
        return entries.get(packId);
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
            case FocusTarget.LastSelected ignored -> getLastSelected();
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
                    : Objects.requireNonNullElse(getLastSelected(), children().getFirst());
            case FocusNavigationEvent.ArrowNavigation(ScreenDirection direction) -> isFocused()
                    ? getNextEntryAt(direction)
                    : Objects.requireNonNullElse(getLastSelected(), children().getFirst());
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
            if (this.select && !this.child.state.isSelectedLast()) {
                this.child.selectPackExclusively();
            }
        }
    }

    private void selectOnKeyPress(@Nullable Entry entry) {
        if (entry == null) return;
        if (isRangeModifierActive()) {
            context.dispatch(new PackListIntent.SelectRange(key(), entry.pack));
        } else {
            context.dispatch(new PackListIntent.SelectExclusive(key(), entry.pack));
        }
        setFocused(entry);
        scrollToEntry(entry);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isSelectAll(keyCode, modifiers)) {
            context.dispatch(new PackListIntent.SelectAll(key(), state.getSelected()));
            return true;
        }
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (isUp(keyCode) || isDown(keyCode)) {
            Entry nextEntry = getNextEntryAt(isUp(keyCode) ? ScreenDirection.UP : ScreenDirection.DOWN);
            selectOnKeyPress(nextEntry);
            return nextEntry != null;
        }
        if (isHome(keyCode) || isEnd(keyCode)) {
            if (children().isEmpty()) return true;
            selectOnKeyPress(isHome(keyCode) ? children().getFirst() : children().getLast());
            return true;
        }
        if (isPageUp(keyCode) || isPageDown(keyCode)) {
            if (children().isEmpty()) return true;
            int pageSize = Math.max(1, getHeight() / ITEM_HEIGHT);
            Entry selected = getFocusedOrSelected();
            int currentIndex = selected != null ? children().indexOf(selected) : 0;
            int targetIndex = isPageUp(keyCode)
                    ? Math.max(0, currentIndex - pageSize)
                    : Math.min(children().size() - 1, currentIndex + pageSize);
            Entry entry = children().get(targetIndex);
            selectOnKeyPress(entry);
            return true;
        }
        return false;
    }

    @Override
    protected void extractEntriesRenderState(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!state.isFolderOpened()) {
            super.extractEntriesRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }

    class Entry extends FZAbstractListWidget.Entry<Entry> implements SelectableEntry, ContainerEventHandlerPatch, FZContextMenu.Source, FZComponent, ElementSink {
        private static final int ICON_SIZE = 32;
        private static final Tooltip FOLDER_OPEN_INFO = Tooltip.create(FOLDER_OPEN_TEXT);
        private static final RenderableRectangle SELECTED_OVERLAY = Renderables.fill(Colors.alpha(Colors.BLUE_500, 0.25f));
        private static final WidgetRenderables SELECT_SPRITES = new WidgetRenderables(
                Renderables.sprite(ResourceLocation.withDefaultNamespace("transferable_list/select")),
                Renderables.sprite(ResourceLocation.withDefaultNamespace("transferable_list/select_highlighted"))
        );
        private static final WidgetRenderables UNSELECT_SPRITES = new WidgetRenderables(
                Renderables.sprite(ResourceLocation.withDefaultNamespace("transferable_list/unselect")),
                Renderables.sprite(ResourceLocation.withDefaultNamespace("transferable_list/unselect_highlighted"))
        );
        private static final WidgetRenderables MOVE_UP_SPRITES = new WidgetRenderables(
                Renderables.sprite(ResourceLocation.withDefaultNamespace("transferable_list/move_up")),
                Renderables.sprite(ResourceLocation.withDefaultNamespace("transferable_list/move_up_highlighted"))
        );
        private static final WidgetRenderables MOVE_DOWN_SPRITES = new WidgetRenderables(
                Renderables.sprite(ResourceLocation.withDefaultNamespace("transferable_list/move_down")),
                Renderables.sprite(ResourceLocation.withDefaultNamespace("transferable_list/move_down_highlighted"))
        );
        private final int index;
        protected final PackNode pack;
        protected final PackListComputed.Entry state;
        private final MouseStateHandler mouseStateHandler;
        private final List<GuiEventListener> children = new ObjectArrayList<>();
        private final List<Renderable> renderables = new ObjectArrayList<>();
        private List<LayoutElement> rightElements = Collections.emptyList();
        protected @Nullable PackWidget packWidget;
        private @Nullable AbstractWidget folderWidget;
        private @Nullable PackDevMenu devMenu;
        private boolean initialized;

        Entry(PackListComputed.Entry state, int index) {
            super(ITEM_HEIGHT);
            this.index = index;
            this.pack = state.pack();
            this.state = state;
            this.mouseStateHandler = new MouseStateHandler(this);
        }

        @Override
        public int getIndex() {
            return index;
        }

        protected PackListKey key() {
            return PackList.this.state.key();
        }

        protected boolean moduleParent() {
            return module();
        }

        protected boolean isIncompatibleWarningsHidden() {
            return context.configs().user().isIncompatibleWarningsHidden();
        }

        protected boolean isFileModifiable() {
            return context.resources().isModifiable(PackList.this.state.profiles(), pack);
        }

        protected ResourceLocation getPackIcon() {
            return context.iconCache().get(pack);
        }

        protected void buildWidgets() {
            this.packWidget = new PackWidget(this, INNER_ITEM_HEIGHT);
            repositionPackWidget();

            if (pack instanceof PackNode.Parent) {
                PreferenceHelper.wrap(Preferences.FOLDER_PACK_WIDGET, FZIconButton.builder()
                                .size(INNER_ITEM_HEIGHT / 3, INNER_ITEM_HEIGHT / 3)
                                .icon(new WidgetElements(HAMBURGER_RECT, 8, 8))
                                .tooltip(FOLDER_OPEN_INFO)
                                .focusOnInteraction(false)
                                .onPress(this::expandFolder)
                                .build())
                        .ifPresent(widget -> {
                            int x = getX() + PackWidget.ICON_SIZE + INNER_ITEM_PADDING * 2;
                            int y = getY() + getHeight() - widget.getHeight() - INNER_ITEM_PADDING;
                            widget.setPosition(x, y);
                            acceptWidget(widget);
                            acceptRenderable(widget);
                            this.folderWidget = widget;
                        });
            }

            if (context.devMode()) {
                this.devMenu = new PackDevMenu(context, PackList.this.state.profiles(), this);
            }
        }

        private void init() {
            if (!this.initialized) {
                this.initialized = true;
                buildWidgets();
            }
        }
        // todo add widgets to navigation path
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

        public void selectPack() {
            if (PackList.this.state.isLocked()) return;
            context.dispatch(new PackListIntent.Select(key(), pack));
        }

        public void selectToggle() {
            if (PackList.this.state.isLocked()) return;
            context.dispatch(new PackListIntent.SelectToggle(key(), pack));
        }

        public void selectTowardsPack() {
            if (PackList.this.state.isLocked()) return;
            context.dispatch(new PackListIntent.SelectRange(key(), pack));
        }

        public void selectPackExclusively() {
            if (PackList.this.state.isLocked()) return;
            context.dispatch(new PackListIntent.SelectExclusive(key(), pack));
        }

        protected List<PackNode> createPayload(BooleanSupplier filter) {
            SequencedCollection<PackNode> selection = PackList.this.state.state().selectedPacks();
            if (!selection.contains(pack)) {
                return filter.getAsBoolean() ? List.of(pack) : Collections.emptyList();
            }

            return CollectionUtils.addIf(new ObjectArrayList<>(selection.size()), selection, ignored -> filter.getAsBoolean());
        }

        protected List<PackNode> createPayload() {
            return state.isSelected() ? List.copyOf(PackList.this.state.state().selectedPacks()) : List.of(pack);
        }

        protected void transferPack() {
            if (!PackList.this.state.isLocked()) {
                List<PackNode> payload = createPayload(state::canTransfer);
                if (!payload.isEmpty()) {
                    List<PackNode> orderedPayload = sortByOrderOf(PackList.this.state.state().visiblePacks(), payload).reversed();
                    context.dispatch(key().type().available()
                            ? new PackListIntent.Enable(key(), pack, orderedPayload)
                            : new PackListIntent.Disable(key(), pack, orderedPayload));
                }
            }
        }

        protected void movePack(boolean upwards) {
            if (!PackList.this.state.isLocked()) {
                List<PackNode> payload = createPayload();
                if (!payload.isEmpty()) {
                    context.dispatch(new PackListIntent.MoveOnce(key(), pack, payload, upwards));
                }
            }
        }

        protected void openRenameModal() {
            context.dispatch(new PackListIntent.OpenRenameModal(key(), pack));
        }

        protected void deletePack() {
            context.dispatch(new PackListIntent.Delete(key(), pack));
        }

        protected void expandFolder() {
            if (pack instanceof PackNode.Parent parent) {
                context.dispatch(new PackListIntent.OpenFolder(key(), parent));
            }
        }

        protected void dragPack() {
            if (!state.canDrag() || PackList.this.state.isLocked()) return;
            List<PackNode> payload = createPayload();
            if (!payload.isEmpty()) {
                List<PackNode> orderedPayload = sortByOrderOf(PackList.this.state.state().visiblePacks(), payload);
                context.dispatch(new PackListIntent.Drag(key(), pack, new ObjectLinkedOpenHashSet<>(orderedPayload)));
            }
        }

        @Override
        public void setFocused(boolean focused) {
            super.setFocused(focused);
            if (!focused && getFocused() != null) {
                getFocused().setFocused(false);
            }
        }

        private boolean isFocusedOrSelected() {
            return PackList.this.getFocused() == null ? state.isSelectedLast() : isFocused();
        }

        @Override
        public boolean fidgetz$shouldTakeFocusAfterInteraction() {
            return !PackList.this.state.isFolderOpened();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (super.mouseClicked(mouseX, mouseY, button) || isRightClick(button)) {
                if (!PackList.this.state.isFolderOpened()) return true;
                // ideally folderWidget should return false on #shouldTakeFocusAfterInteraction,
                // #shouldTakeFocusAfterInteraction also does not bubble up,
                // we want to return false regardless so that the folder can be focused
                this.setFocused(null);
                return false;
            }

            if (isLeftClick(button) && fidgetz$getHovered() == null) {
                int relativeX = (int) mouseX - (getX() + INNER_ITEM_PADDING);
                int relativeY = (int) mouseY - (getY() + INNER_ITEM_PADDING);

                if (state.canEnable() && mouseOverIcon(relativeX, relativeY, ICON_SIZE)) {
                    transferPack();
                    return false;
                }
                if (state.canDisable() && mouseOverLeftHalf(relativeX, relativeY, ICON_SIZE)) {
                    transferPack();
                    return false;
                }
                if (state.canMoveUp() && mouseOverTopRightQuarter(relativeX, relativeY, ICON_SIZE)) {
                    movePack(true);
                    return false;
                }
                if (state.canMoveDown() && mouseOverBottomRightQuarter(relativeX, relativeY, ICON_SIZE)) {
                    movePack(false);
                    return false;
                }
            }

            return mouseStateHandler.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return mouseStateHandler.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            return mouseStateHandler.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (super.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            if (isExpandFolder(keyCode, modifiers) && pack instanceof PackNode.Parent && state.isSelectedExclusively()) {
                expandFolder();
                return true;
            }
            if (isTransfer(keyCode, modifiers) && state.canTransfer()) {
                transferPack();
                return true;
            }
            if (isMoveDown(keyCode, modifiers) && PackList.this.state.canReorder()) {
                movePack(false);
                return true;
            }
            if (isMoveUp(keyCode, modifiers) && PackList.this.state.canReorder()) {
                movePack(true);
                return true;
            }
            if (isOpenFile(keyCode, modifiers)) {
                PackUtil.openPack(pack.path());
                return true;
            }
            if (isOpenFolder(keyCode, modifiers)) {
                PackUtil.openParent(pack.path());
                return true;
            }
            if (isDelete(keyCode, modifiers) && context.resources().isModifiable(PackList.this.state.profiles(), pack)) {
                deletePack();
                return true;
            }
            if (isRename(keyCode, modifiers) && context.resources().isModifiable(PackList.this.state.profiles(), pack)) {
                openRenameModal();
                return true;
            }
            return false;
        }

        public void renderBack(GuiGraphics graphics, int top, int left, int width, int height) {
            if (!pack.compatibility().isCompatible() && !isIncompatibleWarningsHidden()) {
                int margin = INNER_ITEM_PADDING / 2;
                int backgroundLeft = left + margin;
                int backgroundTop = top + margin;
                int backgroundRight = (left + width) - margin;
                int backgroundBottom = (top + height) - margin;
                graphics.fill(backgroundLeft, backgroundTop, backgroundRight, backgroundBottom, Colors.RED_900);
            }
        }

        private void renderWidgetSprites(GuiGraphics graphics, WidgetRenderables sprites, int left, int top, boolean hovered) {
            RenderableRectangle sprite = hovered ? sprites.enabledFocused() : sprites.enabled();
            sprite.extractRenderState(graphics, left, top, ICON_SIZE, ICON_SIZE, 0, 0, 0);
            if (hovered) {
                MinecraftCursor.get().setPointingHand();
            }
        }

        private void renderWidgetSprites(GuiGraphics graphics, int top, int left, int mouseX, int mouseY) {
            boolean hovered = isHovered() && fidgetz$getHovered() == null;
            if ((!hovered && !state.isSelectedLast()) || PackList.this.state.isDragging()) return;

            int relX = mouseX - left;
            int relY = mouseY - top;

            WHITE_OVERLAY.extractRenderState(graphics, left, top, ICON_SIZE, ICON_SIZE, 0, 0, 0);

            if (state.canEnable()) {
                renderWidgetSprites(graphics, SELECT_SPRITES, left, top, hovered && mouseOverIcon(relX, relY, ICON_SIZE));
            }
            if (state.canDisable()) {
                renderWidgetSprites(graphics, UNSELECT_SPRITES, left, top, hovered && mouseOverLeftHalf(relX, relY, ICON_SIZE));
            }
            if (state.canMoveUp()) {
                renderWidgetSprites(graphics, MOVE_UP_SPRITES, left, top, hovered && mouseOverTopRightQuarter(relX, relY, ICON_SIZE));
            }
            if (state.canMoveDown()) {
                renderWidgetSprites(graphics, MOVE_DOWN_SPRITES, left, top, hovered && mouseOverBottomRightQuarter(relX, relY, ICON_SIZE));
            }
        }

        private void renderSelection(GuiGraphics graphics, int top, int left, int width, int height) {
            if (state.isSelected()) {
                RenderableRectangle overlay = state.isSelectedLast() ? WHITE_OVERLAY : SELECTED_OVERLAY;
                overlay.extractRenderState(graphics, left, top, width, height, 0, 0, 0);
                graphics.renderOutline(left, top, width, height, Colors.BLUE_500);
            }
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            init();
            int left = getX();
            int top = getY();
            int width = getWidth();
            int height = getHeight();
            int innerTop = top + INNER_ITEM_PADDING;
            int innerLeft = left + INNER_ITEM_PADDING;

            renderBack(graphics, top, left, width, height);

            Objects.requireNonNull(packWidget).checkCompatibility(isHovered() || isFocusedOrSelected());
            packWidget.render(graphics, mouseX, mouseY, partialTick);

            renderSelection(graphics, top, left, width, height);
            renderWidgetSprites(graphics, innerTop, innerLeft, mouseX, mouseY);

            if (!renderables.isEmpty()) {
                graphics.pose().pushPose();
                graphics.pose().translate(0f, 0f, 0.1f);
                for (Renderable renderable : this.renderables) {
                    renderable.render(graphics, mouseX, mouseY, partialTick);
                }
                graphics.pose().popPose();
            }

            if (devMenu != null) {
                devMenu.render(graphics, innerTop, left, width);
            }
        }

        protected void updateContextEntries(
                Function<ContextMenuEvent.PackEntry.Pos, Collection<FZPopoverMenuItem>> entryFactory,
                FZContextMenu.Collector collector,
                double x,
                double y
        ) {
            entryFactory.apply(ContextMenuEvent.PackEntry.Pos.BEFORE_HEADER).forEach(collector::addEntry);

            collector.addEntry(builder -> builder
                    .message(pack.title())
                    .icon(padded16Rect(Renderables.texture(getPackIcon(), 32, 32)))
                    .background(Renderables.fill(Colors.GRAY_500))
                    .onPress(e -> {
                        e.context().closeMenu();
                        return false;
                    })
                    .allowAutoDivideAfterEntry(false));

            collector.nextSection();

            entryFactory.apply(ContextMenuEvent.PackEntry.Pos.AFTER_HEADER).forEach(collector::addEntry);

            if (devMenu != null) {
                devMenu.updateContextEntries(collector);
                collector.nextSection();
            }

            entryFactory.apply(ContextMenuEvent.PackEntry.Pos.AFTER_DEV).forEach(collector::addEntry);

            if (pack instanceof PackNode.Parent) {
                collector.addEntry(builder -> builder
                        .message(FOLDER_OPEN_TEXT)
                        .onPress(this::expandFolder));
                collector.nextSection();
            }

            if (isFileModifiable()) {
                collector.addEntry(builder -> builder
                        .message(RENAME_FILE_TEXT)
                        .active(this::isFileModifiable)
                        .onPress(this::openRenameModal));
                collector.addEntry(builder -> builder
                        .message(DELETE_FILE_TEXT)
                        .active(this::isFileModifiable)
                        .onPress(this::deletePack));
            }

            if (pack.path() != null) {
                collector.addEntry(builder -> builder.message(OPEN_FILE_TEXT).onPress(() -> PackUtil.openPack(pack.path())));
                collector.addEntry(builder -> builder.message(OPEN_PARENT_TEXT).onPress(() -> PackUtil.openParent(pack.path())));
            }

            entryFactory.apply(ContextMenuEvent.PackEntry.Pos.AFTER_PACK).forEach(collector::addEntry);

            FZContextMenu.Source.super.fidgetz$updateContextEntries(x, y, collector);
        }

        protected void updateContextEntries(double x, double y, FZContextMenu.Collector collector) {
            updateContextEntries(ignored -> Collections.emptyList(), collector, x, y);
        }

        @Override
        public final void fidgetz$updateContextEntries(double x, double y, FZContextMenu.Collector collector) {
            if (this.initialized) {
                updateContextEntries(x, y, collector);
            }
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

    class LeafEntry extends Entry implements PackContext {
        private final Pack exposed;

        LeafEntry(PackListComputed.Entry state, Pack pack, int index) {
            super(state, index);
            this.exposed = pack;
        }

        @Override
        public Pack pack() {
            return exposed;
        }

        @Override
        public ResourceLocation icon() {
            return getPackIcon();
        }

        @Override
        public boolean fileModifiable() {
            return isFileModifiable();
        }

        @Override
        protected void buildWidgets() {
            super.buildWidgets();

            PackedPacksApiImpl.getInstance().eventBus().post(new InitializePackEntryEvent(
                    context,
                    this,
                    Objects.requireNonNull(packWidget),
                    this
            ));
        }

        @Override
        public void updateContextEntries(double x, double y, FZContextMenu.Collector collector) {
            var extensions = ContextMenuEventImpl.postPackEntry(context, this);
            updateContextEntries(extensions::entries, collector, x, y);
        }

        // PackSelectionModel.Entry overrides

        @Override
        @Deprecated
        public ResourceLocation getIconTexture() {
            return getPackIcon();
        }

        @Override
        @Deprecated
        public PackCompatibility getCompatibility() {
            return pack.compatibility();
        }

        @Override
        @Deprecated
        public String getId() {
            return pack.id();
        }

        @Override
        @Deprecated
        public Component getTitle() {
            return pack.title();
        }

        @Override
        @Deprecated
        public Component getDescription() {
            return pack.metadata().description();
        }

        @Override
        @Deprecated
        public PackSource getPackSource() {
            return pack.packSource();
        }

        @Override
        @Deprecated
        public boolean isFixedPosition() {
            return PackList.this.state.profiles().isPackFixed(pack);
        }

        @Override
        @Deprecated
        public boolean isRequired() {
            return PackList.this.state.profiles().isPackRequired(pack);
        }

        @Override
        @Deprecated
        public void select() {
            if (state.canEnable()) {
                transferPack();
            }
        }

        @Override
        @Deprecated
        public void unselect() {
            if (state.canDisable()) {
                transferPack();
            }
        }

        @Override
        @Deprecated
        public void moveUp() {
            movePack(true);
        }

        @Override
        @Deprecated
        public void moveDown() {
            movePack(false);
        }

        @Override
        @Deprecated
        public boolean isSelected() {
            return key().type().enabled();
        }

        @Override
        @Deprecated
        public boolean canMoveUp() {
            return state.canMoveUp();
        }

        @Override
        @Deprecated
        public boolean canMoveDown() {
            return state.canMoveDown();
        }
    }
}
