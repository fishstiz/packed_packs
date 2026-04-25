package io.github.fishstiz.packed_packs.pack.folder;

import io.github.fishstiz.packed_packs.util.PackUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;

import java.nio.file.Path;
import java.util.Optional;

public record FolderLocationInfo(String id, String name, Path path) {
    public static FolderLocationInfo fromPath(Path path) {
        String name = PackUtil.generatePackName(path);
        String id = PackUtil.generatePackId(name);
        return new FolderLocationInfo(id, name, path);
    }

    public PackLocationInfo packLocationInfo() {
        return new PackLocationInfo(this.id, Component.literal(this.name), PackUtil.PACK_SOURCE, Optional.empty());
    }
}
