package io.github.fishstiz.packed_packs.transform.mixin.gui;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksScreen;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksScreenPreloader;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {
    @Shadow
    @Final
    private static Component RESOURCEPACK;

    protected OptionsScreenMixin(Component title) {
        super(title);
    }

    @WrapWithCondition(method = "applyPacks", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"
    ))
    public boolean shouldCloseOnApplyPacks(Gui instance, Screen screen) {
        // apply packs without closing the screen
        return !(instance.screen() instanceof PackedPacksScreen);
    }

    @ModifyReturnValue(method = "openScreenButton", at = @At("RETURN"))
    private Button preloadPackedPacksScreen(Button original, Component message) {
        if (message == RESOURCEPACK) {
            PackedPacksScreenPreloader.attach(this, original);
        }
        return original;
    }
}
