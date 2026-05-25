package io.github.fishstiz.packed_packs.gui.screens;

import io.github.fishstiz.fidgetz.v0.utils.GuiHooks;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Util;

import java.util.concurrent.CompletableFuture;

public class PackedPacksScreenPreloader implements Renderable {
    private static boolean preloaded;
    private final Minecraft minecraft;
    private final Screen screen;
    private final AbstractWidget widget;

    private PackedPacksScreenPreloader(Minecraft minecraft, Screen screen, AbstractWidget widget) {
        this.minecraft = minecraft;
        this.screen = screen;
        this.widget = widget;
    }

    public static void attach(Screen screen, AbstractWidget widget) {
        if (!preloaded) {
            GuiHooks.modifyRenderables(screen, renderables -> {
                renderables.add(new PackedPacksScreenPreloader(Minecraft.getInstance(), screen, widget));
                return renderables;
            });
        }
    }

    private void detach() {
        GuiHooks.modifyRenderables(screen, renderables -> {
            renderables.remove(this);
            return renderables;
        });
    }

    private void preload() {
        if (preloaded) return;

        CompletableFuture.runAsync(() -> {
            long start = 0;
            boolean debug = System.getProperty("packed_packs.debug") != null;

            if (Services.PLATFORM.isDev() || debug) {
                start = System.nanoTime();
                PackedPacks.LOGGER.info("[packed_packs] ======== Preloading PackedPacksScreen ========");
            }

            PackedPacksScreen.preload(minecraft);

            if (Services.PLATFORM.isDev() || debug) {
                long duration = (System.nanoTime() - start) / 1_000_000;
                PackedPacks.LOGGER.info("[packed_packs] ======== Preloaded PackedPacksScreen in {}ms ========", duration);
            }
        }, Util.backgroundExecutor()).thenRunAsync(() -> {
            if (minecraft.screen == screen) {
                detach();
            }
        }, minecraft);

        preloaded = true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (!preloaded && widget.isHoveredOrFocused()) {
            preload();
        }
    }
}
