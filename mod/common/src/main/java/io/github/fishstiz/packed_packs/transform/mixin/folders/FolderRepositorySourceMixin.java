package io.github.fishstiz.packed_packs.transform.mixin.folders;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.pack.folder.FolderResources;
import io.github.fishstiz.packed_packs.transform.interfaces.FilePack;
import io.github.fishstiz.packed_packs.util.PackUtil;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackDetector;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Mixin(FolderRepositorySource.class)
public abstract class FolderRepositorySourceMixin {
    @Unique
    private static final ThreadLocal<Boolean> IS_SUBDIRECTORY = ThreadLocal.withInitial(() -> false);

    @Inject(method = "loadPacks", at = @At("RETURN"))
    private void ensureRemoveThreadLocals(Consumer<Pack> consumer, CallbackInfo ci) {
        IS_SUBDIRECTORY.remove();
    }

    @WrapOperation(method = "discoverPacks", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/packs/repository/FolderRepositorySource$FolderPackDetector;detectPackResources(Ljava/nio/file/Path;Ljava/util/List;)Ljava/lang/Object;"
    ))
    private static Object discoverNestedPacks(
            @Coerce PackDetector<Pack.ResourcesSupplier> instance,
            Path path,
            List<ForbiddenSymlinkInfo> list,
            Operation<Object> original,
            @Local(argsOnly = true) DirectoryValidator validator,
            @Local(argsOnly = true) BiConsumer<Path, Pack.ResourcesSupplier> output,
            @Share("suppressLog") LocalBooleanRef suppressLogRef
    ) {
        if (PackUtil.isNonPackDirectory(path)) {
            suppressLogRef.set(true);
            boolean isRoot = !IS_SUBDIRECTORY.get();
            try {
                if (isRoot) {
                    IS_SUBDIRECTORY.set(true);
                    discoverPacks(path, validator, output);
                }
            } catch (IOException e) {
                PackedPacks.LOGGER.warn("[packed_packs] Failed to list packs in {}", path, e);
            } finally {
                if (isRoot) {
                    IS_SUBDIRECTORY.remove();
                }
            }
        }

        return original.call(instance, path, list);
    }

    @WrapOperation(method = "discoverPacks", at = @At(
            value = "INVOKE",
            target = "Lorg/slf4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;)V",
            remap = false
    ))
    private static void suppressLogOnFolderDiscovery(Logger instance, String s, Object o, Operation<Void> original, @Share("suppressLog") LocalBooleanRef suppressLogRef) {
        if (!suppressLogRef.get() && !(o instanceof Path p && (p.endsWith(FolderResources.FOLDER_CONFIG_FILENAME) || p.endsWith(PackUtil.ICON_FILENAME)))) {
            original.call(instance, s, o);
        }
        suppressLogRef.set(false);
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @ModifyArg(method = {"method_45272", "lambda$loadPacks$0"}, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/packs/repository/Pack;readMetaAndCreate(Lnet/minecraft/server/packs/PackLocationInfo;Lnet/minecraft/server/packs/repository/Pack$ResourcesSupplier;Lnet/minecraft/server/packs/PackType;Lnet/minecraft/server/packs/PackSelectionConfig;)Lnet/minecraft/server/packs/repository/Pack;"
    ))
    private PackLocationInfo modifyPackLocation(PackLocationInfo location, @Local(argsOnly = true) Path path) {
        return IS_SUBDIRECTORY.get() ? PackUtil.replicateLocationInfo(location, PackUtil.generateNestedPackId(path)) : location;
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @ModifyArg(method = {"method_45272", "lambda$loadPacks$0"}, at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"))
    private Object bindDirToNestedPack(Object arg, @Local(argsOnly = true) Path path) {
        if (arg instanceof FilePack pack) {
            pack.packed_packs$setNestedPack(IS_SUBDIRECTORY.get());
            pack.packed_packs$setPath(path);
        }
        return arg;
    }

    @Shadow
    public static void discoverPacks(Path folder, DirectoryValidator validator, BiConsumer<Path, Pack.ResourcesSupplier> output) throws IOException {
        throw new AssertionError();
    }
}
