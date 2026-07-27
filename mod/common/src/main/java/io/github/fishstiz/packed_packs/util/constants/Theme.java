package io.github.fishstiz.packed_packs.util.constants;

import io.github.fishstiz.fidgetz.util.ARGBColor;

public enum Theme implements ARGBColor {
    RED_700(0xFFB00000),
    RED_900(0xFF770000),
    GREEN_500(0xFF22C55E),
    BLUE_500(0xFF3B82F6),
    YELLOW_500(0xFFEAB308),
    ORANGE_500(0xFFFC9C36),
    BROWN_500(0xFFB46C1E),
    PURPLE_500(0xFF8B5CF6),
    MAGENTA_500(0xFFEC4899),
    GRAY_500(0xFF808080),
    GRAY_800(0xFF3F3F3F),
    BLACK(0xFF000000),
    WHITE(0xFFFFFFFF);

    private final int argb;

    Theme(int argb) {
        this.argb = argb;
    }

    @Override
    public int getARGB() {
        return this.argb;
    }
}
