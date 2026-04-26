package io.github.fishstiz.packed_packs.pack.folder;

import io.github.fishstiz.packed_packs.util.PackUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;

import java.nio.file.Path;
import java.util.Optional;

public record FolderLocationInfo(String id, String name, Path path) {
    public FolderLocationInfo {
        path = path.toAbsolutePath().normalize();
    }

    public static FolderLocationInfo fromPath(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        String name = PackUtil.generatePackName(normalized);
        String id = PackUtil.generatePackId(name);
        return new FolderLocationInfo(id, name, normalized);
    }

    public PackLocationInfo packLocationInfo() {
        return new PackLocationInfo(this.id, Component.literal(this.name), PackUtil.PACK_SOURCE, Optional.empty());
    }
}
