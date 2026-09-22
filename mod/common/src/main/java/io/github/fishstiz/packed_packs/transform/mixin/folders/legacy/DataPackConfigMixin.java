package io.github.fishstiz.packed_packs.transform.mixin.folders.legacy;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.fishstiz.packed_packs.config.migrations.LegacyFoldersHelper;
import net.minecraft.world.level.DataPackConfig;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@SuppressWarnings({"unused", "UnusedMixin"})
@Mixin(DataPackConfig.class) // took me too long to realize this isn't actually a record
abstract class DataPackConfigMixin {
    @Shadow
    @Mutable
    @Final
    private List<String> enabled;

    @Shadow
    @Mutable
    @Final
    private List<String> disabled;

    @Unique
    private boolean packed_packs$disabledRemapped = false;

    @Unique
    private boolean packed_packs$enabledRemapped = false;

    // see MinecraftServer#configurePackRepository

    @ModifyReturnValue(method = "getDisabled", at = @At("RETURN"))
    private List<String> remapLegacyDisabledPackIds(List<String> original) {
        if (this.packed_packs$disabledRemapped) {
            return original;
        } else {
            List<String> remapped = LegacyFoldersHelper.remapLegacyDataPackIdsForLevel(original);
            this.disabled = remapped;
            this.packed_packs$disabledRemapped = true;
            return remapped;
        }
    }

    @ModifyReturnValue(method = "getEnabled", at = @At("RETURN"))
    private List<String> remapLegacyEnabledPackIds(List<String> original) {
        if (this.packed_packs$enabledRemapped) {
            return original;
        } else {
            List<String> remapped = LegacyFoldersHelper.remapLegacyDataPackIdsForLevel(original);
            this.enabled = remapped;
            this.packed_packs$enabledRemapped = true;
            return remapped;
        }
    }
}
