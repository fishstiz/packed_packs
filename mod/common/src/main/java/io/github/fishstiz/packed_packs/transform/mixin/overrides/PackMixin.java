package io.github.fishstiz.packed_packs.transform.mixin.overrides;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.config.ProfileManager;
import io.github.fishstiz.packed_packs.transform.interfaces.ConfiguredPack;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Pack.class)
public abstract class PackMixin implements ConfiguredPack {
    @Shadow
    @Final
    private Pack.Metadata metadata;

    @Shadow
    @Final
    private PackSelectionConfig selectionConfig;

    @Shadow
    public abstract String getId();

    @Unique
    @Nullable
    private PackType packed_packs$packType;

    @Override
    public void packed_packs$setPackType(PackType packType) {
        this.packed_packs$packType = packType;
    }

    @Unique
    private @Nullable Profile packed_packs$getDefaultProfile() {
        return this.packed_packs$packType == null ? null : ProfileManager.get(this.packed_packs$packType).getDefault();
    }

    @Override
    public boolean packed_packs$isHidden() {
        Profile profile = packed_packs$getDefaultProfile();
        return profile != null && profile.isHidden(getId());
    }

    @WrapMethod(method = "isRequired")
    private boolean resolveRequired(Operation<Boolean> original) {
        Profile profile = packed_packs$getDefaultProfile();
        if (profile != null && profile.overridesRequired(getId())) {
            return profile.isRequired(getId());
        }
        return original.call();
    }

    @WrapMethod(method = "isFixedPosition")
    private boolean resolveFixed(Operation<Boolean> original) {
        Profile profile = packed_packs$getDefaultProfile();
        if (profile != null && profile.overridesPosition(getId())) {
            return profile.isFixed(getId());
        }
        return original.call();
    }

    @WrapMethod(method = "getDefaultPosition")
    private Pack.Position resolvePosition(Operation<Pack.Position> original) {
        Profile profile = packed_packs$getDefaultProfile();
        if (profile != null) {
            Pack.Position position = profile.getPosition(getId());
            if (position != null) {
                return position;
            }
        }
        return original.call();
    }

    @WrapMethod(method = "selectionConfig")
    private PackSelectionConfig resolveSelectionConfig(Operation<PackSelectionConfig> original) {
        Profile profile = packed_packs$getDefaultProfile();
        if (profile != null) {
            PackSelectionConfig selectionConfig = profile.getSelectionConfig(getId());
            if (selectionConfig != null) {
                return selectionConfig;
            }
        }
        return original.call();
    }

    @WrapMethod(method = "getCompatibility")
    private PackCompatibility resolveCompatibility(Operation<PackCompatibility> original) {
        Profile profile = packed_packs$getDefaultProfile();
        if (profile != null && profile.includes(getId())) {
            return PackCompatibility.COMPATIBLE;
        }
        return original.call();
    }

    @Override
    public boolean packed_packs$isConfigured() {
        Profile profile = packed_packs$getDefaultProfile();
        if (profile != null) {
            return profile.includes(getId());
        }
        return false;
    }

    @Override
    public Pack.Metadata packed_packs$getMetadata() {
        return this.metadata;
    }

    @Override
    public PackSelectionConfig packed_packs$originalConfig() {
        return this.selectionConfig;
    }
}
