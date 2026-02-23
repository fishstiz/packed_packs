package io.github.fishstiz.fidgetz.gui.components;

import io.github.fishstiz.fidgetz.util.LogUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public abstract class AbstractFixedListWidget<T extends AbstractFixedListWidget<T>.Entry> extends ContainerObjectSelectionList<T> {
    protected static final int DEFAULT_SCROLLBAR_OFFSET = 6;
    protected int scrollbarOffset;
    protected int offsetY;
    protected int rowGap;

    protected AbstractFixedListWidget(int itemHeight, int scrollbarOffset, int offsetY, int rowGap) {
        super(Minecraft.getInstance(), 0, 0, 0, itemHeight);

        this.scrollbarOffset = scrollbarOffset;
        this.offsetY = offsetY;
        this.rowGap = rowGap;
    }

    protected AbstractFixedListWidget(int itemHeight) {
        this(itemHeight, DEFAULT_SCROLLBAR_OFFSET, 0, 0);
    }

    @Override
    protected int scrollBarX() {
        return this.getRight() - this.scrollbarOffset;
    }

    protected boolean beforeScrollbarX(double mouseX) {
        return !this.scrollbarVisible() || mouseX < this.scrollBarX();
    }

    public int getMaxPosition() {
        return this.getItemCount() * this.itemHeight + this.offsetY;
    }

    @Override
    public int maxScrollAmount() {
        return Math.max(0, this.getMaxPosition() - (this.getBottom() - (this.getY() + this.offsetY * 2)) + (this.rowGap * getItemCount()) - this.rowGap);
    }

    public void setClampedScrollAmount(double scrollAmount) {
        this.setScrollAmount(Math.clamp(scrollAmount, 0d, this.maxScrollAmount()));
    }

    public void clampScrollAmount() {
        this.setClampedScrollAmount(this.scrollAmount());
    }

    @Override
    public int getRowTop(int index) {
        return this.getY() + this.offsetY + (this.itemHeight + this.rowGap) * index - (int) this.scrollAmount();
    }

    protected int getRowIndex(double y) {
        int index = ((int) Math.floor(y - this.getY() - this.offsetY) + (int) this.scrollAmount()) / (this.itemHeight + this.rowGap);
        return index >= 0 && index < this.getItemCount() ? index : -1;
    }

    @Override
    protected @Nullable T getEntryAtPosition(double mouseX, double mouseY) {
        int index = this.getRowIndex(mouseY);
        return index >= 0 && this.isMouseOver(mouseX, mouseY) ? this.getEntry(index) : null;
    }

    @Override
    protected void renderListItems(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int left = this.getX();
        int width = this.getWidth() - (this.scrollbarVisible() ? this.scrollbarOffset : 0);

        for (int i = 0; i < this.getItemCount(); i++) {
            int top = this.getRowTop(i);
            int bottom = this.getRowBottom(i);
            if (bottom >= this.getY() && top <= this.getBottom()) {
                this.renderItem(guiGraphics, mouseX, mouseY, partialTick, i, left, top, width, this.itemHeight);
            }
        }
    }

    protected @Nullable T getPreviousEntry(T current) {
        if (current != null && current.getIndex() > 0) {
            return this.children().get(current.getIndex() - 1);
        } else if (current == null && !this.children().isEmpty()) {
            return this.getFirstElement();
        }
        return null;
    }

    protected @Nullable T getNextEntry(T current) {
        if (current != null && current.getIndex() + 1 < this.children().size()) {
            return this.children().get(current.getIndex() + 1);
        } else if (current == null && !this.children().isEmpty()) {
            return this.getFirstElement();
        }
        return null;
    }

    //  === compat ===

    protected int getItemHeight() {
        return this.itemHeight;
    }

    protected void scrollToEntry(T entry) {
        this.ensureVisible(entry);
    }

    public abstract class Entry extends ContainerObjectSelectionList.Entry<T> implements LayoutElement {
        protected final int index;

        protected Entry(int index) {
            this.index = index;
        }

        protected Entry() {
            this(AbstractFixedListWidget.this.children().size());
        }

        public int getIndex() {
            return this.index;
        }

        @Override
        public final void setX(int x) {
            LogUtil.logUnsupported();
        }

        @Override
        public final void setY(int y) {
            LogUtil.logUnsupported();
        }

        @Override
        public int getX() {
            return AbstractFixedListWidget.this.getX();
        }

        @Override
        public int getY() {
            return AbstractFixedListWidget.this.getRowTop(this.index);
        }

        @Override
        public int getWidth() {
            int offset = AbstractFixedListWidget.this.scrollbarVisible() ? AbstractFixedListWidget.this.scrollbarOffset : 0;
            return AbstractFixedListWidget.this.getWidth() - offset;
        }

        @Override
        public int getHeight() {
            return AbstractFixedListWidget.this.itemHeight;
        }

        public int getRight() {
            return this.getX() + this.getWidth();
        }

        public int getBottom() {
            return this.getY() + this.getHeight();
        }

        @Override
        public void visitWidgets(Consumer<AbstractWidget> consumer) {
        }

        @Override
        public @NotNull ScreenRectangle getRectangle() {
            return new ScreenRectangle(this.getX(), this.getY(), this.getWidth(), this.getHeight());
        }
    }
}
