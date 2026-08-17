package io.github.fishstiz.packed_packs.transform.mixin.gui;

import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.gui.screens.PackSelectionScreenArgs;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksScreen;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.concurrent.Executor;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin implements Executor {
    @Shadow
    @Nullable
    public Screen screen;

    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen replacePackScreen(Screen original) {
        if (original instanceof PackSelectionScreen packScreen &&
            (((PackSelectionScreenAccessor) packScreen).packed_packs$getPrevious() == null) &&
            !(this.screen instanceof PackedPacksScreen)) {

            PackSelectionScreenArgs args = PackSelectionScreenArgs.extract(packScreen);

            if (Config.packs(args.packType()).isReplaceOriginal()) {
                ((PackSelectionScreenAccessor) packScreen).packed_packs$closeWatcher();
                return new PackedPacksScreen(this.screen, args);
            }
        }

        return original;
    }
}
