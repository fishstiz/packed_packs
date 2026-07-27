package io.github.fishstiz.packed_packs.compat.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.fishstiz.packed_packs.gui.screens.OptionsScreen;

public class PackedPacksModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<OptionsScreen> getModConfigScreenFactory() {
        return OptionsScreen::new;
    }
}
