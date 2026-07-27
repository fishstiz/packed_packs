package io.github.fishstiz.fidgetz.gui.shapes;

public record Line(int start, int length) {
    public int end() {
        return this.start + this.length;
    }

    public static Line zero(int length) {
        return new Line(0, length);
    }
}
