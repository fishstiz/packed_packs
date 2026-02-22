package io.github.fishstiz.packed_packs.gui.components.pack;

import io.github.fishstiz.fidgetz.gui.components.*;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuContainer;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.layouts.*;
import io.github.fishstiz.fidgetz.gui.renderables.ColoredRect;
import io.github.fishstiz.fidgetz.util.DrawUtil;
import io.github.fishstiz.fidgetz.util.GuiUtil;
import io.github.fishstiz.packed_packs.api.context.PackContext;
import io.github.fishstiz.packed_packs.api.events.ContextMenuEvent;
import io.github.fishstiz.packed_packs.api.events.InitializePackEntryEvent;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.config.Preferences;
import io.github.fishstiz.packed_packs.gui.components.MouseSelectionHandler;
import io.github.fishstiz.packed_packs.gui.components.SelectionContext;
import io.github.fishstiz.packed_packs.gui.components.contextmenu.PackMenuHeader;
import io.github.fishstiz.packed_packs.gui.components.events.PackListEventListener;
import io.github.fishstiz.packed_packs.gui.history.Restorable;
import io.github.fishstiz.packed_packs.gui.components.ToggleableHelper;
import io.github.fishstiz.packed_packs.impl.PackedPacksApiImpl;
import io.github.fishstiz.packed_packs.impl.context.PackContextImpl;
import io.github.fishstiz.packed_packs.impl.events.ContextMenuEventImpl;
import io.github.fishstiz.packed_packs.pack.PackAssetManager;
import io.github.fishstiz.packed_packs.pack.PackFileOperations;
import io.github.fishstiz.packed_packs.pack.PackOptionsContext;
import io.github.fishstiz.packed_packs.transform.interfaces.FilePack;
import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.packed_packs.util.ToastUtil;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import io.github.fishstiz.packed_packs.gui.components.events.*;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

import static io.github.fishstiz.fidgetz.util.GuiUtil.playClickSound;
import static io.github.fishstiz.packed_packs.util.InputUtil.*;
import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.*;
import static io.github.fishstiz.fidgetz.util.lang.ObjectsUtil.*;

