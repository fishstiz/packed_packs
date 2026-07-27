package io.github.fishstiz.fidgetz.util;

public interface ARGBColor {
    int WHITE = 0xFFFFFFFF;

    int getARGB();

    default int withAlpha(float alpha) {
        return withAlpha(this.getARGB(), alpha);
    }

    static int withAlpha(int argb, float alpha) {
        if (alpha < 0.0f || alpha > 1.0f) {
            LogUtil.LOGGER.warn("Alpha must be between 0.0 and 1.0");
            alpha = Math.clamp(alpha, 0.0f, 1.0f);
        }

        int alphaInt = (int) (alpha * 255) & 0xFF;
        return (alphaInt << 24) | (argb & 0x00FFFFFF);
    }
}
