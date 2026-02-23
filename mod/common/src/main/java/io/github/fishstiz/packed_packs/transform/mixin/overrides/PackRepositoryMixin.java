package io.github.fishstiz.packed_packs.transform.mixin.overrides;

import com.google.common.collect.ImmutableMap;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.github.fishstiz.packed_packs.config.DevConfig;
import io.github.fishstiz.packed_packs.pack.PackAliasMap;
import io.github.fishstiz.packed_packs.pack.PackOptionsResolver;
import io.github.fishstiz.packed_packs.transform.interfaces.ConfiguredPack;
import io.github.fishstiz.packed_packs.transform.interfaces.MappedPackRepository;
import net.minecraft.client.resources.ClientPackSource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.repository.ServerPacksSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.Consumer;

@Mixin(value = PackRepository.class, priority = 5000)
public abstract class PackRepositoryMixin implements MappedPackRepository {
    @Unique
    private PackOptionsResolver packed_packs$resolver;

    @Unique
    private DevConfig.Packs packed_packs$config;

    @Unique
    private boolean packed_packs$hasAlias = false;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void setConfigOnInit(RepositorySource[] sources, CallbackInfo ci) {
        if (sources.length > 0) {
            if (sources[0] instanceof ServerPacksSource) {
                this.packed_packs$resolver = PackOptionsResolver.DATA_PACKS;
                this.packed_packs$config = DevConfig.get().getDatapacks();
            } else if (sources[0] instanceof ClientPackSource) {
                this.packed_packs$resolver = PackOptionsResolver.RESOURCE_PACKS;
                this.packed_packs$config = DevConfig.get().getResourcepacks();
            }
        }
    }

    @ModifyArg(method = "discoverAvailable", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/packs/repository/RepositorySource;loadPacks(Ljava/util/function/Consumer;)V"
    ))
    private Consumer<Pack> applyResolverOnLoad(Consumer<Pack> onLoad) {
        if (this.packed_packs$resolver == null) {
            return onLoad;
        }

        return pack -> {
            ((ConfiguredPack) pack).packed_packs$setConfigurationResolver(this.packed_packs$resolver);
            onLoad.accept(pack);
        };
    }

    @ModifyArg(method = "discoverAvailable", at = @At(
            value = "INVOKE",
            target = "Lcom/google/common/collect/ImmutableMap;copyOf(Ljava/util/Map;)Lcom/google/common/collect/ImmutableMap;",
            remap = false
    ))
    private Map<String, Pack> captureMutableMap(Map<String, Pack> map, @Share("mutableMap") LocalRef<Map<String, Pack>> mutableMapRef) {
        if (!(map instanceof ImmutableMap<String, Pack>)) {
            // map needs to be mutable to preserve behavior, as it's a TreeMap in vanilla & fabric, and a LinkedHashMap in neoforge
            // if another mod somehow changes the underlying map to be immutable, without using guava's immutable map,
            // it will crash the game when resolving aliases of packs
            mutableMapRef.set(map);
        }
        return map;
    }

    @WrapMethod(method = "discoverAvailable")
    private Map<String, Pack> createAliasMap(Operation<Map<String, Pack>> original, @Share("mutableMap") LocalRef<Map<String, Pack>> mutableMapRef) {
        Map<String, Pack> immutableMap = original.call();
        Map<String, Pack> mutableMap = mutableMapRef.get();

        if (this.packed_packs$config == null || !this.packed_packs$config.hasAliases() || mutableMap == null) {
            this.packed_packs$hasAlias = false;
            return immutableMap;
        }

        this.packed_packs$hasAlias = true;
        return new PackAliasMap(this.packed_packs$config, mutableMap);
    }

    @Override
    public boolean packed_packs$hasAlias() {
        return this.packed_packs$hasAlias;
    }
}
