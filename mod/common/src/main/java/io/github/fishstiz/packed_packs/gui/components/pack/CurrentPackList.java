package io.github.fishstiz.packed_packs.gui.components.pack;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import io.github.fishstiz.fidgetz.gui.renderables.ColoredRect;
import io.github.fishstiz.fidgetz.gui.renderables.GradientRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.util.DrawUtil;
import io.github.fishstiz.fidgetz.util.GuiUtil;
import io.github.fishstiz.packed_packs.api.context.PackContext;
import io.github.fishstiz.packed_packs.gui.components.MouseSelectionHandler;
import io.github.fishstiz.packed_packs.gui.components.SelectionContext;
import io.github.fishstiz.packed_packs.gui.components.events.DragEvent;
import io.github.fishstiz.packed_packs.gui.components.events.PackListEventListener;
import io.github.fishstiz.packed_packs.pack.PackAssetManager;
import io.github.fishstiz.packed_packs.pack.PackFileOperations;
import io.github.fishstiz.packed_packs.pack.PackOptionsContext;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import io.github.fishstiz.packed_packs.gui.components.events.MoveEvent;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.function.*;

import static io.github.fishstiz.fidgetz.util.GuiUtil.playClickSound;
import static io.github.fishstiz.packed_packs.util.InputUtil.*;
import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.*;
import static io.github.fishstiz.fidgetz.util.lang.ObjectsUtil.*;
import static io.github.fishstiz.packed_packs.util.ResourceUtil.getVanillaSprite;

public class CurrentPackList extends PackList {
    private static final Sprite UNSELECT_HIGHLIGHTED_SPRITE = Sprite.of32(getVanillaSprite("transferable_list/unselect_highlighted"));
    private static final Sprite UNSELECT_SPRITE = Sprite.of32(getVanillaSprite("transferable_list/unselect"));
    private static final Sprite MOVE_UP_HIGHLIGHTED_SPRITE = Sprite.of32(getVanillaSprite("transferable_list/move_up_highlighted"));
    private static final Sprite MOVE_UP_SPRITE = Sprite.of32(getVanillaSprite("transferable_list/move_up"));
    private static final Sprite MOVE_DOWN_HIGHLIGHTED_SPRITE = Sprite.of32(getVanillaSprite("transferable_list/move_down_highlighted"));
    private static final Sprite MOVE_DOWN_SPRITE = Sprite.of32(getVanillaSprite("transferable_list/move_down"));
    private static final Theme DROP_THEME = Theme.GREEN_500;
    private static final ColoredRect DROP_INDEX = new ColoredRect(DROP_THEME.getARGB());
    private static final GradientRect SCROLL_UP = GradientRect.fromTop(DROP_THEME.withAlpha(0.75f), DROP_THEME.withAlpha(0));
    private static final GradientRect SCROLL_DOWN = SCROLL_UP.flip();
    private static final int DROP_INDEX_PADDING = 3;
    private static final double SCROLL_STEP = 10;
    private boolean scrolling;

    public CurrentPackList(PackOptionsContext options, PackAssetManager assets, PackFileOperations fileOps, PackListEventListener listener) {
        super(options, assets, fileOps, listener);
    }

    @Override
    protected @NonNull Entry createEntry(PackContext context, SelectionContext<Pack> selectionContext, int index) {
        return new Entry(context, selectionContext, index);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        boolean keyPressed = super.keyPressed(keyEvent);
        if (!keyPressed) {
            PackList.Entry entry = this.getEntry(this.getLastSelected());
            if (entry instanceof Entry movableEntry) {
                if (isMoveDown(keyEvent)) {
                    if (movableEntry.moveDown()) playClickSound();
                    return true;
                } else if (isMoveUp(keyEvent)) {
                    if (movableEntry.moveUp()) playClickSound();
                    return true;
                }
            }
        }
        return keyPressed;
    }

    private void scrollStep(MoveDirection direction, float partialTick) {
        double scrollAmount = this.scrollAmount();
        if (direction.isUp()) {
            scrollAmount -= SCROLL_STEP * partialTick;
        } else if (direction.isDown()) {
            scrollAmount += SCROLL_STEP * partialTick;
        }

        this.scrolling = true;
        this.setClampedScrollAmount(scrollAmount);
    }

