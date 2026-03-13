package io.github.fishstiz.packed_packs.compat.polytone;

import io.github.fishstiz.fidgetz.gui.components.SpriteButton;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.GuiSprite;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.api.PackedPacksApi;
import io.github.fishstiz.packed_packs.api.Preference;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.api.events.ContextMenuEvent;
import io.github.fishstiz.packed_packs.api.events.InitializeLayoutEvent;
import io.github.fishstiz.packed_packs.compat.Mod;
import io.github.fishstiz.packed_packs.compat.ModContext;
import io.github.fishstiz.packed_packs.compat.ModIntegration;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionScreenAccessor;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.resources.Identifier;

public class PolytoneIntegration extends ModIntegration {
    @Override
    public ModContext mod() {
        return Mod.POLYTONE;
    }

    @Override
    public void onInitLoaded(PackedPacksApi api) {
        Preference<Boolean> buttonEnabled = api.preferences().register(ResourceUtil.id("polytone_button"), true);

        api.eventBus().register(InitializeLayoutEvent.class, this.id(), event -> {
            if (!event.screenContext().isClientResources()) return;
            var button = event.screenContext().wrapWidget(buttonEnabled, getWidgetPrefText(buttonEnabled), ButtonFactory.create(event.screenContext()));
            if (button != null) event.addWidget(InitializeLayoutEvent.Pos.AFTER_TITLE, button);
        });

        api.eventBus().register(ContextMenuEvent.Preferences.class, this.id(), event -> {
            if (event.screenContext().isClientResources()) event.addToggle(buttonEnabled, getWidgetPrefText(buttonEnabled));
        });
    }

    static final class ButtonFactory {
        private ButtonFactory() {
        }

        static Button create(ScreenContext screenContext) {
            return SpriteButton.builder()
                    .makeSquare()
                    .setSprite(new GuiSprite(Identifier.fromNamespaceAndPath("polytone", "paint_brush"), 16, 16))
                    .setOnPress(() -> {
                        try {
                            PackSelectionScreen original = screenContext.originalScreen();
                            ((PackSelectionScreenAccessor) original).packed_packs$setActualScreen(screenContext.screen());
                            Minecraft.getInstance().setScreen(Polytone.CONFIGS.createScreenForPack(original));
                        } catch (Throwable e) {
                            PackedPacks.LOGGER.error("[packed_packs] Failed to open Polytone config screen ", e);
                        }
                    })
                    .build();
        }
    }
}
