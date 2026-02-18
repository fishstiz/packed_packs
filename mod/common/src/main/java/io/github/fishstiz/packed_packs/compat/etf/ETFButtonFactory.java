package io.github.fishstiz.packed_packs.compat.etf;

import io.github.fishstiz.fidgetz.gui.components.SpriteButton;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.ButtonSprites;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.gui.shapes.Size;
import io.github.fishstiz.packed_packs.api.PreferenceRegistry;
import io.github.fishstiz.packed_packs.compat.Mod;
import io.github.fishstiz.packed_packs.compat.ModScreenFactory;
import io.github.fishstiz.packed_packs.config.Preferences;
import io.github.fishstiz.packed_packs.gui.components.ToggleableHelper;
import io.github.fishstiz.packed_packs.impl.PackedPacksApiImpl;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

public class ETFButtonFactory {
    private static final String NAMESPACE = "entity_features";
    private static final String ETF_SCREEN_NAME = "traben.entity_texture_features.config.screens.ETFConfigScreenMain";
    private static final Size SIZE = new Size(24, 20);
    private static final Sprite UNFOCUSED = new Sprite(getEtfIcon("settings_unfocused.png"), SIZE);
    private static final Sprite FOCUSED = new Sprite(getEtfIcon("settings_focused.png"), SIZE);

    private ETFButtonFactory() {
    }

    public static SpriteButton<Void> create(PreferenceRegistry.Key<Boolean> prefKey, Screen previous) {
        return ToggleableHelper.applyPref(
                        Preferences.INSTANCE.getOrThrow(PackedPacksApiImpl.getInstance().preferences().getSpec(prefKey)),
                        SpriteButton.<Void>builder(SpriteButton.Sprites.of(new ButtonSprites(FOCUSED, UNFOCUSED)))
                )
                .setMessage(Mod.ETF.getId())
                .setDimensions(SIZE.width(), SIZE.height())
                .setOnPress(ModScreenFactory.createScreenSetter(
                        ETF_SCREEN_NAME,
                        new ModScreenFactory.Arg<>(Screen.class, previous)
                ))
                .build();
    }

    private static Identifier getEtfIcon(String icon) {
        return Identifier.fromNamespaceAndPath(NAMESPACE, "textures/gui/").withSuffix(icon);
    }
}
