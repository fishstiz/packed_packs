package io.github.fishstiz.fidgetz.gui.components;

import io.github.fishstiz.fidgetz.gui.Metadata;
import io.github.fishstiz.fidgetz.transform.interfaces.IStringWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public class FidgetzText<E> extends StringWidget implements Fidgetz, Metadata<E> {
    private E metadata;

    private FidgetzText(Builder<E> builder) {
        super(
                builder.x,
                builder.y,
                builder.width,
                builder.height,
                applyStylesFromBuilder(builder.message, builder),
                builder.font
        );

        this.metadata = builder.metadata;

        ((IStringWidget) this).fidgetz$setOffsetY(builder.offsetY);
    }

    private static Component applyStylesFromBuilder(Component message, Builder<?> builder) {
        if (builder.color == null && builder.shadow) return message;

        return message.copy().withStyle(style -> {
            if (builder.color != null) {
                style = style.withColor(builder.color);
            }
            if (!builder.shadow) {
                style = style.withoutShadow();
            }
            return style;
        });

    }

    public void setOffsetY(int offsetY) {
        ((IStringWidget) this).fidgetz$setOffsetY(offsetY);
    }

    @Override
    public void renderWidget(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.isHovered = this.isHovered && Fidgetz.super.isHovered(mouseX, mouseY);
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.visible && Fidgetz.super.isMouseOver(mouseX, mouseY);
    }

    @Override
    public E getMetadata() {
        return this.metadata;
    }

    @Override
    public void setMetadata(E metadata) {
        this.metadata = metadata;
    }

    public static <E> Builder<E> builder(Font font) {
        return new Builder<>(font);
    }

    public static <E> Builder<E> builder() {
        return builder(Minecraft.getInstance().font);
    }

    public static class Builder<E> extends AbstractWidgetBuilder<Builder<E>> {
        private final Font font;
        private int offsetY;
        private Component message = CommonComponents.EMPTY;
        private Integer color;
        private boolean shadow = true;
        private E metadata;

        private Builder(Font font) {
            this.font = font;
        }

        public Builder<E> setOffsetY(int offsetY) {
            this.offsetY = offsetY;
            return this;
        }

        public Builder<E> setColor(Integer color) {
            this.color = color;
            return this;
        }

        public Builder<E> setShadow(boolean shadow) {
            this.shadow = shadow;
            return this;
        }

        public Builder<E> setMessage(Component message) {
            this.message = message;
            return this;
        }

        public Builder<E> setMessage(String message) {
            this.message = Component.translatable(message);
            return this;
        }

        public Builder<E> setMetadata(E metadata) {
            this.metadata = metadata;
            return this;
        }

        public FidgetzText<E> build() {
            return new FidgetzText<>(this);
        }
    }
}
