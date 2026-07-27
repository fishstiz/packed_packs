package io.github.fishstiz.packed_packs.transform.mixin.overrides;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.fishstiz.packed_packs.transform.interfaces.ConfiguredPack;
import io.github.fishstiz.packed_packs.transform.interfaces.FilterableModel;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.stream.Stream;

@Mixin(PackSelectionModel.class)
public abstract class PackSelectionModelMixin implements FilterableModel {
    @Unique
    private boolean packed_packs$filterHidden = true;

    @Override
    public void packed_packs$filterHidden(boolean filter) {
        this.packed_packs$filterHidden = filter;
    }

    @ModifyExpressionValue(method = {"getSelected", "getUnselected"}, at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;stream()Ljava/util/stream/Stream;",
            remap = false
    ))
    private Stream<Pack> filterHiddenPacks(Stream<Pack> original) {
        return this.packed_packs$filterHidden
                ? original.filter(pack -> !((ConfiguredPack) pack).packed_packs$isHidden())
                : original;
    }
}
