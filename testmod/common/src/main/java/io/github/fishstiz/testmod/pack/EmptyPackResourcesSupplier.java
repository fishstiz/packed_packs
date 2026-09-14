package io.github.fishstiz.testmod.pack;

import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackMetadataResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;

import java.util.stream.Stream;

public final class EmptyPackResourcesSupplier implements Pack.ResourcesSupplier {
    @Override
    public EmptyPackResources openMetadata(PackLocationInfo packLocationInfo) {
        return new EmptyPackResources(packLocationInfo);
    }

    @Override
    public Stream<PackResources> openResources(PackLocationInfo packLocationInfo, Pack.Metadata metadata) {
        return Stream.of(openMetadata(packLocationInfo));
    }
}
