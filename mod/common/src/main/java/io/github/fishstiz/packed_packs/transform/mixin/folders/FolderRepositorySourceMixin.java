package io.github.fishstiz.packed_packs.transform.mixin.folders;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.datafixers.util.Pair;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.config.FolderPackMeta;
import io.github.fishstiz.packed_packs.models.PackEntry;
import io.github.fishstiz.packed_packs.pack.folder.FolderResourcesSupplier;
import io.github.fishstiz.packed_packs.transform.interfaces.FilePack;
import io.github.fishstiz.packed_packs.util.PackUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackDetector;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Mixin(FolderRepositorySource.class)
public abstract class FolderRepositorySourceMixin {
    @Inject(method = "loadPacks", at = @At("HEAD"))
    private void shareEntryCallback(
            Consumer<Pack> result,
            CallbackInfo ci,
            @Share("entryCallback") LocalRef<@Nullable Consumer<PackEntry>> entryCallbackRef
    ) {
        entryCallbackRef.set(
                PackEntry.ENTRY_CALLBACK.isBound() ? PackEntry.ENTRY_CALLBACK.get() : null
        );
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
            @Share("suppressLog") LocalBooleanRef suppressLogRef,
            @Share("entryCallback") LocalRef<@Nullable Consumer<PackEntry>> entryCallbackRef,
            @Share("parent") LocalRef<@Nullable Pair<String, List<PackEntry>>> parentRef,
            @Share("depth") LocalIntRef depthRef
    ) {
        if (PackUtil.isNonPackDirectory(path)) {
            suppressLogRef.set(true);

            Pair<String, List<PackEntry>> parent = parentRef.get();
            int depth = depthRef.get();
            depthRef.set(depth + 1);

            try {
                Path normalized = path.toAbsolutePath().normalize();
                String name = PackUtil.generatePackName(normalized);
                String baseId = PackUtil.generatePackId(name);
                // todo consider not making it recursive
                // id concat is needed otherwise nested folders cant have same names
                // which is probably going to be common
                String id = parent == null ? baseId : parent.getFirst() + '/' + baseId;

                Consumer<PackEntry> entryCallback = entryCallbackRef.get();
                if (entryCallback == null) {
                    parentRef.set(Pair.of(id, Collections.emptyList()));
                    discoverPacks(path, validator, output);
                } else {
                    List<PackEntry> children = new ArrayList<>();

                    parentRef.set(Pair.of(id, children));
                    discoverPacks(path, validator, output);

                    PackLocationInfo info = new PackLocationInfo(
                            id,
                            Component.literal(name),
                            PackUtil.PACK_SOURCE,
                            Optional.empty()
                    );

                    entryCallback.accept(new PackEntry.Parent(
                            parent == null ? null : parent.getFirst(),
                            normalized,
                            info,
                            new FolderResourcesSupplier(normalized),
                            children
                    ));
                }
            } catch (IOException e) {
                PackedPacks.LOGGER.warn("[packed_packs] Failed to list packs in {}", path, e);
            } finally {
                parentRef.set(parent);
                depthRef.set(depth);
            }
        }

        return original.call(instance, path, list);
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

    @ModifyArg(method = "lambda$loadPacks$0", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/packs/repository/Pack;readMetaAndCreate(Lnet/minecraft/server/packs/PackLocationInfo;Lnet/minecraft/server/packs/repository/Pack$ResourcesSupplier;Lnet/minecraft/server/packs/PackType;Lnet/minecraft/server/packs/PackSelectionConfig;)Lnet/minecraft/server/packs/repository/Pack;"
    ))
    private PackLocationInfo modifyPackLocation(
            PackLocationInfo location,
            @Local(argsOnly = true) Path path,
            @Share("depth") LocalIntRef depthRef
    ) {
        // todo set as internal alias for depth == 1 instead and remove modifying pack location
        return depthRef.get() == 1
                ? PackUtil.replicateLocationInfo(location, PackUtil.generateNestedPackId(path))
                : location;
    }

    @ModifyArg(method = "lambda$loadPacks$0", at = @At(
            value = "INVOKE",
            target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"
    ))
    private Object bindDirToNestedPack(
            Object arg,
            @Local(argsOnly = true) Path path,
            @Share("entryCallback") LocalRef<@Nullable Consumer<PackEntry>> entryCallbackRef,
            @Share("parent") LocalRef<@Nullable Pair<String, List<PackEntry>>> parentRef
    ) {
        if (arg instanceof FilePack pack) {
            pack.packed_packs$setPath(path);

            Consumer<PackEntry> entryCallback = entryCallbackRef.get();
            Pair<String, List<PackEntry>> parent = parentRef.get();
            PackEntry entry = null;

            if (parent != null) {
                pack.packed_packs$setNestedPack(true);
                if (entryCallback != null) {
                    entry = new PackEntry.Leaf((Pack) pack, path, parent.getFirst());
                    parent.getSecond().add(entry);
                }
            } else {
                pack.packed_packs$setNestedPack(false);
                if (entryCallback != null) {
                    entry = new PackEntry.Leaf((Pack) pack, path, null);
                }
            }

            if (entryCallback != null) {
                entryCallback.accept(entry);
            }
        }
        return arg;
    }

    @Shadow
    public static void discoverPacks(Path folder, DirectoryValidator validator, BiConsumer<Path, Pack.ResourcesSupplier> output) throws IOException {
        throw new AssertionError();
    }
}
