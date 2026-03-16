package io.github.fishstiz.testmod.pack;

import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;

public final class EmptyPackResourcesSupplier implements Pack.ResourcesSupplier {
    @Override
    public PackResources openPrimary(PackLocationInfo location) {
        return new EmptyPackResources(location);
    }

    @Override
    public PackResources openFull(PackLocationInfo location, Pack.Metadata metadata) {
        return openPrimary(location);
    }
}
