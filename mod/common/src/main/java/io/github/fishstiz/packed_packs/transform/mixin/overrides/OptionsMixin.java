package io.github.fishstiz.packed_packs.transform.mixin.overrides;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.config.*;
import io.github.fishstiz.packed_packs.transform.interfaces.ConfiguredPack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.List;

import static io.github.fishstiz.packed_packs.config.DevConfig.ResourcePacks.LoadDefaultCondition.NO_OPTIONS_OR_VERSION_FILE;

@Mixin(Options.class)
public abstract class OptionsMixin {
    @Shadow
    @Final
    private File optionsFile;

    @Shadow
    public List<String> resourcePacks;

    @Unique
    private static boolean packed_packs$shouldCheckVersion() {
        return DevConfig.get().getResourcepacks().getLoadDefaultCondition() == NO_OPTIONS_OR_VERSION_FILE;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void applyDefaultProfile(Minecraft minecraft, File gameDirectory, CallbackInfo ci) {
        if (!this.optionsFile.exists() || (packed_packs$shouldCheckVersion() && VersionState.uninitialized())) {
            Profile defaultProfile = ProfileManager.clientResources().getDefault();
            if (defaultProfile != null) {
                PackedPacks.LOGGER.info(
                        "[packed_packs] Applying default resource pack profile due to load condition: {}",
                        DevConfig.get().getResourcepacks().getLoadDefaultCondition()
                );
                this.resourcePacks.clear();
                this.resourcePacks.addAll(defaultProfile.getPackIds().reversed());
            }
        }

        VersionState.update();
    }

    // fixed position packs are not saved to options
    @WrapOperation(method = "updateResourcePacks", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/packs/repository/Pack;isFixedPosition()Z"
    ))
    private boolean resolveFixed(Pack instance, Operation<Boolean> original) {
        ConfiguredPack configuredPack = (ConfiguredPack) instance;
        return configuredPack.packed_packs$isConfigured()
                ? configuredPack.packed_packs$originalConfig().fixedPosition()
                : original.call(instance);
    }
}
