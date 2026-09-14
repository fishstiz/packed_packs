package io.github.fishstiz.packed_packs.pack.folder;

import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.NullMarked;

import java.nio.file.Path;
import java.util.stream.Stream;

@NullMarked
public record FolderResourcesSupplier(Path path) implements Pack.ResourcesSupplier {
    @Override
    public FolderResources openMetadata(PackLocationInfo location) {
        return new FolderResources(location, this.path);
    }

    @Override
    public Stream<PackResources> openResources(PackLocationInfo location, Pack.Metadata metadata) {
        return Stream.of(openMetadata(location));
    }
}