package io.github.fishstiz.fidgetz.gui.components;

import io.github.fishstiz.fidgetz.util.GuiUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public abstract class AbstractFixedListWidget<T extends AbstractFixedListWidget<T>.Entry> extends ContainerObjectSelectionList<T> {
    protected static final int DEFAULT_SCROLLBAR_OFFSET = 6;
    protected final int scrollbarOffset;
    protected final int offsetY;
    protected final int rowGap;

    protected AbstractFixedListWidget(int itemHeight, int scrollbarOffset, int offsetY, int rowGap) {
        super(Minecraft.getInstance(), 0, 0, 0, itemHeight + rowGap);

        this.scrollbarOffset = scrollbarOffset;
        this.offsetY = offsetY;
        this.rowGap = rowGap;
    }

    protected AbstractFixedListWidget(int itemHeight, int offsetY, int rowGap) {
        this(itemHeight, DEFAULT_SCROLLBAR_OFFSET, offsetY, rowGap);
    }

    protected AbstractFixedListWidget(int itemHeight) {
        this(itemHeight, 0, 0);
    }

    @Override
    protected boolean isOverScrollbar(double mouseX, double mouseY) {
        return super.isOverScrollbar(mouseX, mouseY) && this.isHovered();
    }

    protected boolean beforeScrollbarX(double mouseX) {
        return !this.scrollbarVisible() || mouseX < this.scrollBarX();
    }

    public void setClampedScrollAmount(double scrollAmount) {
        this.setScrollAmount(Math.clamp(scrollAmount, 0d, this.maxScrollAmount()));
    }

    public void clampScrollAmount() {
        this.setClampedScrollAmount(this.scrollAmount());
    }

    @Override
    protected int scrollBarX() {
        return this.getRight() - SCROLLBAR_WIDTH;
    }

    @Override
    public int getRowWidth() {
        return this.scrollbarVisible() ? this.getWidth() - SCROLLBAR_WIDTH : this.getWidth();
    }

    @Override
    public int getRowLeft() {
        return this.getX();
    }

    @Override
    public int getRowTop(int index) {
        return (this.getY() + this.offsetY) - (int) this.scrollAmount() + index * this.defaultEntryHeight;
    }

    protected int getRowIndex(double y) {
        int index = (int) Math.floor(Math.max(0, y - (this.getY() + this.offsetY) + this.scrollAmount()) / this.defaultEntryHeight);
        return index >= 0 && index < this.getItemCount() ? index : -1;
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        this.clampScrollAmount();
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        this.clampScrollAmount();
    }

    @Override
    protected int contentHeight() {
        return this.offsetY * 2 + this.getItemCount() * this.defaultEntryHeight - this.rowGap;
    }

    public int getItemHeight() {
        return this.defaultEntryHeight - this.rowGap;
    }

    protected T getEntry(int index) {
        return this.children().get(index);
    }

    protected @Nullable T getPreviousEntry(T current) {
        if (current != null && current.getIndex() > 0) {
            return this.children().get(current.getIndex() - 1);
        } else if (current == null && !this.children().isEmpty()) {
            return this.children().getFirst();
        }
        return null;
    }

    protected @Nullable T getNextEntry(T current) {
        if (current != null && current.getIndex() + 1 < this.children().size()) {
            return this.children().get(current.getIndex() + 1);
        } else if (current == null && !this.children().isEmpty()) {
            return this.children().getFirst();
        }
        return null;
    }

    public abstract class Entry extends ContainerObjectSelectionList.Entry<T> {
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
        public int getX() {
            return AbstractFixedListWidget.this.getRowLeft();
        }

        @Override
        public int getY() {
            return AbstractFixedListWidget.this.getRowTop(this.index);
        }

        @Override
        public int getWidth() {
            return AbstractFixedListWidget.this.getRowWidth();
        }

        @Override
        public int getHeight() {
            return AbstractFixedListWidget.this.getItemHeight();
        }

        public int getRight() {
            return this.getX() + this.getWidth();
        }

        public int getBottom() {
            return this.getY() + this.getHeight();
        }

        @Override
        public @NonNull ScreenRectangle getRectangle() {
            return new ScreenRectangle(this.getX(), this.getY(), this.getWidth(), this.getHeight());
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return GuiUtil.containsPoint(this, mouseX, mouseY);
        }
    }
}
