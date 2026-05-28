package io.github.fishstiz.testmod.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.fishstiz.testmod.pack.TestPackRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @WrapOperation(method = "<init>", at = @At(
            value = "NEW",
            target = "([Lnet/minecraft/server/packs/repository/RepositorySource;)Lnet/minecraft/server/packs/repository/PackRepository;")
    )
    private PackRepository addTestPacks(RepositorySource[] sources, Operation<PackRepository> original) {
        return original.call((Object) ArrayUtils.add(sources, TestPackRegistry.asRepositorySource()));
    }
}
