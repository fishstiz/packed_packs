package io.github.fishstiz.packed_packs.transform.mixin.folders;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.fishstiz.packed_packs.pack.FolderLocationInfo;
import io.github.fishstiz.packed_packs.transform.interfaces.FilePack;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.nio.file.Path;
import java.util.Objects;

@Mixin(Pack.class)
public abstract class PackMixin implements FilePack {
    @Unique
    private Path packed_packs$path;

    @Unique
    private FolderLocationInfo packed_packs$parent;

    @Override
    public @Nullable FolderLocationInfo packed_packs$getParent() {
        return this.packed_packs$parent;
    }

    @Override
    public void packed_packs$setParent(FolderLocationInfo parent) {
        this.packed_packs$parent = parent;
    }

    @Override
    public void packed_packs$setPath(Path path) {
        this.packed_packs$path = path;
    }

    @Override
    public @Nullable Path packed_packs$getPath() {
        return this.packed_packs$path;
    }

    // PackSelectionModel would otherwise retain nested packs that moved directories on reload
    @WrapOperation(method = "equals", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/packs/PackLocationInfo;equals(Ljava/lang/Object;)Z"
    ))
    private boolean onEquals(
            PackLocationInfo instance,
            Object object,
            Operation<Boolean> original
    ) {
        boolean equals = original.call(instance, object);
        if (packed_packs$path == null || !(object instanceof Pack that)) {
            return equals;
        }

        return equals && Objects.equals(packed_packs$path, ((FilePack) that).packed_packs$getPath());
    }

    @ModifyReturnValue(method = "hashCode", at = @At(value = "RETURN"))
    private int onHashCode(int original) {
        if (packed_packs$path == null) return original;

        return 31 * original + Objects.hashCode(packed_packs$path);
    }
}