    private int getDropIndex(double mouseY) {
        if (this.children().isEmpty()) return -1;

        int index = this.getRowIndex(mouseY);
        if (index == -1) return -1;

        PackList.Entry entry = this.getEntry(index);
        int centerY = entry.getY() + (entry.getHeight() / 2);

        if (mouseY >= centerY) {
            int next = index + 1;
            return next < this.children().size() ? next : -1;
        }
        return index;
    }

    private int toPackIndex(int dropIndex) {
        List<PackList.Entry> children = this.children();
        if (dropIndex == -1) {
            return !children.isEmpty() ? this.list.indexOf(children.getLast().pack()) + 1 : -1;
        }
        return Math.clamp(this.list.indexOf(children.get(dropIndex).pack()), 0, this.list.size());
    }

    private boolean isMouserOverSelection(List<Pack> selection, double mouseX, double mouseY) {
        for (Pack selected : selection) {
            PackList.Entry entry = this.getEntry(selected);
            if (entry != null && entry.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canInteract(PackList source) {
        return true;
    }

    @Override
    public boolean canDrop(DragEvent dragEvent, double mouseX, double mouseY) {
        PackList source = dragEvent.target();
        List<Pack> payload = dragEvent.payload();
        Pack trigger = dragEvent.trigger();

        if (this.scrolling || this.isQueried() || this.options.isLocked() || payload.isEmpty() || !source.canInteract(this)) {
            return false;
        }
        if (this.children().isEmpty()) {
            return true;
        }
        if ((source == this && this.options.isFixed(trigger)) || this.isMouserOverSelection(payload, mouseX, mouseY)) {
            return false;
        }
        int dropIndex = this.getDropIndex(mouseY);
        if (!this.list.isValidDropPosition(this.toPackIndex(dropIndex))) {
            return false;
        }
        if (source != this) {
            return source.isTransferable(trigger);
        }
        return this.list.isValidInsertPosition(dropIndex, payload);
    }

    @Override
    protected List<Pack> handleDrop(DragEvent dragEvent, double mouseX, double mouseY) {
        if (!this.canDrop(dragEvent, mouseX, mouseY)) {
            return Collections.emptyList();
        }

        PackList source = dragEvent.target();
        List<Pack> payload = dragEvent.payload();

        int dropPackIndex = this.list.clampPosition(this.toPackIndex(this.getDropIndex(mouseY)));
        if (source == this) {
            List<Pack> movable = new ObjectArrayList<>(payload);
            movable.removeIf(this.options::isFixed);
            return this.moveAll(movable, dropPackIndex) ? payload : Collections.emptyList();
        }

        this.clearSelection();
        List<Pack> dropped = new ObjectArrayList<>(payload);
        for (Pack selected : payload) {
            if (source.isTransferable(selected)) {
                dropped.add(selected);
                this.addOrMove(selected, dropPackIndex);
            }
        }
        this.refreshList();
        source.removeAll(dropped);
        dropped.forEach(this::select);
        this.select(dragEvent.trigger());

        return dropped;
    }

    private void renderDropIndex(GuiGraphics guiGraphics, int mouseY, int x, int width) {
        int dropIndex = this.getDropIndex(mouseY);
        int rowTop = Math.clamp(
                this.getRowTop(dropIndex != -1 ? dropIndex : this.children().size()),
                this.getY() + this.offsetY + DROP_INDEX_PADDING,
                this.getBottom() - this.rowGap - DROP_INDEX_PADDING
        );
        int indexY = rowTop - this.rowGap - DROP_INDEX_PADDING;

        guiGraphics.enableScissor(this.getX(), this.getY(), this.getRight(), this.getBottom());
        DROP_INDEX.render(guiGraphics, x, indexY, width, rowTop - indexY + DROP_INDEX_PADDING);
        guiGraphics.disableScissor();
    }

    @Override
    public void renderDroppableZone(GuiGraphics guiGraphics, DragEvent dragEvent, int mouseX, int mouseY, float partialTick) {
        PackList source = dragEvent.target();
        if (this.options.isLocked() || !source.canInteract(this)) {
            return;
        }

        int x = this.getX();
        int y = this.getY();
        int width = this.scrollbarVisible() ? this.getWidth() - this.scrollbarOffset : this.getWidth();
        int height = this.getHeight();
        int bottom = this.getBottom();

        if (this.isMouseOver(mouseX, mouseY)) {
            double scrollAmount = this.scrollAmount();

            int scrollDownY = bottom - this.getItemHeight();
            if (scrollAmount < this.maxScrollAmount() && mouseY >= scrollDownY) {
                SCROLL_DOWN.render(guiGraphics, x, scrollDownY, width, this.getItemHeight());
                this.scrollStep(MoveDirection.DOWN, partialTick);
            } else if (scrollAmount > 0 && mouseY <= y + this.getItemHeight()) {
                SCROLL_UP.render(guiGraphics, x, y, width, this.getItemHeight());
                this.scrollStep(MoveDirection.UP, partialTick);
            } else {
                this.scrolling = false;
            }

            if (this.canDrop(dragEvent, mouseX, mouseY)) {
                this.renderDropIndex(guiGraphics, mouseY, x, width);
            }
        }

        DrawUtil.renderOutline(guiGraphics, x, y, width, height, DROP_THEME.getARGB());
    }

    public class Entry extends PackList.Entry {
        protected Entry(PackContext context, SelectionContext<Pack> selectionContext, int index) {
            super(context, selectionContext, index);
        }

        @Override
        public boolean isTransferable() {
            return super.isTransferable() && !CurrentPackList.this.options.isRequired(this.pack());
        }

        @Override
        protected boolean handleMouseAction(MouseSelectionHandler.Action action) {
            if (action == MouseSelectionHandler.Action.DRAG) {
                return !this.isFixed() && super.handleMouseAction(action);
            }
            return super.handleMouseAction(action);
        }

        public boolean isFixed() {
            return CurrentPackList.this.options.isFixed(this.pack()) ||
                   CurrentPackList.this.options.isLocked() ||
                   CurrentPackList.this.isQueried() ||
                   this.isStale();
        }

        public boolean canMoveDown() {
            return !this.isFixed() && CurrentPackList.this.list.canMoveDown(this.pack());
        }

        public boolean canMoveUp() {
            return !this.isFixed() && CurrentPackList.this.list.canMoveUp(this.pack());
        }

        public boolean isMouseOverRemove(double mouseX, double mouseY) {
            return CurrentPackList.this.isHovered() && this.isTransferable() && GuiUtil.containsPoint(
                    this.getX() + H_SPACING,
                    this.getY(),
                    UNSELECT_SPRITE.width / 2,
                    UNSELECT_SPRITE.height,
                    mouseX,
                    mouseY
            );
        }

        public boolean isMouseOverUp(double mouseX, double mouseY) {
            return CurrentPackList.this.isHovered() && this.canMoveUp() && GuiUtil.containsPoint(
                    this.getX() + H_SPACING + MOVE_UP_SPRITE.width / 2,
                    this.getY(),
                    MOVE_UP_SPRITE.width / 2,
                    MOVE_UP_SPRITE.height / 2,
                    mouseX,
                    mouseY
            );
        }

        public boolean isMouseOverDown(double mouseX, double mouseY) {
            return CurrentPackList.this.isHovered() && this.canMoveDown() && GuiUtil.containsPoint(
                    this.getX() + H_SPACING + MOVE_DOWN_SPRITE.width / 2,
                    this.getY() + MOVE_DOWN_SPRITE.height / 2,
                    MOVE_DOWN_SPRITE.width / 2,
                    MOVE_DOWN_SPRITE.height / 2,
                    mouseX,
                    mouseY
            );
        }

        private void sendMoveEvent(List<Pack> moved) {
            CurrentPackList.this.sendEvent(new MoveEvent(CurrentPackList.this, this.pack(), moved));
            PackList.Entry entry = CurrentPackList.this.getEntry(this.pack());
            if (entry != null) CurrentPackList.this.scrollToEntry(entry);
        }

        private boolean move(MoveDirection moveDirection) {
            List<Pack> selectedPacks = this.selectionContext.getItemOrSelection();
            if (selectedPacks.size() == 1) {
                Pack pack = selectedPacks.getFirst();
                if (moveDirection.movePack(CurrentPackList.this.list, pack)) {
                    CurrentPackList.this.selectExclusive(pack);
                    CurrentPackList.this.refreshList();
                    this.sendMoveEvent(selectedPacks);
                    return true;
                }
            } else if (selectedPacks.size() > 1) {
                Pack lastSelected = this.selectionContext.selection().getLast();
                List<Pack> moved = moveDirection.moveSelection(CurrentPackList.this.list, selectedPacks);
                if (!moved.isEmpty()) {
                    CurrentPackList.this.select(lastSelected);
                    CurrentPackList.this.refreshList();
                    this.sendMoveEvent(moved);
                    return true;
                }
            }
            return false;
        }

        public boolean moveUp() {
            return this.move(MoveDirection.UP);
        }

        public boolean moveDown() {
            return this.move(MoveDirection.DOWN);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
            if (isLeftClick(mouseButtonEvent)) {
                double mouseX = mouseButtonEvent.x();
                double mouseY = mouseButtonEvent.y();

                if (this.isMouseOverRemove(mouseX, mouseY)) {
                    return this.consumeClick(Entry::transfer);
                } else if (this.isMouseOverUp(mouseX, mouseY)) {
                    return this.consumeClick(Entry::moveUp);
                } else if (this.isMouseOverDown(mouseX, mouseY)) {
                    return this.consumeClick(Entry::moveDown);
                }
            }
            return super.mouseClicked(mouseButtonEvent, doubleClicked);
        }

        private boolean consumeClick(Consumer<Entry> action) {
            playClickSound();
            action.accept(this);
            return false;
        }

        @Override
        protected void renderForeground(GuiGraphics guiGraphics, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            if (!hovering && !this.isSelectedLast()) return;

            int x = left + H_SPACING;
            WHITE_OVERLAY.render(guiGraphics, x, top, UNSELECT_SPRITE.width, UNSELECT_SPRITE.height);

            if (this.isTransferable()) {
                boolean overRemove = this.isMouseOverRemove(mouseX, mouseY);
                pick(!overRemove, UNSELECT_SPRITE, UNSELECT_HIGHLIGHTED_SPRITE).render(guiGraphics, x, top);
                this.updateCursor(guiGraphics, overRemove);
            }
            if (this.canMoveUp()) {
                boolean overUp = this.isMouseOverUp(mouseX, mouseY);
                pick(!overUp, MOVE_UP_SPRITE, MOVE_UP_HIGHLIGHTED_SPRITE).render(guiGraphics, x, top);
                this.updateCursor(guiGraphics, overUp);
            }
            if (this.canMoveDown()) {
                boolean overDown = this.isMouseOverDown(mouseX, mouseY);
                pick(!overDown, MOVE_DOWN_SPRITE, MOVE_DOWN_HIGHLIGHTED_SPRITE).render(guiGraphics, x, top);
                this.updateCursor(guiGraphics, overDown);
            }
        }

        private void updateCursor(GuiGraphics guiGraphics, boolean hoveringButton) {
            if (hoveringButton) {
                guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
            }
        }
    }

    enum MoveDirection {
        UP {
            @Override
            boolean movePack(PackListModel list, Pack pack) {
                return list.moveUp(pack);
            }

            @Override
            List<Pack> moveSelection(PackListModel list, List<Pack> selection) {
                return list.moveSelectionUp(selection);
            }
        },
        DOWN {
            @Override
            boolean movePack(PackListModel list, Pack pack) {
                return list.moveDown(pack);
            }

            @Override
            List<Pack> moveSelection(PackListModel list, List<Pack> selection) {
                return list.moveSelectionDown(selection);
            }
        };

        boolean isUp() {
            return this == MoveDirection.UP;
        }

        boolean isDown() {
            return this == MoveDirection.DOWN;
        }

        abstract boolean movePack(PackListModel list, Pack pack);

        abstract List<Pack> moveSelection(PackListModel list, List<Pack> selection);
    }
}
