package io.github.fishstiz.packed_packs.transform.mixin.compat.vtd;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.fishstiz.packed_packs.compat.PackWrapperDelegatorAbstractionEpicModelEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Slice;

@Pseudo
@Mixin(targets = "me.bymartrixx.vtd.gui.VTDownloadScreen", remap = false)
public abstract class VTDownloaderScreenMixin extends Screen {
    protected VTDownloaderScreenMixin(Component title) {
        super(title);
    }

    @Dynamic
    @Shadow(aliases = "pack")
    private PackSelectionModel.Entry packEntry;

    @Dynamic
    @WrapOperation(method = "readResourcePack", at = @At(value = "INVOKE", target = "Ljava/lang/Class;isNestmateOf(Ljava/lang/Class;)Z"))
    private boolean hasPack(Class<?> instance, Class<?> clazz, Operation<Boolean> original) {
        return original.call(instance, clazz) || this.packEntry instanceof PackWrapperDelegatorAbstractionEpicModelEntry;
    }

    @Dynamic
    @WrapOperation(
            method = "readResourcePack",
            slice = @Slice(from = @At(value = "INVOKE", target = "Ljava/lang/Class;isNestmateOf(Ljava/lang/Class;)Z")),
            at = @At(
                    value = "FIELD:FIRST",
                    target = "Lme/bymartrixx/vtd/gui/VTDownloadScreen;pack:Lnet/minecraft/client/gui/screens/packs/PackSelectionModel$Entry;",
                    ordinal = 0,
                    remap = true
            )
    )
    private PackSelectionModel.Entry getPackEntry(@Coerce Screen instance, Operation<PackSelectionModel.Entry> original) {
        return this.packEntry instanceof PackWrapperDelegatorAbstractionEpicModelEntry ? null : original.call(instance);
    }

    @Dynamic
    @WrapOperation(method = "readResourcePack", at = @At(
            value = "INVOKE",
            target = "Lme/bymartrixx/vtd/access/AbstractPackAccess;vtdownloader$getProfile()Lnet/minecraft/server/packs/repository/Pack;",
            remap = true
    ))
    private Pack getPack(@Coerce Object instance, Operation<Pack> original) {
        return this.packEntry instanceof PackWrapperDelegatorAbstractionEpicModelEntry(Pack pack) ? pack : original.call(instance);
    }
}
