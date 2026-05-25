package io.github.fishstiz.packed_packs.transform.mixin.gui;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksScreenPreloader;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$MoreTab")
public abstract class CreateWorldScreenMoreTabMixin {
    @Final
    @Shadow(aliases = "this$0")
    CreateWorldScreen this$0;

    @Shadow
    @Final
    private static Component DATA_PACKS_LABEL;

    @ModifyExpressionValue(method = "<init>", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/Button$Builder;build()Lnet/minecraft/client/gui/components/Button;"
    ))
    private Button preloadPackedPacksScreen(Button original) {
        if (original.getMessage() == DATA_PACKS_LABEL) {
            PackedPacksScreenPreloader.attach(this$0, original);
        }
        return original;
    }
}
