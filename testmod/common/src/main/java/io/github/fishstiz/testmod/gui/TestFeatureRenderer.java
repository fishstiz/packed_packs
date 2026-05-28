package io.github.fishstiz.testmod.gui;

import io.github.fishstiz.testmod.TestFeature;
import net.minecraft.client.gui.GuiGraphics;

public abstract class TestFeatureRenderer {
    private final TestFeature feature;

    TestFeatureRenderer(TestFeature feature) {
        this.feature = feature;
    }

    protected abstract int renderFeature(GuiGraphics guiGraphics, int mouseX, int mouseY, int y, float partialTick);

    public final int render(GuiGraphics guiGraphics, int mouseX, int mouseY, int y, float partialTick) {
        if (feature.isEnabled()) {
            return renderFeature(guiGraphics, mouseX, mouseY, y, partialTick);
        }
        return y;
    }
}
