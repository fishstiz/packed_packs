package io.github.fishstiz.fidgetz.gui.components;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import io.github.fishstiz.fidgetz.gui.Metadata;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuProvider;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.renderables.RenderableRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.ButtonSprites;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.util.DrawUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

public class FidgetzButton<E> extends Button.Plain implements Fidgetz, ContextMenuProvider, Metadata<E> {
    private final List<Runnable> listeners;
    private final Integer focusedBorder;
    private final boolean spriteOnly;
    private final boolean focusOnInteract;
    private final BiConsumer<FidgetzButton<E>, ContextMenuItemBuilder> contextMenuBuilder;
    private final RenderableRect foreground;
    private ButtonSprites sprites;
    private E metadata;

    protected FidgetzButton(Builder<E, ?> builder) {
        super(builder.x, builder.y, builder.width, builder.height, builder.message, builder.onPress, DEFAULT_NARRATION);

        this.metadata = builder.metadata;
        this.sprites = builder.sprites;
        this.spriteOnly = builder.spriteOnly;
        this.foreground = builder.foreground;
        this.focusedBorder = builder.focusedBorder;
        this.focusOnInteract = builder.focusOnInteract;
        this.contextMenuBuilder = builder.contextMenuBuilder;
        this.listeners = !builder.listeners.isEmpty() ? builder.listeners : Collections.emptyList();

        if (builder.tooltip != null) {
            this.setTooltip(builder.tooltip);
        }
    }

    @Override
    public void onPress(@NonNull InputWithModifiers inputWithModifiers) {
        super.onPress(inputWithModifiers);

        for (var listener : this.listeners) {
            listener.run();
        }
    }

    public void setSprites(ButtonSprites sprites) {
        this.sprites = sprites;
    }

    @Override
    public E getMetadata() {
        return this.metadata;
    }

    @Override
    public void setMetadata(E metadata) {
        this.metadata = metadata;
    }

    protected boolean hasSprite() {
        return this.sprites != null;
    }

    protected void renderSprite(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, float partialTick) {
        this.sprites.render(guiGraphics, x, y, width, height, this.active, partialTick);
    }

    protected void renderBorder(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, float partialTick) {
        DrawUtil.renderOutline(guiGraphics, x, y, width, height, this.focusedBorder);
    }

    protected void renderForeground(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, float partialTick) {
        if (this.foreground != null) {
            this.foreground.render(guiGraphics, x, y, width, height, partialTick);
        }
    }

    @Override
    protected void extractContents(@NonNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.isHovered = this.isHovered && this.isHovered(mouseX, mouseY);

        if (!this.spriteOnly) {
            super.extractContents(guiGraphics, mouseX, mouseY, partialTick);
        }

        if (this.hasSprite()) {
            int spriteWidth = this.getWidth();
            int spriteHeight = this.getHeight();
            int spriteX = this.getX() + (this.getWidth() - spriteWidth) / 2;
            int spriteY = this.getY() + (this.getHeight() - spriteHeight) / 2;

            this.renderSprite(guiGraphics, spriteX, spriteY, spriteWidth, spriteHeight, partialTick);
        }

        if (this.isHoveredOrFocused() && this.focusedBorder != null) {
            this.renderBorder(guiGraphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), partialTick);
        }

        this.renderForeground(guiGraphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), partialTick);

        this.updateCursor(guiGraphics);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.visible && Fidgetz.super.isMouseOver(mouseX, mouseY);
    }

    protected void updateCursor(GuiGraphicsExtractor guiGraphics) {
        if (this.isHovered()) {
            guiGraphics.requestCursor(this.isActive() ? CursorTypes.POINTING_HAND : CursorTypes.NOT_ALLOWED);
        }
    }

    @Override
    protected void extractDefaultLabel(@NonNull ActiveTextCollector activeTextCollector) {
        if (!this.hasSprite()) {
            super.extractDefaultLabel(activeTextCollector);
        }
    }

    @Override
    public boolean shouldTakeFocusAfterInteraction() {
        return this.focusOnInteract;
    }

    @Override
    public void buildItems(ContextMenuItemBuilder builder, int mouseX, int mouseY) {
        if (this.contextMenuBuilder != null) {
            this.contextMenuBuilder.accept(this, builder);
        }
    }

    public static <E> Builder<E, ?> builder() {
        return new Builder<>();
    }

    public static class Builder<E, B extends Builder<E, B>> extends AbstractWidgetBuilder<B> {
        private final List<Runnable> listeners = new ObjectArrayList<>();
        private Component message = CommonComponents.EMPTY;
        private Tooltip tooltip;
        private ButtonSprites sprites;
        private boolean spriteOnly = false;
        private RenderableRect foreground;
        private Integer focusedBorder;
        private boolean focusOnInteract = true;
        private OnPress onPress = btn -> {};
        private BiConsumer<FidgetzButton<E>, ContextMenuItemBuilder> contextMenuBuilder;
        private E metadata;

        protected Builder() {
        }

        public B setMessage(Component message) {
            this.message = message;
            return self();
        }

        public B setMessage(String message) {
            return this.setMessage(Component.translatable(message));
        }

        public B setTooltip(Tooltip tooltip) {
            this.tooltip = tooltip;
            return self();
        }

        public B setSprite(ButtonSprites sprites) {
            this.sprites = sprites;
            return self();
        }

        public B setSprite(Sprite sprite) {
            this.sprites = ButtonSprites.of(sprite);
            return self();
        }

        public B spriteOnly() {
            this.spriteOnly = true;
            return self();
        }

        public B setForeground(RenderableRect foreground) {
            this.foreground = foreground;
            return self();
        }

        public B setFocusedBorder(Integer hoverBorder) {
            this.focusedBorder = hoverBorder;
            return self();
        }

        public B setFocusOnInteract(boolean focusOnInteract) {
            this.focusOnInteract = focusOnInteract;
            return self();
        }

        public B setOnPress(OnPress onPress) {
            this.onPress = onPress;
            return self();
        }

        public B setOnPress(Runnable onPress) {
            this.onPress = btn -> onPress.run();
            return self();
        }

        public B addListener(Runnable listener) {
            this.listeners.add(listener);
            return self();
        }

        public B setContextMenuBuilder(BiConsumer<FidgetzButton<E>, ContextMenuItemBuilder> contextMenuBuilder) {
            this.contextMenuBuilder = contextMenuBuilder;
            return self();
        }

        public B setMetadata(E metadata) {
            this.metadata = metadata;
            return self();
        }

        public FidgetzButton<E> build() {
            return new FidgetzButton<>(this);
        }
    }
}
