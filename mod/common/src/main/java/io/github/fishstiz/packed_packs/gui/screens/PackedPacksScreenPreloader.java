package io.github.fishstiz.packed_packs.gui.screens;

import io.github.fishstiz.fidgetz.v0.utils.GuiHooks;
import io.github.fishstiz.packed_packs.PackedPacks;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;

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

            if (PackedPacks.DEBUG) {
                start = System.nanoTime();
                PackedPacks.LOGGER.info("[packed_packs] ======== Preloading PackedPacksScreen ========");
            }

            PackedPacksScreen.preload(minecraft);

            if (PackedPacks.DEBUG) {
                PackedPacks.LOGGER.info("[packed_packs] ======== Preloaded PackedPacksScreen in {}ms ========", PackedPacks.duration(start));
            }
        }, Util.backgroundExecutor()).thenRunAsync(() -> {
            if (minecraft.screen == screen) {
                detach();
            }
        }, minecraft);

        preloaded = true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!preloaded && widget.isHoveredOrFocused()) {
            preload();
        }
    }
}
