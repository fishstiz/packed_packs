package io.github.fishstiz.packed_packs.pack.folder;

import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public record FolderResourcesSupplier(Path path) implements Pack.ResourcesSupplier {
        @Override
        public @NotNull FolderResources openPrimary(PackLocationInfo location) {
            return new FolderResources(location, this.path);
        }

        @Override
        public @NotNull FolderResources openFull(PackLocationInfo location, Pack.Metadata metadata) {
            return this.openPrimary(location);
        }
    }