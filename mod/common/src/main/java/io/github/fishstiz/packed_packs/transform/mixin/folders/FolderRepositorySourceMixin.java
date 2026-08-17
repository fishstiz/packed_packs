package io.github.fishstiz.packed_packs.transform.mixin.folders;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.config.FolderPackMeta;
import io.github.fishstiz.packed_packs.pack.FolderLocationInfo;
import io.github.fishstiz.packed_packs.transform.interfaces.FilePack;
import io.github.fishstiz.packed_packs.util.PackUtil;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackDetector;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;
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
    @Shadow
    @Final
    private static Logger LOGGER;

    @Unique
    private static final ThreadLocal<@Nullable FolderLocationInfo> PARENT_CONTEXT = new ThreadLocal<>();

    @Inject(method = "loadPacks", at = @At("RETURN"))
    private void ensureRemoveThreadLocals(Consumer<Pack> result, CallbackInfo ci) {
        PARENT_CONTEXT.remove();
    }

    @WrapOperation(method = "discoverPacks", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/packs/repository/FolderRepositorySource$FolderPackDetector;detectPackResources(Ljava/nio/file/Path;Ljava/util/List;)Ljava/lang/Object;"
    ))
    private static Object discoverNestedPacks(
            @Coerce PackDetector<Pack.ResourcesSupplier> instance,
            Path path,
            List<ForbiddenSymlinkInfo> list,
            Operation<Pack.ResourcesSupplier> original,
            @Local(argsOnly = true) DirectoryValidator validator,
            @Local(argsOnly = true) BiConsumer<Path, Pack.ResourcesSupplier> output,
            @Share("suppressLog") LocalBooleanRef suppressLogRef
    ) {
        Pack.ResourcesSupplier resourcesSupplier = null;
        try {
            resourcesSupplier = original.call(instance, path, list);
        } catch (Exception e) {
            LOGGER.warn("Failed to read properties of '{}', ignoring", path, e);
        }

        if (resourcesSupplier == null && PackUtil.isNonPackDirectory(path)) {
            suppressLogRef.set(true);

            FolderLocationInfo parent = PARENT_CONTEXT.get();

            try {
                FolderLocationInfo parentInfo = FolderLocationInfo.fromPath(path.toAbsolutePath().normalize(), parent);
                PARENT_CONTEXT.set(parentInfo);
                discoverPacks(path, validator, output);
            } catch (IOException e) {
                PackedPacks.LOGGER.warn("[packed_packs] Failed to list packs in {}", path, e);
            } finally {
                if (parent == null) {
                    PARENT_CONTEXT.remove();
                } else {
                    PARENT_CONTEXT.set(parent);
                }
            }
        }

        return resourcesSupplier;
    }

    @WrapOperation(method = "discoverPacks", at = @At(
            value = "INVOKE",
            target = "Lorg/slf4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;)V",
            remap = false
    ))
    private static void suppressLogOnFolderDiscovery(
            Logger instance,
            String s,
            Object o,
            Operation<Void> original,
            @Share("suppressLog") LocalBooleanRef suppressLogRef
    ) {
        if (!suppressLogRef.get() &&
            !(o instanceof Path p && (p.endsWith(FolderPackMeta.FILENAME) || p.endsWith(PackUtil.ICON_FILENAME)))) {
            original.call(instance, s, o);
        }
        suppressLogRef.set(false);
    }

    // todo migrate folder pack ids using VersionState

    @ModifyArg(method = "lambda$loadPacks$0", at = @At(
            value = "INVOKE",
            target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"
    ))
    private Object bindDirToNestedPack(Object arg, @Local(argsOnly = true) Path path) {
        if (arg instanceof FilePack pack) {
            pack.packed_packs$setPath(path);
            FolderLocationInfo parent = PARENT_CONTEXT.get();
            if (parent != null) {
                pack.packed_packs$setParent(parent);
            }
        }
        return arg;
    }

    @Shadow
    public static void discoverPacks(Path folder, DirectoryValidator validator, BiConsumer<Path, Pack.ResourcesSupplier> output) throws IOException {
        throw new AssertionError();
    }
}
