package io.github.fishstiz.packed_packs.util;

public final class Colors {
    public static final int RED_700 = 0xFFB00000;
    public static final int RED_900 = 0xFF770000;
    public static final int GREEN_500 = 0xFF22C55E;
    public static final int BLUE_500 = 0xFF3B82F6;
    public static final int YELLOW_500 = 0xFFEAB308;
    public static final int ORANGE_500 = 0xFFFC9C36;
    public static final int BROWN_500 = 0xFFB46C1E;
    public static final int PURPLE_500 = 0xFF8B5CF6;
    public static final int MAGENTA_500 = 0xFFEC4899;
    public static final int GRAY_500 = 0xFF808080;
    public static final int GRAY_800 = 0xFF3F3F3F;
    public static final int BLACK = 0xFF000000;
    public static final int WHITE = 0xFFFFFFFF;

    public static int alpha(int argb, float alpha) {
        int alphaInt = (int) (Math.clamp(alpha, 0.0f, 1.0f) * 255) & 0xFF;
        return (alphaInt << 24) | (argb & 0x00FFFFFF);
    }

    private Colors() {
    }
}
