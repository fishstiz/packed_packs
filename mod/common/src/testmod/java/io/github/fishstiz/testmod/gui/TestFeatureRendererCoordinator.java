package io.github.fishstiz.testmod.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TestFeatureRendererCoordinator implements Renderable {
    private final List<TestFeatureRenderer> renderers = new ArrayList<>();

    public TestFeatureRendererCoordinator add(TestFeatureRenderer renderer) {
        renderers.add(renderer);
        return this;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int y = 0;

        for (TestFeatureRenderer renderer : renderers) {
            y = renderer.render(guiGraphics, mouseX, mouseY, y, partialTick);
        }
    }
}
