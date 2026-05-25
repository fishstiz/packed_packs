package io.github.fishstiz.packed_packs.compat.etf;

import io.github.fishstiz.fidgetz.v0.gui.components.FZIconButton;
import io.github.fishstiz.fidgetz.v0.gui.components.WidgetRenderables;
import io.github.fishstiz.fidgetz.v0.gui.renderables.RenderableRectangle;
import io.github.fishstiz.fidgetz.v0.gui.renderables.Renderables;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.api.PackedPacksApi;
import io.github.fishstiz.packed_packs.api.Preference;
import io.github.fishstiz.packed_packs.api.events.ContextMenuEvent;
import io.github.fishstiz.packed_packs.api.events.InitializeLayoutEvent;
import io.github.fishstiz.packed_packs.compat.ModIntegration;
import io.github.fishstiz.packed_packs.compat.Mod;
import io.github.fishstiz.packed_packs.compat.ModContext;
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
        Preference<Boolean> etfButton = api.preferences().register(PackedPacks.id("etf_button"), true);

        api.eventBus().register(InitializeLayoutEvent.class, this.id(), event -> {
            if (!event.screenContext().isClientResources()) return;
            var button = event.screenContext().wrapWidget(etfButton, getWidgetPrefText(etfButton), this.createButton(event.screenContext().screen()));
            if (button != null) event.addWidget(InitializeLayoutEvent.Pos.AFTER_TITLE, button);
        });

        api.eventBus().register(ContextMenuEvent.Preferences.class, this.id(), event -> {
            if (event.screenContext().isClientResources()) event.addToggle(etfButton, getWidgetPrefText(etfButton));
        });
    }

    private Button createButton(Screen parent) {
        WidgetRenderables sprites = new WidgetRenderables(getSprite("settings_unfocused.png"), getSprite("settings_focused.png"));
        return FZIconButton.builder(sprites)
                .size(24, 20)
                .onPress(createScreenSetter("traben.entity_texture_features.config.screens.ETFConfigScreenMain", ScreenArg.parent(parent)))
                .build();
    }

    private RenderableRectangle getSprite(String path) {
        return Renderables.texture(Identifier.fromNamespaceAndPath("entity_features", "textures/gui/" + path), 24, 20);
    }
}
