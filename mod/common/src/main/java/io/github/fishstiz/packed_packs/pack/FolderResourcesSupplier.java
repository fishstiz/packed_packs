package io.github.fishstiz.packed_packs.pack;

import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.NullMarked;

import java.nio.file.Path;

@NullMarked
public record FolderResourcesSupplier(Path path) implements Pack.ResourcesSupplier {
    @Override
    public FolderResources openPrimary(PackLocationInfo location) {
        return new FolderResources(location, this.path);
    }

    @Override
    public FolderResources openFull(PackLocationInfo location, Pack.Metadata metadata) {
        return this.openPrimary(location);
    }
}