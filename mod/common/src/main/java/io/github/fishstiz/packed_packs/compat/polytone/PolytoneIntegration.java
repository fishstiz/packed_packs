package io.github.fishstiz.packed_packs.compat.polytone;

import io.github.fishstiz.fidgetz.gui.components.SpriteButton;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.GuiSprite;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.api.PackedPacksApi;
import io.github.fishstiz.packed_packs.api.PreferenceRegistry;
import io.github.fishstiz.packed_packs.api.events.ScreenContext;
import io.github.fishstiz.packed_packs.api.events.ScreenEvent;
import io.github.fishstiz.packed_packs.compat.Mod;
import io.github.fishstiz.packed_packs.compat.ModContext;
import io.github.fishstiz.packed_packs.compat.ModIntegration;
import io.github.fishstiz.packed_packs.gui.components.ToggleableHelper;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionScreenAccessor;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import org.jspecify.annotations.NonNull;

public class PolytoneIntegration implements ModIntegration {
    @Override
    public ModContext mod() {
        return Mod.POLYTONE;
    }

    @Override
    public void onInitialize(@NonNull PackedPacksApi api) {
        if (!this.mod().isLoaded()) return;

        PreferenceRegistry.Key<Boolean> prefKey = api.preferences().register(ResourceUtil.id("polytone_button"), true);

        api.eventBus().register(ScreenEvent.InitLayout.class, this.id(), event -> {
            if (event.ctx().getPackType() != PackType.CLIENT_RESOURCES) return;

            if (event.ctx().isDevMode() || Boolean.TRUE.equals(api.preferences().get(prefKey))) {
                event.addElement(ScreenEvent.InitLayout.Phase.AFTER_HEADER_TITLE, ButtonFactory.create(prefKey, event.ctx()));
            }
        });

        api.eventBus().register(ScreenEvent.OpenCtxMenu.class, this.id(), event -> {
            if (event.ctx().getPackType() != PackType.CLIENT_RESOURCES) return;

            event.addPreferenceToggle(api.preferences(), prefKey, ModIntegration.getWidgetPrefText(prefKey));
        });
    }

    static final class ButtonFactory {
        private ButtonFactory() {
        }

        static Button create(PreferenceRegistry.Key<Boolean> prefKey, ScreenContext screenContext) {
            return ToggleableHelper.applyPref(prefKey, SpriteButton.builder())
                    .makeSquare()
                    .setSprite(new GuiSprite(Identifier.fromNamespaceAndPath("polytone", "paint_brush"), 16, 16))
                    .setOnPress(() -> {
                        try {
                            PackSelectionScreen original = screenContext.getOriginalScreen();
                            ((PackSelectionScreenAccessor) original).packed_packs$setActualScreen(screenContext.getScreen());
                            Minecraft.getInstance().setScreen(Polytone.CONFIGS.createScreenForPack(original));
                        } catch (Throwable e) {
                            PackedPacks.LOGGER.error("[packed_packs] Failed to open Polytone config screen ", e);
                        }
                    })
                    .build();
        }
    }
}
