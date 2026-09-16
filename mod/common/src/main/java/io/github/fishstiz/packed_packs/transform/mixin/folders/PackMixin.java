package io.github.fishstiz.packed_packs.transform.mixin.folders;

import io.github.fishstiz.packed_packs.pack.FolderLocationInfo;
import io.github.fishstiz.packed_packs.transform.interfaces.FilePack;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.nio.file.Path;

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
}
