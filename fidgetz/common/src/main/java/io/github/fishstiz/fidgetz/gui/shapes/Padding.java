package io.github.fishstiz.fidgetz.gui.shapes;

public record Padding(int top, int right, int bottom, int left) {
    private static final Padding EMPTY = new Padding();

    public Padding() {
        this(0, 0, 0, 0);
    }

    public Padding(int padding) {
        this(padding, padding, padding, padding);
    }

    public static Padding empty() {
        return EMPTY;
    }
}
