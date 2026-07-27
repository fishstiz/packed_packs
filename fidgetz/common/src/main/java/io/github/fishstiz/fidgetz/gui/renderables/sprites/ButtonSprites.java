package io.github.fishstiz.fidgetz.gui.renderables.sprites;

import net.minecraft.client.gui.GuiGraphics;

public record ButtonSprites(Sprite active, Sprite inactive, boolean clamped) {
    public ButtonSprites(Sprite active, Sprite inactive) {
        this(active, inactive, true);
    }

    public static ButtonSprites unclamp(Sprite active, Sprite inactive) {
        return new ButtonSprites(active, inactive, false);
    }

    public static ButtonSprites unclamp(Sprite sprite) {
        return unclamp(sprite, sprite);
    }

    public static ButtonSprites of(Sprite sprite) {
        return new ButtonSprites(sprite, sprite);
    }

    public Sprite get(boolean active) {
        return active ? this.active : this.inactive;
    }

    public void render(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean active, float partialTick) {
        Sprite sprite = this.get(active);
        if (this.clamped) {
            sprite.renderClamped(guiGraphics, x, y, width, height, partialTick);
        } else {
            sprite.render(guiGraphics, x, y, width, height, partialTick);
        }
    }
}
