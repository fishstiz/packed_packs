package io.github.fishstiz.packed_packs.transform.mixin.compat.polytone;

import io.github.fishstiz.packed_packs.gui.screens.PackedPacksScreen;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionScreenAccessor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(OptionsSubScreen.class)
public abstract class OptionsSubScreenMixin {
    // polytone's screen factory only accepts PackSelectionScreen as parent.
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static Screen replacePackScreen(Screen parent) {
        if (parent instanceof PackSelectionScreenAccessor packScreen) {
            Screen screen = packScreen.packed_packs$getActualScreen();
            if (screen instanceof PackedPacksScreen packedPacksScreen) {
                return packedPacksScreen;
            }
        }
        return parent;
    }
}
