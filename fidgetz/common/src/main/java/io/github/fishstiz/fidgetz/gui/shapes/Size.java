package io.github.fishstiz.fidgetz.gui.shapes;

public record Size(int width, int height) {
    private static final Size SQUARE_16 = square(16);
    private static final Size SQUARE_32 = square(32);

    public static Size square(int size) {
        return new Size(size, size);
    }

    public static Size of16() {
        return SQUARE_16;
    }

    public static Size of32() {
        return SQUARE_32;
    }
}
