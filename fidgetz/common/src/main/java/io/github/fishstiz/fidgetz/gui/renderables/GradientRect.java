package io.github.fishstiz.fidgetz.gui.renderables;

import net.minecraft.client.gui.GuiGraphics;

public record GradientRect(int colorFrom, int colorTo, Direction direction) implements RenderableRect {
    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int width, int height, float partialTick) {
        int x2 = x + width;
        int y2 = y + height;

        switch (this.direction) {
            case UP -> guiGraphics.fillGradient(x, y, x2, y2, this.colorTo, this.colorFrom);
            case DOWN -> guiGraphics.fillGradient(x, y, x2, y2, this.colorFrom, this.colorTo);
        }
    }

    public static GradientRect fromTop(int colorFrom, int colorTo) {
        return new GradientRect(colorFrom, colorTo, Direction.DOWN);
    }

    public static GradientRect fromBottom(int colorFrom, int colorTo) {
        return new GradientRect(colorFrom, colorTo, Direction.UP);
    }

    public GradientRect flip() {
        return new GradientRect(this.colorFrom, this.colorTo, this.direction.getOpposite());
    }

    public enum Direction {
        UP,
        DOWN;

        public Direction getOpposite() {
            return this == UP ? DOWN : UP;
        }
    }
}
