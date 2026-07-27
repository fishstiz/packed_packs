package io.github.fishstiz.packed_packs.transform.mixin.gui;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {
    protected OptionsScreenMixin(Component title) {
        super(title);
    }

    @WrapWithCondition(method = "applyPacks", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"
    ))
    public boolean shouldCloseOnApplyPacks(Minecraft instance, Screen guiScreen) {
        return this.minecraft == null || !(this.minecraft.screen instanceof PackedPacksScreen);
    }
}
