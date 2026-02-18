package io.github.fishstiz.packed_packs.compat.vtdownloader;

import io.github.fishstiz.fidgetz.gui.components.FidgetzButton;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.packed_packs.api.PreferenceRegistry;
import io.github.fishstiz.packed_packs.compat.ModScreenFactory;
import io.github.fishstiz.packed_packs.config.Preferences;
import io.github.fishstiz.packed_packs.gui.components.ToggleableHelper;
import io.github.fishstiz.packed_packs.impl.PackedPacksApiImpl;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class VTDButtonFactory {
    static final String VTD_SCREEN_NAME = "me.bymartrixx.vtd.gui.VTDownloadScreen";
    static final Sprite VTD_ICON = Sprite.of32(Identifier.fromNamespaceAndPath("vt_downloader", "icon.png"));
    static final Component VTD_MESSAGE = Component.translatable("vtd.resourcePack.button");
    static final Component VTD_SUBTITLE = Component.translatable("vtd.resourcePack.subtitle");

    private VTDButtonFactory() {
    }

    public static FidgetzButton<Void> create(PreferenceRegistry.Key<Boolean> prefKey, Screen previous) {
        return ToggleableHelper.applyPref(
                        Preferences.INSTANCE.getOrThrow(PackedPacksApiImpl.getInstance().preferences().getSpec(prefKey)),
                        FidgetzButton.<Void>builder()
                )
                .makeSquare()
                .setTooltip(Tooltip.create(VTD_MESSAGE))
                .setSprite(VTD_ICON)
                .setFocusedBorder(Theme.WHITE.getARGB())
                .setOnPress(ModScreenFactory.createScreenSetter(
                        VTD_SCREEN_NAME,
                        new ModScreenFactory.Arg<>(Screen.class, previous),
                        new ModScreenFactory.Arg<>(Component.class, VTD_SUBTITLE)
                ))
                .build();
    }
}

