package io.github.fishstiz.packed_packs.transform.mixin.overrides;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.pack.PackOptionsResolver;
import io.github.fishstiz.packed_packs.transform.interfaces.ConfiguredPack;
import net.minecraft.server.packs.PackSelectionConfig;
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

    @Unique
    @Nullable
    private PackOptionsResolver packed_packs$resolver;

    @Override
    public void packed_packs$setConfigurationResolver(PackOptionsResolver resolver) {
        this.packed_packs$resolver = resolver;
    }

    @Override
    public boolean packed_packs$isHidden() {
        return this.packed_packs$resolver != null && this.packed_packs$resolver.isHidden(packed_packs$self());
    }

    @Unique
    private Pack packed_packs$self() {
        return (Pack) (Object) this;
    }

    @WrapMethod(method = "isRequired")
    private boolean resolveRequired(Operation<Boolean> original) {
        PackOptionsResolver resolver = this.packed_packs$resolver;
        if (resolver != null && resolver.overridesRequired(packed_packs$self())) {
            return resolver.isRequired(packed_packs$self());
        }
        return original.call();
    }

    @WrapMethod(method = "isFixedPosition")
    private boolean resolveFixed(Operation<Boolean> original) {
        PackOptionsResolver resolver = this.packed_packs$resolver;
        if (resolver != null && resolver.overridesPosition(packed_packs$self())) {
            return resolver.isFixed(packed_packs$self());
        }
        return original.call();
    }

    @WrapMethod(method = "getDefaultPosition")
    private Pack.Position resolvePosition(Operation<Pack.Position> original) {
        if (this.packed_packs$resolver != null) {
            Pack.Position position = this.packed_packs$resolver.getPosition(packed_packs$self());
            if (position != null) {
                return position;
            }
        }
        return original.call();
    }

    @WrapMethod(method = "selectionConfig")
    private PackSelectionConfig resolveSelectionConfig(Operation<PackSelectionConfig> original) {
        if (this.packed_packs$resolver != null) {
            PackSelectionConfig selectionConfig = this.packed_packs$resolver.getSelectionConfig(packed_packs$self());
            if (selectionConfig != null) {
                return selectionConfig;
            }
        }
        return original.call();
    }

    @WrapMethod(method = "getCompatibility")
    private PackCompatibility resolveCompatibility(Operation<PackCompatibility> original) {
        if (this.packed_packs$resolver != null) {
            Profile defaultProfile = this.packed_packs$resolver.defaultProfileSupplier().get();
            if (defaultProfile != null && defaultProfile.includes(packed_packs$self())) {
                return PackCompatibility.COMPATIBLE;
            }
        }
        return original.call();
    }

    @Override
    public boolean packed_packs$isConfigured() {
        if (this.packed_packs$resolver != null) {
            Profile defaultProfile = this.packed_packs$resolver.defaultProfileSupplier().get();
            return defaultProfile != null && defaultProfile.includes(packed_packs$self());
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
