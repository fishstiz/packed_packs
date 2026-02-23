package io.github.fishstiz.packed_packs.compat.etf;

import io.github.fishstiz.fidgetz.gui.components.SpriteButton;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.ButtonSprites;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.packed_packs.api.PackedPacksApi;
import io.github.fishstiz.packed_packs.api.PreferenceRegistry;
import io.github.fishstiz.packed_packs.api.events.ContextMenuEvent;
import io.github.fishstiz.packed_packs.api.events.InitializeLayoutEvent;
import io.github.fishstiz.packed_packs.compat.ModIntegration;
import io.github.fishstiz.packed_packs.compat.Mod;
import io.github.fishstiz.packed_packs.compat.ModContext;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

public class ETFIntegration extends ModIntegration {
    @Override
    public ModContext mod() {
        return Mod.ETF;
    }

    @Override
    public void onInitLoaded(PackedPacksApi api) {
        PreferenceRegistry.Key<Boolean> prefKey = api.preferences().register(ResourceUtil.id("etf_button"), true);

        api.eventBus().register(InitializeLayoutEvent.class, this.id(), event -> {
            if (!event.screenContext().isClientResources()) return;
            var button = event.screenContext().bindPreference(prefKey, this.createButton(event.screenContext().screen()));
            if (button != null) event.addWidget(InitializeLayoutEvent.Pos.AFTER_TITLE, button);
        });

        api.eventBus().register(ContextMenuEvent.Preferences.class, this.id(), event -> {
            if (event.screenContext().isClientResources()) event.addToggle(prefKey, getWidgetPrefText(prefKey));
        });
    }

    private Button createButton(Screen parent) {
        ButtonSprites sprites = new ButtonSprites(this.getSprite("settings_focused.png"), this.getSprite("settings_unfocused.png"));
        return SpriteButton.builder(SpriteButton.Sprites.of(sprites))
                .setDimensions(24, 20)
                .setOnPress(createScreenSetter("traben.entity_texture_features.config.screens.ETFConfigScreenMain", ScreenArg.parent(parent)))
                .build();
    }

    private Sprite getSprite(String path) {
        return new Sprite(Identifier.fromNamespaceAndPath("entity_features", "textures/gui/" + path), 24, 20);
    }
}
