package io.github.fishstiz.fidgetz.gui.components.contextmenu;

import com.mojang.blaze3d.platform.cursor.CursorType;
import io.github.fishstiz.fidgetz.gui.components.*;
import io.github.fishstiz.fidgetz.gui.layouts.Layouts;
import io.github.fishstiz.fidgetz.gui.renderables.ColoredRect;
import io.github.fishstiz.fidgetz.gui.renderables.RenderableRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.gui.shapes.GuiRectangle;
import io.github.fishstiz.fidgetz.util.DrawUtil;
import io.github.fishstiz.fidgetz.util.GuiUtil;
import io.github.fishstiz.fidgetz.util.ARGBColor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class ContextMenu extends ToggleableDialog<LayoutWrapper<ScrollableLayout>> {
    static final int DEFAULT_BORDER_COLOR = ARGBColor.withAlpha(Objects.requireNonNull(ChatFormatting.GRAY.getColor()), 1);
    static final int DEFAULT_TEXT_INACTIVE_COLOR = ARGBColor.withAlpha(DEFAULT_BORDER_COLOR, 0.5f);
    static final int DEFAULT_BACKGROUND_COLOR = ARGBColor.withAlpha(Objects.requireNonNull(ChatFormatting.DARK_GRAY.getColor()), 1);
    private static final int DEFAULT_SPACING = Button.DEFAULT_SPACING;
    private static final int ITEM_HEIGHT = 20;
    private static final int MAX_HEIGHT = ITEM_HEIGHT * 10;
    private static final int MIN_WIDTH = 150;
    private static final int MENU_POINT_OFFSET = 1;
    private static final int DROP_SHADOW_SIZE = 16;
    private final Builder builder;
    private final List<ContextMenu> childMenus = new ArrayList<>();
    private final int spacing;
    private final int borderColor;
    private final int softSeparatorColor;
    private final ContextMenu parentMenu;
    private Direction direction;
    private boolean forceOpen;

    protected ContextMenu(Builder builder) {
        super(builder);

        this.builder = builder;
        this.spacing = builder.spacing;
        this.borderColor = builder.borderColor;
        this.softSeparatorColor = ARGBColor.withAlpha(this.borderColor, 0.15f);
        this.parentMenu = builder.parentMenu;
        this.direction = builder.direction;
        this.root().setMinWidth(MIN_WIDTH);
        this.addListener(open -> {
            if (!open) {
                this.direction = this.builder.direction;
                this.forceOpen = false;
            }
        });
    }

    @Override
    protected void clearWidgets() {
        super.clearWidgets();
        this.forEachChild(ContextMenu::clearWidgets);
        this.childMenus.clear();
    }

    private ItemWidget createItemWidget(MenuItem item) {
        if (!item.parent()) {
            return new ItemWidget(MIN_WIDTH, this.spacing, item, this);
        }

        ContextMenu childMenu = Builder.buildChild(this);
        this.childMenus.add(childMenu);
        return new ParentItemWidget(MIN_WIDTH, this.spacing, item, this, childMenu);
    }

    private void setItems(List<? extends MenuItem> items) {
        this.clearWidgets();

        LinearLayout content = LinearLayout.vertical();

        for (int i = 0; i < items.size(); i++) {
            MenuItem current = items.get(i);
            MenuItem next = (i + 1 < items.size()) ? items.get(i + 1) : null;

            if (current == MenuItem.SEPARATOR) {
                content.addChild(new Separator(MIN_WIDTH, this.borderColor));
                continue;
            }

            content.addChild(this.createItemWidget(current));

            if (current.shouldAutoSeparate() && next != null && next.shouldAutoSeparate()) {
                content.addChild(new Separator(MIN_WIDTH, this.softSeparatorColor));
            }
        }

        this.childMenus.forEach(this::addWidget);

        ScrollableLayout layout = Layouts.unpaddedScrollableLayout(content);

        layout.setMaxHeight(MAX_HEIGHT);
        layout.visitWidgets(this::addRenderableWidget);

        this.root().setLayout(layout);
        this.root().visitWidgets(this::addRenderableWidget);
    }

    private void open(int x, int y, Direction direction, List<? extends MenuItem> items) {
        if (items.isEmpty()) return;

        this.direction = direction;
        this.setItems(items);
        this.root().arrangeElements();
        this.root().setPosition(this.clampX(x), this.clampY(y));
        this.setOpen(true);
    }

    public void open(int x, int y, List<MenuItem> items) {
        this.open(x, y, this.builder.direction, items);
    }

    private int clampX(int x) {
        GuiRectangle bounds = this.getBoundingBox();
        this.direction = this.direction.next(this.screen, bounds, x);
        return this.direction.clamp(this.screen, bounds, x);
    }

    private int clampY(int y) {
        GuiRectangle bounds = this.getBoundingBox();
        return y + bounds.getHeight() > this.screen.height ? Math.max(0, this.screen.height - bounds.getHeight()) : y;
    }

    public int getItemHeight() {
        return ITEM_HEIGHT;
    }

    @Override
    protected void renderBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, float partialTick) {
        DrawUtil.renderDropShadow(guiGraphics, x, y, width, height, DROP_SHADOW_SIZE);
        super.renderBackground(guiGraphics, x, y, width, height, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderForeground(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, float partialTick) {
        DrawUtil.renderOutline(guiGraphics, x, y, width, height, this.borderColor);

        for (ContextMenu childMenu : this.childMenus) {
            childMenu.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        GuiRectangle bounds = this.getBoundingBox();
        if (GuiUtil.containsPoint(bounds.getX() + MENU_POINT_OFFSET, bounds.getY(), bounds.getWidth(), bounds.getHeight(), mouseX, mouseY)) {
            guiGraphics.requestCursor(CursorType.DEFAULT);
        }
    }

    public @Nullable ContextMenu getOpenedChildMenu() {
        for (ContextMenu child : this.childMenus) {
            if (child.isOpen()) return child;
        }
        return null;
    }

    private boolean isHoveredAtDirection(int mouseX, int mouseY) {
        return this.isOpen() && (this.isMouseOverBounds(mouseX, mouseY) || this.direction.isHovered(mouseX, mouseY, this.getBoundingBox()));
    }

    @Override
    public boolean isMouseOverBounds(double mouseX, double mouseY) {
        if (!this.isOpen()) return false;

        if (this.getBoundingBox().containsPoint(mouseX, mouseY)) {
            return super.isMouseOverBounds(mouseX, mouseY);
        }

        ContextMenu child = this.getOpenedChildMenu();
        return child != null && child.isMouseOverBounds(mouseX, mouseY);
    }

    public void forEachChild(Consumer<ContextMenu> consumer) {
        for (ContextMenu child : this.childMenus) {
            consumer.accept(child);
            child.forEachChild(consumer);
        }
    }

    public void forEachParent(Consumer<ContextMenu> visitor) {
        if (this.parentMenu != null) {
            visitor.accept(this.parentMenu);
            this.parentMenu.forEachParent(visitor);
        }
    }

    private void closeCascade() {
        this.setOpen(false);
        this.forEachParent(parent -> parent.setOpen(false));
    }

    public static <S extends Screen & ToggleableDialogContainer> Builder builder(S screen) {
        return new Builder(screen, new LayoutWrapper<>(new ScrollableLayout(Minecraft.getInstance(), LinearLayout.vertical(), MAX_HEIGHT), MIN_WIDTH, 0));
    }

    public static class Builder extends ToggleableDialog.Builder<LayoutWrapper<ScrollableLayout>, Builder> {
        protected Direction direction = Direction.RIGHT;
        protected int backgroundColor = DEFAULT_BACKGROUND_COLOR;
        protected int borderColor = DEFAULT_BORDER_COLOR;
        protected int spacing = DEFAULT_SPACING;
        private ContextMenu parentMenu = null;

        protected <S extends Screen & ToggleableDialogContainer> Builder(S screen, LayoutWrapper<ScrollableLayout> root) {
            super(screen, root);
            this.focusOnOpen = false;
        }

        @SuppressWarnings("unchecked")
        protected static <S extends Screen & ToggleableDialogContainer> ContextMenu buildChild(ContextMenu parentMenu) {
            Builder child = builder((S) parentMenu.builder.screen);
            child.spacing = parentMenu.builder.spacing;
            child.backgroundColor = parentMenu.builder.backgroundColor;
            child.borderColor = parentMenu.builder.borderColor;
            child.background = parentMenu.builder.background;
            child.autoClose = parentMenu.builder.autoClose;
            child.focusOnOpen = parentMenu.builder.focusOnOpen;
            child.autoLoseFocus = parentMenu.builder.autoLoseFocus;
            child.parentMenu = parentMenu;
            child.direction = parentMenu.direction;
            return child.build();
        }

        public Builder setSpacing(int spacing) {
            this.spacing = spacing;
            return this;
        }

        public Builder setBorderColor(int borderColor) {
            this.borderColor = borderColor;
            return this;
        }

        @Override
        public Builder setBackground(int color) {
            this.backgroundColor = color;
            return this;
        }

        public Builder setDirection(Direction direction) {
            this.direction = direction;
            return this;
        }

        @Override
        public ContextMenu build() {
            if (this.background == null) {
                this.background = new ColoredRect(this.backgroundColor);
            }

            return new ContextMenu(this);
        }
    }

    private static class ItemWidget extends AbstractWidget implements Fidgetz {
        private static final int HOVER_OVERLAY_COLOR = ARGBColor.withAlpha(ARGBColor.WHITE, 0.1f);
        private final FidgetzText<Void> text;
        protected final ContextMenu parent;
        protected final MenuItem item;
        protected final int spacing;

        private ItemWidget(int width, int spacing, MenuItem item, ContextMenu parent) {
            super(0, 0, width, ITEM_HEIGHT, item.text());
            this.spacing = spacing;
            this.text = FidgetzText.<Void>builder()
                    .setOffsetY(MENU_POINT_OFFSET)
                    .setShadow(true)
                    .setMessage(item.text())
                    .setColor(item.textColor())
                    .build();
            this.parent = parent;
            this.item = item;
        }

        @Override
        public void playDownSound(@NonNull SoundManager handler) {
            if (this.item.active()) {
                super.playDownSound(handler);
            }
        }

        @Override
        public void onClick(@NonNull MouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
            if (this.item.active()) {
                this.item.run();
            }
            if (this.item.shouldCloseOnInteract()) {
                this.parent.closeCascade();
            }
        }

        protected void renderBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, float partialTick) {
            RenderableRect background = this.item.background();
            if (background != null) {
                background.render(guiGraphics, x, y, width, height, partialTick);
            }
        }

        protected void renderIcon(GuiGraphics guiGraphics, int x, int y, int width, int height, float partialTick) {
            Sprite icon = this.item.icon();
            if (icon != null) {
                icon.render(guiGraphics, x, y, width, height, partialTick);
            }
        }

        protected void renderText(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, float partialTick) {
            this.text.setPosition(x, y);
            this.text.setSize(width, height);
            this.text.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        }

        protected void renderHighlight(GuiGraphics guiGraphics, int x, int y, int right, int bottom, boolean hovered, float partialTick) {
            if (hovered && this.item.active()) {
                guiGraphics.fill(x, y, right, bottom, HOVER_OVERLAY_COLOR);
            }
        }

        @SuppressWarnings("unused")
        protected void renderForeground(@NonNull GuiGraphics guiGraphics, int x, int y, int width, int height, boolean hovered, double mouseX, double mouseY, float partialTick) {
            // for subclass
        }

        @Override
        protected void renderWidget(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.setTooltip(this.item.tooltip());

            this.isHovered = this.isHovered && this.isMouseOver(mouseX, mouseY);

            int x = this.getX();
            int y = this.getY();
            int width = this.getWidth();
            int height = this.getHeight();
            int right = x + width;
            int bottom = y + height;

            this.renderBackground(guiGraphics, x, y, width, height, partialTick);
            this.renderHighlight(guiGraphics, x, y, right, bottom, this.isHovered, partialTick);

            int size = Minecraft.getInstance().font.lineHeight;
            int innerX = x + this.spacing;
            int innerY = y + this.spacing;
            int innerWidth = width - this.spacing * 2;
            int innerHeight = height - this.spacing * 2;
            int iconY = innerY + (innerHeight - size) / 2;
            int textX = this.item.icon() != null ? innerX + size + this.spacing : innerX;
            int textWidth = this.item.icon() != null ? innerWidth - size - this.spacing : innerWidth;

            this.renderIcon(guiGraphics, innerX, iconY, size, size, partialTick);
            this.renderText(guiGraphics, textX, innerY, textWidth, innerHeight, mouseX, mouseY, partialTick);
            this.renderForeground(guiGraphics, x, y, width, height, this.isHovered, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            if (!this.parent.isOpen()) {
                return false;
            }
            for (ContextMenu siblingChild : this.parent.childMenus) {
                if (siblingChild.isOpen() && siblingChild.isMouseOverBounds(mouseX, mouseY)) {
                    return false;
                }
            }
            return super.isMouseOver(mouseX, mouseY);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            narrationElementOutput.add(NarratedElementType.TITLE, this.getMessage());
        }
    }

    private static class ParentItemWidget extends ItemWidget {
        private static final String CARET_RIGHT = ">";
        private final ContextMenu child;

        ParentItemWidget(int width, int spacing, MenuItem item, ContextMenu parent, ContextMenu child) {
            super(width, spacing, item, parent);
            this.child = child;
        }

        private void openChild() {
            GuiRectangle parentBounds = this.parent.getBoundingBox();
            GuiRectangle childBounds = this.child.getBoundingBox();
            Direction parentDirection = this.parent.direction;
            Direction nextDirection = parentDirection.next(parent.screen, childBounds, parentDirection.getX(parentBounds));
            int x = nextDirection.getX(this);
            int y = this.getY();
            this.parent.forEachChild(menu -> {
                if (menu != this.child) menu.setOpen(false);
            });
            this.child.open(x, y, nextDirection, this.item.children());
        }

        private void closeChildren() {
            this.child.setOpen(false);
            this.child.forEachChild(menu -> menu.setOpen(false));
        }

        @Override
        public void onClick(@NonNull MouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
            if (this.item.active()) {
                this.item.run();
                this.child.forceOpen = !this.child.forceOpen || !this.child.isOpen();
                if (!this.child.isOpen()) {
                    this.openChild();
                }
            } else if (this.item.shouldCloseOnInteract()) {
                this.parent.closeCascade();
            }
        }

        private boolean isWithinParentXBounds(int mouseX, int mouseY) {
            if (this.isHovered()) return true;

            GuiRectangle parentBox = this.parent.getBoundingBox();
            return mouseX >= parentBox.getX() &&
                   mouseX <= parentBox.getRight() &&
                   mouseY >= this.getY() &&
                   mouseY <= this.getBottom();
        }

        @Override
        protected void renderText(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, float partialTick) {
            int textWidth = width - (height + this.spacing);
            super.renderText(guiGraphics, x, y, textWidth, height, mouseX, mouseY, partialTick);

            if (this.item.active()) {
                Font font = Minecraft.getInstance().font;
                int caretWidth = font.width(CARET_RIGHT);
                int caretHeight = font.lineHeight;
                int caretX = (x + textWidth + this.spacing) + (height - caretWidth) / 2;
                int caretY = y + (height - caretHeight) / 2;
                int color = this.item.textColor();

                guiGraphics.drawString(font, CARET_RIGHT, caretX, caretY, color, false);
            }
        }

        @Override
        protected void renderHighlight(GuiGraphics guiGraphics, int x, int y, int right, int bottom, boolean hovered, float partialTick) {
            super.renderHighlight(guiGraphics, x, y, right, bottom, hovered || this.child.isOpen(), partialTick);
        }

        @Override
        protected void renderWidget(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);

            if (!this.item.active()) {
                if (this.child.isOpen()) this.closeChildren();
                this.child.forceOpen = false;
                return;
            }

            boolean looselyHovered = this.isWithinParentXBounds(mouseX, mouseY) && guiGraphics.containsPointInScissor(mouseX, mouseY);
            ContextMenu sibling = this.parent.getOpenedChildMenu();

            if (!looselyHovered && this.child.isOpen() && !this.child.forceOpen && (sibling == null || !sibling.isHoveredAtDirection(mouseX, mouseY))) {
                this.closeChildren();
            } else if (looselyHovered && this.isHovered() && !this.child.isOpen() && (sibling == null || !sibling.forceOpen && !sibling.isHoveredAtDirection(mouseX, mouseY))) {
                this.openChild();
            }
        }
    }

    private static class Separator extends AbstractWidget implements Fidgetz {
        private final int color;

        private Separator(int width, int color) {
            super(0, 0, width, 1, CommonComponents.EMPTY);
            this.color = color;
        }

        @Override
        public boolean isFocused() {
            return false;
        }

        @Override
        public boolean mouseClicked(@NonNull MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
            return false;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return this.visible && GuiUtil.containsPoint(this, mouseX, mouseY);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            guiGraphics.hLine(this.getX(), this.getRight() - 1, this.getMidY(), this.color);
        }

        @Override
        protected void updateWidgetNarration(@NonNull NarrationElementOutput narrationElementOutput) {
            // no-op
        }
    }

    public enum Direction {
        LEFT {
            @Override
            protected Direction next(Screen screen, GuiRectangle bounds, int x) {
                return x - bounds.getWidth() / 2 < 0 ? RIGHT : this;
            }

            @Override
            protected int clamp(Screen screen, GuiRectangle bounds, int x) {
                return Math.max(0, x - bounds.getWidth());
            }

            @Override
            protected int getX(GuiRectangle bounds) {
                return bounds.getX();
            }

            @Override
            protected boolean isHovered(int mouseX, int mouseY, GuiRectangle bounds) {
                return mouseX >= bounds.getX() &&
                       mouseX <= bounds.getX() + bounds.getWidth() + HOVER_LEEWAY &&
                       mouseY >= bounds.getY() &&
                       mouseY <= bounds.getY() + bounds.getHeight();
            }
        },
        RIGHT {
            @Override
            protected Direction next(Screen screen, GuiRectangle bounds, int x) {
                return x + bounds.getWidth() / 2 > screen.width ? LEFT : this;
            }

            @Override
            protected int clamp(Screen screen, GuiRectangle bounds, int x) {
                return x + bounds.getWidth() > screen.width ? screen.width - bounds.getWidth() : x;
            }

            @Override
            protected int getX(GuiRectangle bounds) {
                return bounds.getRight();
            }

            @Override
            protected boolean isHovered(int mouseX, int mouseY, GuiRectangle bounds) {
                return mouseX >= bounds.getX() - HOVER_LEEWAY &&
                       mouseX <= bounds.getX() + bounds.getWidth() &&
                       mouseY >= bounds.getY() &&
                       mouseY <= bounds.getY() + bounds.getHeight();
            }
        };

        private static final int HOVER_LEEWAY = 5;

        protected abstract Direction next(Screen screen, GuiRectangle bounds, int x);

        protected abstract int clamp(Screen screen, GuiRectangle bounds, int x);

        protected abstract int getX(GuiRectangle bounds);

        protected abstract boolean isHovered(int mouseX, int mouseY, GuiRectangle bounds);
    }
}
