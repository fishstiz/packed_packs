package io.github.fishstiz.packed_packs.transform.mixin.aliases;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.fishstiz.packed_packs.transform.interfaces.MappedPackRepository;
import net.minecraft.client.Options;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

@Mixin(Options.class)
public class OptionsMixin {
    @WrapOperation(method = "loadSelectedResourcePacks", at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;iterator()Ljava/util/Iterator;",
            remap = false
    ))
    private Iterator<String> replaceIterator(List<String> instance, Operation<Iterator<String>> original, PackRepository packRepository) {
        if (((MappedPackRepository) packRepository).packed_packs$hasAlias()) {
            return instance.listIterator();
        }
        return original.call(instance);
    }

    @ModifyExpressionValue(method = "loadSelectedResourcePacks", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/packs/repository/PackRepository;getPack(Ljava/lang/String;)Lnet/minecraft/server/packs/repository/Pack;"
    ))
    private Pack remapPack(Pack original, @Local(ordinal = 0) Iterator<String> iterator) {
        if (original != null && iterator instanceof ListIterator<String> listIterator) {
            listIterator.set(original.getId());
        }

        return original;
    }
}
