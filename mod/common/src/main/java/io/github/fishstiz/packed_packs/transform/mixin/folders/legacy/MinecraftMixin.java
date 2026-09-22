package io.github.fishstiz.packed_packs.transform.mixin.folders.legacy;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.fishstiz.packed_packs.config.VersionState;
import io.github.fishstiz.packed_packs.config.migrations.LegacyFolders;
import io.github.fishstiz.packed_packs.config.migrations.LegacyFoldersHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@SuppressWarnings({"unused", "UnusedMixin"})
@Mixin(Minecraft.class)
abstract class MinecraftMixin {
    @Shadow
    public abstract LevelStorageSource getLevelSource();

    @WrapOperation(method = "<init>", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Options;loadSelectedResourcePacks(Lnet/minecraft/server/packs/repository/PackRepository;)V",
            unsafe = true
    ))
    private void remapLegacyPacks(Options instance, PackRepository repository, Operation<Void> original) {
        if (VersionState.shouldMigrateLegacyFolders() && LegacyFolders.flagResourcePacksMigrated()) {
            LegacyFoldersHelper.remapLegacyResourcePackIdsForOptions(instance.resourcePacks);
            LegacyFoldersHelper.remapLegacyResourcePackIdsForOptions(instance.incompatibleResourcePacks);
            LegacyFolders.save();
        }

        original.call(instance, repository);
    }
}
