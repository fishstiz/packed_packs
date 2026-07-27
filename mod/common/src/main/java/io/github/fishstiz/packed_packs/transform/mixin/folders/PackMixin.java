package io.github.fishstiz.packed_packs.transform.mixin.folders;

import io.github.fishstiz.packed_packs.transform.interfaces.FilePack;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.nio.file.Path;

@Mixin(Pack.class)
public abstract class PackMixin implements FilePack {
    @Unique
    private boolean packed_packs$nested = false;

    @Unique
    private Path packed_packs$path;

    @Override
    public boolean packed_packs$nestedPack() {
        return this.packed_packs$nested;
    }

    @Override
    public void packed_packs$setNestedPack(boolean nested) {
        this.packed_packs$nested = nested;
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
