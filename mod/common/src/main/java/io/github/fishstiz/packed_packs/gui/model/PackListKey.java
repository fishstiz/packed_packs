package io.github.fishstiz.packed_packs.gui.model;

public record PackListKey(PackListType type, int depth) {
    private static final PackListKey AVAILABLE_ROOT = new PackListKey(PackListType.AVAILABLE, 0);
    private static final PackListKey ENABLED_ROOT = new PackListKey(PackListType.ENABLED, 0);

    public static PackListKey available() {
        return AVAILABLE_ROOT;
    }

    public static PackListKey enabled() {
        return ENABLED_ROOT;
    }

    public static PackListKey root(PackListType type) {
        return switch (type) {
            case AVAILABLE -> available();
            case ENABLED -> enabled();
        };
    }

    public PackListKey root() {
        return root(this.type);
    }

    public PackListKey nest() {
        return new PackListKey(this.type, this.depth + 1);
    }

    public PackListKey unnest() {
        return new PackListKey(this.type, this.depth - 1);
    }
}