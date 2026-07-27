package io.github.fishstiz.fidgetz.transform.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.fishstiz.fidgetz.gui.components.ToggleableDialogContainer;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @WrapOperation(method = "keyPressed", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;shouldCloseOnEsc()Z"
    ))
    public boolean shouldCloseDialogs(Screen instance, Operation<Boolean> original, int keyCode) {
        if (this instanceof ToggleableDialogContainer dialogContainer && keyCode == KEY_ESCAPE) {
            for (var dialog : dialogContainer.getOpenDialogs()) {
                if (dialog.shouldCloseOnEscape()) {
                    dialog.setOpen(false);
                    return false;
                }
            }
        }
        return original.call(instance);
    }
}
