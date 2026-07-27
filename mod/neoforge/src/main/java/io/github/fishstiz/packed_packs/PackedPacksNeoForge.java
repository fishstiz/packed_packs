package io.github.fishstiz.packed_packs;

import io.github.fishstiz.packed_packs.gui.screens.OptionsScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = PackedPacks.MOD_ID, dist = Dist.CLIENT)
public class PackedPacksNeoForge {
    public PackedPacksNeoForge(ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ((container, modListScreen) ->
                new OptionsScreen(modListScreen)
        ));
    }
}