public abstract class PackList extends AbstractFixedListWidget<PackList.Entry> implements
        Restorable<PackList.Snapshot>,
        ContainerEventHandlerPatch,
        ContextMenuContainer {
    protected static final int Y_OFFSET = 1;
    protected static final int ITEM_HEIGHT = 35;
    protected static final int ROW_GAP = 3;
    protected final PackOptionsContext options;
    protected final PackAssetManager assets;
    protected final PackListModel list;
    private final PackFileOperations fileOps;
    private final PackListEventListener listener;

    protected PackList(PackOptionsContext options, PackAssetManager assets, PackFileOperations fileOps, PackListEventListener listener) {
        super(ITEM_HEIGHT);
        this.assets = assets;
        this.options = options;
        this.fileOps = fileOps;
        this.listener = listener;
        this.list = new PackListModel(this.options);
    }

    protected abstract @NonNull Entry createEntry(PackContext context, SelectionContext<Pack> selectionContext, int index);

    public @Nullable Entry getEntry(@Nullable Pack pack) {
        if (pack == null) return null;
        for (Entry entry : this.children()) {
            if (Objects.equals(entry.pack(), pack)) return entry;
        }
        return null;
    }

    private void refreshEntries() {
        Entry focused = this.getFocused();
        List<Pack> selection = this.list.getSelection();
        List<Pack> visiblePacks = this.list.getVisibleItems();

        this.clearEntries();
        for (int i = 0; i < visiblePacks.size(); i++) {
            Pack pack = visiblePacks.get(i);
            PackContext context = new PackContextImpl(pack, this.assets, this.fileOps);
            Entry entry = this.createEntry(context, new SelectionContext<>(selection, pack), i);
            InitializePackEntryEvent event = new InitializePackEntryEvent(listener.ctx(), context, entry, entry::addTopLayer);
            PackedPacksApiImpl.getInstance().eventBus().post(event);

            this.addEntry(entry);
        }

        this.clampScrollAmount();
        this.setFocused(mapOrNull(focused, f -> this.getEntry(f.pack())));
    }

    protected void refreshList() {
        this.list.refresh();
        this.refreshEntries();
    }

    public void scrollToTop() {
        this.setScrollAmount(0);
    }

    public void reload(Collection<Pack> packs) {
        this.list.replaceAll(packs);
        this.setFocused(null);
        this.refresh();
    }

    public @NonNull List<Pack> copyPacks() {
        return List.copyOf(this.list.getItems());
    }

    public List<Pack> getOrderedSelection() {
        return this.list.getOrderedSelection();
    }

    public void clearSelection() {
        this.list.clearSelection();
    }

    private void refresh() {
        this.clearSelection();
        this.refreshList();
        this.scrollToTop();
    }

    public void sort(Query.SortOption sort) {
        if (this.list.sort(sort)) {
            this.refresh();
        }
    }

    public void hideIncompatible(boolean hideIncompatible) {
        if (this.list.hideIncompatible(hideIncompatible)) {
            this.clearSelection();
            this.refreshList();
        }
    }

    public void search(@NonNull String search) {
        if (this.list.search(search)) {
            this.refresh();
        }
    }

    public boolean isQueried() {
        return this.list.isQueried();
    }

    public void addAll(List<Pack> packs) {
        for (Pack pack : packs) {
            this.list.add(pack);
        }
        this.refreshList();
    }

    public void addOrMove(Pack pack, int to) {
        this.list.insertOrMove(to, pack);
        this.list.select(pack);
    }

    public boolean moveAll(List<Pack> selection, int to) {
        if (this.list.moveAll(to, selection)) {
            this.refreshList();
            return true;
        }
        return true;
    }

    private boolean removePack(Pack pack) {
        Entry focused = this.getFocused();
        if (this.list.remove(pack)) {
            if (focused != null && focused.pack().getId().equals(pack.getId())) {
                this.setFocused(null);
            }
            return true;
        }
        return false;
    }

    public void remove(Pack pack) {
        this.removePack(pack);
        this.refreshList();
    }

    public void removeAll(List<Pack> packs) {
        boolean removed = false;
        for (Pack pack : packs) {
            removed |= this.removePack(pack);
        }
        if (removed) {
            this.refreshList();
        }
    }

    public @Nullable Pack getLastSelected() {
        return this.list.getLastSelected();
    }

    @Override
    public @Nullable Entry getSelected() {
        return this.getLastSelected() != null ? this.getEntry(this.getLastSelected()) : super.getSelected();
    }

    @Override
    public void setSelected(@Nullable Entry selected) {
        this.selected = selected;
    }

    public boolean isSelected(Pack pack) {
        return this.list.isSelected(pack);
    }

    public void scrollToLastSelected() {
        ifPresent(this.getEntry(this.getLastSelected()), this::scrollToEntry);
    }

    public void unselect(Pack pack) {
        this.list.unselect(pack);

        Entry entry = this.getEntry(pack);
        if (entry == this.getFocused()) {
            this.setFocused(null);
        }
        if (entry == this.getSelected()) {
            this.setSelected(null);
        }
    }

    public void select(Pack pack) {
        if (this.list.select(pack)) {
            Entry entry = this.getEntry(pack);
            this.setFocused(entry);
            this.setSelected(entry);
        }
    }

    public void selectAll() {
        this.list.getVisibleItems().forEach(this::select);
    }

    public void selectAll(List<Pack> packs) {
        packs.forEach(this::select);
    }

    public void selectExclusive(Pack pack) {
        this.clearSelection();
        this.select(pack);
    }

    public void selectToggle(Pack pack) {
        if (this.isSelected(pack)) {
            this.unselect(pack);
        } else {
            this.select(pack);
        }
    }

    public void selectRange(Pack pack) {
        this.list.selectRange(pack);
        this.select(pack);
    }

    public boolean isTransferable(Pack pack) {
        return testNullable(this.getEntry(pack), PackList.Entry::isTransferable);
    }

    public void transferAll() {
        List<Pack> payload = new ArrayList<>();
        List<Pack> visiblePacks = this.list.getVisibleItems();
        for (int i = visiblePacks.size() - 1; i >= 0; i--) {
            Pack pack = visiblePacks.get(i);
            if (this.isTransferable(pack)) {
                payload.add(pack);
            }
        }
        if (!payload.isEmpty()) {
            this.sendEvent(new RequestTransferEvent(this, this.getLastSelected(), payload));
        }
    }

    protected void sendEvent(PackListEvent event) {
        this.listener.onEvent(event);
    }

    public abstract boolean canInteract(PackList source);

    protected abstract boolean canDrop(DragEvent dragEvent, double mouseX, double mouseY);

    protected abstract List<Pack> handleDrop(DragEvent dragEvent, double mouseX, double mouseY);

    public abstract void renderDroppableZone(GuiGraphics guiGraphics, DragEvent dragEvent, int mouseX, int mouseY, float partialTick);

    public final void drop(DragEvent dragEvent, double mouseX, double mouseY) {
        if (this.options.isLocked()) return;

        List<Pack> dropped = this.handleDrop(dragEvent, mouseX, mouseY);
        if (!dropped.isEmpty()) {
            if (dragEvent.target() != this) {
                this.sendEvent(new DropEvent(dragEvent.target(), this, dropped));
            } else {
                this.sendEvent(new MoveEvent(this, dragEvent.trigger(), dropped));
            }
        }
    }

    protected void openFolder(FolderPack folderPack) {
        this.sendEvent(new FolderOpenEvent(this, folderPack));
    }

    private @Nullable ComponentPath handleArrowNavigation(FocusNavigationEvent.ArrowNavigation arrowNavigation) {
        Entry entry = switch (arrowNavigation.direction()) {
            case UP -> this.getPreviousEntry();
            case DOWN -> this.getNextEntry();
            default -> null;
        };
        if (entry != null) {
            if (isRangeModifierActive()) {
                this.selectRange(entry.pack());
            } else {
                this.selectExclusive(entry.pack());
            }
            this.sendEvent(new SelectionEvent(this));
            this.scrollToEntry(entry);
            return ComponentPath.path(entry, this);
        }
        this.setFocused(null);
        return null;
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
        if (!this.isFocused()) {
            Pack lastSelected = this.getLastSelected();
            Entry entry = null;
            if (lastSelected != null) {
                entry = this.getEntry(lastSelected);
            } else if (!this.children().isEmpty()) {
                entry = this.children().getFirst();
            }
            if (entry != null) {
                this.select(entry.pack());
                this.scrollToEntry(entry);
                return ComponentPath.path(entry, this);
            }
        } else if (event instanceof FocusNavigationEvent.ArrowNavigation arrowNavigation) {
            return this.handleArrowNavigation(arrowNavigation);
        } else {
            this.setFocused(null);
        }
        return null;
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        Entry entry = this.getEntry(this.getLastSelected());
        if (isExpandFolder(keyEvent) && entry != null && entry.folderWidget != null && this.list.getSelection().size() == 1) {
            this.openFolder(entry.folderWidget.getMetadata());
            return true;
        }
        if (isTransfer(keyEvent)) {
            if (entry != null && entry.transfer()) {
                playClickSound();
            }
            return entry != null;
        }
        return super.keyPressed(keyEvent);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
        boolean scrolling = this.updateScrolling(mouseButtonEvent);
        return this.isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y()) &&
               ContainerEventHandlerPatch.super.mouseClickedAt(mouseButtonEvent, doubleClicked) ||
               scrolling;
    }

    @Override
    protected void renderListItems(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderListItems(guiGraphics, mouseX, mouseY, partialTick);

        Entry focused = this.getFocused();
        if (focused != null && focused.isFocused() && this.children().contains(focused)) {
            int outlineTop = focused.getY();
            int outlineHeight = focused.getHeight() + Y_OFFSET;
            DrawUtil.renderOutline(guiGraphics, focused.getX(), outlineTop, focused.getWidth(), outlineHeight, Theme.WHITE.getARGB());
        }
    }

    @Override
    public int maxScrollAmount() {
        int maxScrollAmount = super.maxScrollAmount();
        return maxScrollAmount > 0 ? maxScrollAmount + Y_OFFSET : maxScrollAmount;
    }

    public @NonNull Snapshot captureState(String eventName) {
        return new Snapshot(this);
    }

    public void replaceState(@NonNull Snapshot snapshot) {
        snapshot.model.restore();
        this.refreshEntries();
        this.setFocused(this.getEntry(snapshot.focused));
        this.setSelected(this.getEntry(snapshot.selected));
    }

    public record Snapshot(
            PackList target,
            @Nullable Pack focused,
            @Nullable Pack selected,
            PackListModel.Snapshot model
    ) implements Restorable.Snapshot<Snapshot> {
        public Snapshot(PackList target, PackListModel.Snapshot model) {
            this(target, extractPack(target.getFocused()), extractPack(target.getSelected()), model);
        }

        public Snapshot(PackList target) {
            this(target, target.list.captureState());
        }

        public Snapshot replaceAll(List<Pack> packs) {
            return new Snapshot(this.target, this.focused, this.selected, this.model.replaceAll(packs));
        }

        public Snapshot retainAll(Set<Pack> packs) {
            return new Snapshot(this.target, this.focused, this.selected, this.model.retainAll(packs));
        }
    }

    private static @Nullable Pack extractPack(@Nullable Entry entry) {
        return mapOrNull(entry, Entry::pack);
    }

    public abstract class Entry extends AbstractFixedListWidget<Entry>.Entry implements ContainerEventHandlerPatch, ContextMenuContainer {
        private static final int V_MARGIN = ROW_GAP / 2 + Y_OFFSET;
        private static final int BACKGROUND_MARGIN = 1;
        private static final Tooltip FOLDER_OPEN_INFO = Tooltip.create(FolderPack.FOLDER_OPEN_TEXT);
        protected static final int H_SPACING = 2;
        protected static final ColoredRect SELECTED_OVERLAY = new ColoredRect(Theme.BLUE_500.withAlpha(0.25F));
        protected final PackContext context;
        protected final SelectionContext<Pack> selectionContext;
        private final MouseSelectionHandler<Pack> selectionHandler;
        private final List<GuiEventListener> children = new ObjectArrayList<>();
        private final List<Renderable> renderables = new ObjectArrayList<>();
        private final List<Renderable> topRenderables = new ObjectArrayList<>();
        private final PackWidget packWidget;
        private final @Nullable PackListDevMenu devMenu;
        private FidgetzButton<FolderPack> folderWidget;
        private boolean stale = false;

        protected Entry(PackContext context, SelectionContext<Pack> selectionContext, int index) {
            super(index);
            this.context = context;
            this.selectionContext = selectionContext;
            this.selectionHandler = new MouseSelectionHandler<>(this, selectionContext);
            this.packWidget = this.addRenderableWidget(new PackWidget(
                    this.pack(),
                    PackList.this.assets,
                    this.getX(),
                    PackList.this.getRowTop(this.index),
                    this.getWidth(),
                    ITEM_HEIGHT - ROW_GAP,
                    H_SPACING
            ));
            boolean devMode = Config.get().isDevMode();
            if (this.pack() instanceof FolderPack folderPack && (devMode || Preferences.INSTANCE.folderPackWidget.get())) {
                this.folderWidget = this.addTopRenderableOnly(this.prependWidget(
                        ToggleableHelper.applyPref(Preferences.INSTANCE.folderPackWidget, FidgetzButton.<FolderPack>builder())
                                .setTooltip(FOLDER_OPEN_INFO)
                                .setHeight(this.packWidget.getHeight() / 3)
                                .makeSquare()
                                .setSprite(GuiConstants.HAMBURGER_SPRITE)
                                .setFocusOnInteract(false)
                                .setMetadata(folderPack)
                                .setOnPress(this::openFolder)
                                .build()
                ));
            }
            this.devMenu = devMode
                    ? new PackListDevMenu(PackList.this.minecraft, PackList.this.options, this.selectionContext, this::handleDevMenuEvent)
                    : null;
        }

        public Pack pack() {
            return this.context.pack();
        }

        public <U extends GuiEventListener & Renderable> U addRenderableWidget(U widget) {
            this.children.add(widget);
            this.renderables.add(widget);
            return widget;
        }

        public <U extends GuiEventListener> U prependWidget(U widget) {
            this.children.addFirst(widget);
            return widget;
        }

        public <U extends Renderable> U addTopRenderableOnly(U renderable) {
            this.topRenderables.add(renderable);
            return renderable;
        }

        public <U extends GuiEventListener & Renderable> void addTopLayer(U widget) {
            this.addTopRenderableOnly(this.prependWidget(widget));
        }

        public boolean isTransferable() {
            return !PackList.this.options.isLocked() && !this.isStale();
        }

        public boolean isSelected() {
            return this.selectionContext.isSelected();
        }

        public boolean isSelectedLast() {
            return this.selectionContext.isSelectedLast();
        }

        protected void sendPacks(Pack trigger, List<Pack> payload) {
            PackList.this.sendEvent(new RequestTransferEvent(PackList.this, trigger, payload));
        }

        private boolean sendSelection() {
            List<Pack> payload = new ObjectArrayList<>();

            for (Pack selected : PackList.this.getOrderedSelection().reversed()) {
                if (PackList.this.isTransferable(selected)) {
                    payload.add(selected);
                }
            }

            if (!payload.isEmpty()) {
                Pack trigger = this.isTransferable() ? this.pack() : null;
                this.sendPacks(trigger, payload);
                return true;
            }

            return false;
        }

        public boolean transfer() {
            if (!this.isSelected() && this.isTransferable()) {
                PackList.this.sendEvent(new RequestTransferEvent(PackList.this, this.pack()));
                return true;
            }

            return this.sendSelection();
        }

        protected boolean handleMouseAction(MouseSelectionHandler.Action action) {
            if (!action.shouldDispatch() || this.isStale()) return false;

            switch (action) {
                case SELECT -> PackList.this.select(this.pack());
                case SELECT_TOGGLE -> PackList.this.selectToggle(this.pack());
                case SELECT_EXCLUSIVE -> PackList.this.selectExclusive(this.pack());
                case SELECT_RANGE -> PackList.this.selectRange(this.pack());
                case TRANSFER -> {
                    if (this.isTransferable()) {
                        PackList.this.sendEvent(new RequestTransferEvent(PackList.this, this.pack()));
                        return false;
                    }
                }
                case DRAG ->
                        PackList.this.sendEvent(new DragEvent(PackList.this, PackList.this.getOrderedSelection().reversed(), this.pack()));
            }

            if (action.shouldSelect()) {
                PackList.this.sendEvent(new SelectionEvent(PackList.this));
            }

            return true;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return PackList.this.beforeScrollbarX(mouseX) && super.isMouseOver(mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
            if (ContainerEventHandlerPatch.super.mouseClicked(mouseButtonEvent, doubleClicked)) {
                return false;
            }
            return this.handleMouseAction(this.selectionHandler.mouseClicked(mouseButtonEvent));
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
            return this.handleMouseAction(this.selectionHandler.mouseReleased(mouseButtonEvent));
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
            return this.handleMouseAction(this.selectionHandler.mouseDragged(mouseButtonEvent, dragX, dragY));
        }

        @Override
        public boolean keyPressed(KeyEvent keyEvent) {
            if (super.keyPressed(keyEvent)) {
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
            if (isDelete(keyEvent) && this.canOperateFile()) {
                this.deletePack();
                return true;
            }
            if (isRename(keyEvent) && this.canOperateFile()) {
                this.renamePack();
                return true;
            }
            return false;
        }

        public void renderBack(GuiGraphics guiGraphics, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            if (!this.pack().getCompatibility().isCompatible() && !PackList.this.options.getUserConfig().isIncompatibleWarningsHidden()) {
                int backgroundLeft = left + BACKGROUND_MARGIN;
                int backgroundTop = top + BACKGROUND_MARGIN;
                int backgroundRight = backgroundLeft + width - BACKGROUND_MARGIN * 2;
                int backgroundBottom = backgroundTop + height - BACKGROUND_MARGIN;

                guiGraphics.fill(backgroundLeft, backgroundTop, backgroundRight, backgroundBottom, Theme.RED_900.getARGB());
            }
        }

        protected abstract void renderForeground(GuiGraphics guiGraphics, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick);

        private void renderSelection(GuiGraphics guiGraphics, int top, int left, int width, int height) {
            if (this.isSelected()) {
                pick(isSelectedLast(), WHITE_OVERLAY, SELECTED_OVERLAY).render(guiGraphics, left, top, width, height);
                DrawUtil.renderOutline(guiGraphics, left, top, width, height, Theme.BLUE_500.getARGB());
            }
        }

        protected void renderTop(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            if (this.folderWidget != null) {
                int folderWidgetY = this.getBottom() - this.folderWidget.getHeight() - BACKGROUND_MARGIN;
                this.folderWidget.setPosition(this.packWidget.getContentLeft(), folderWidgetY);
            }

            for (Renderable renderable : this.topRenderables) {
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }

        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovering, float partialTick) {
            hovering = hovering && PackList.this.beforeScrollbarX(mouseX) && GuiUtil.isHovered(this, mouseX, mouseY);

            int left = this.getX();
            int top = this.getY();
            int width = this.getWidth();
            int height = this.getHeight();
            int innerTop = top + V_MARGIN;
            int innerHeight = height - ROW_GAP;

            this.packWidget.setPosition(left, innerTop);
            this.packWidget.setWidth(width);

            this.renderBack(guiGraphics, top, left, width, height, mouseX, mouseY, hovering, partialTick);

            for (Renderable renderable : this.renderables) {
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            }

            this.renderSelection(guiGraphics, top, left, width, height + Y_OFFSET);
            this.renderForeground(guiGraphics, innerTop, left, width, innerHeight, mouseX, mouseY, hovering, partialTick);
            this.renderTop(guiGraphics, mouseX, mouseY, partialTick);

            if (this.devMenu != null) {
                this.devMenu.renderDevSprites(guiGraphics, innerTop, left, width);
            }
        }

        protected void handleDevMenuEvent(PackListDevMenu.Event<?> event) {
            if (event instanceof PackListDevMenu.Event.EditAliases editAliases) {
                PackList.this.sendEvent(new PackAliasOpenEvent(PackList.this, editAliases.trigger()));
            }
        }

        @Override
        public void buildItems(ContextMenuItemBuilder builder, int mouseX, int mouseY) {
            PackList.this.setFocused(this);

            var extensions = ContextMenuEventImpl.postPackEntry(PackList.this.listener.ctx(), this.context);

            ContextMenuContainer.super.buildItems(builder
                            .whenNonNull(extensions.getItems(ContextMenuEvent.PackEntry.Pos.BEFORE_HEADER))
                            .ifTrue((items, b) -> b.addAll(items))
                            .add(new PackMenuHeader(this.pack(), this.packWidget.getSprite()))
                            .whenNonNull(extensions.getItems(ContextMenuEvent.PackEntry.Pos.AFTER_HEADER))
                            .ifTrue((items, b) -> b.addAll(items))
                            .whenNonNull(this.devMenu)
                            .ifTrue(PackListDevMenu::onBuildHeader)
                            .whenNonNull(extensions.getItems(ContextMenuEvent.PackEntry.Pos.AFTER_DEV))
                            .ifTrue((items, b) -> b.addAll(items))
                            .whenNonNull(this.folderWidget)
                            .ifTrue(b -> b
                                    .simpleItem(FolderPack.FOLDER_OPEN_TEXT, this::openFolder)
                                    .separator()
                            )
                            .whenNonNull(((FilePack) this.pack()).packed_packs$getPath())
                            .ifTrue(b -> b
                                    .simpleItem(RENAME_FILE_TEXT, this::canOperateFile, this::renamePack)
                                    .simpleItem(DELETE_FILE_TEXT, this::canOperateFile, this::deletePack)
                                    .simpleItem(OPEN_FILE_TEXT, () -> PackUtil.openPack(this.pack()))
                                    .simpleItem(OPEN_PARENT_TEXT, () -> PackUtil.openParent(this.pack()))
                            )
                            .whenNonNull(extensions.getItems(ContextMenuEvent.PackEntry.Pos.AFTER_HEADER))
                            .ifTrue((items, b) -> b.addAll(items)),
                    mouseX,
                    mouseY
            );
        }

        private void openFolder() {
            PackList.this.openFolder(Objects.requireNonNull(this.folderWidget, "Cannot open folder without folder widget").getMetadata());
        }

        public boolean canOperateFile() {
            return PackList.this.fileOps.isOperable(this.pack());
        }

        public void deletePack() {
            if (PackList.this.fileOps.deletePack(this.pack())) {
                this.stale = true;
                PackList.this.remove(this.pack());
                PackList.this.sendEvent(new FileDeleteEvent(PackList.this));
            } else {
                ToastUtil.onFileFailToast(ToastUtil.getDeleteFailText(this.pack().getTitle().getString()));
            }
        }

        public void renamePack() {
            PackList.this.sendEvent(new FileRenameOpenEvent(PackList.this, this.pack()));
        }

        public void onRename(Component newName) {
            this.stale = true;
            this.packWidget.onRename(newName);
            if (this.folderWidget != null) {
                this.folderWidget.active = false;
            }
        }

        public boolean isStale() {
            return this.stale;
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
