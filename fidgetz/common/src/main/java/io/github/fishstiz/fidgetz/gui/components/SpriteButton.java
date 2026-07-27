package io.github.fishstiz.fidgetz.gui.components;

import io.github.fishstiz.fidgetz.gui.renderables.sprites.ButtonSprites;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Objects;

public class SpriteButton<E> extends FidgetzButton<E> {
    private final Sprites sprites;

    protected SpriteButton(Builder<E> builder) {
        super(builder);

        this.sprites = builder.sprites;
    }

    @Override
    protected boolean hasSprite() {
        return true;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.sprites.get(this.active).get(this.isHoveredOrFocused()).render(guiGraphics, this.getX(), this.getY());
        this.renderForeground(guiGraphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), partialTick);
    }

    public static <E> Builder<E> builder(Sprites sprites) {
        return new Builder<>(sprites);
    }

    public static class Builder<E> extends FidgetzButton.Builder<E, Builder<E>> {
        private final Sprites sprites;

        private Builder(Sprites sprites) {
            this.sprites = Objects.requireNonNull(sprites);
        }

        @Override
        public Builder<E> setSprite(Sprite sprite) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Builder<E> setSprite(ButtonSprites sprites) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SpriteButton<E> build() {
            return new SpriteButton<>(this);
        }
    }

    public record Sprites(ButtonSprites active, ButtonSprites inactive) {
        public static Sprites of(ButtonSprites active) {
            return new Sprites(active, ButtonSprites.of(active.get(false)));
        }

        public ButtonSprites get(boolean active) {
            return active ? this.active : this.inactive;
        }
    }
}
