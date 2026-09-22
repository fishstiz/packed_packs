package io.github.fishstiz.packed_packs.pack;

import io.github.fishstiz.packed_packs.util.PackUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Optional;

public record FolderLocationInfo(Path path, PackLocationInfo location, @Nullable FolderLocationInfo parent) {
    public static FolderLocationInfo fromPath(Path path, @Nullable FolderLocationInfo parent) {
        String name = path.getFileName().toString();
        String id = parent == null ? "file/" + name : parent.location().id() + '/' + name;
        PackLocationInfo info = new PackLocationInfo(id, Component.literal(name), PackUtil.PACK_SOURCE, Optional.empty());
        return new FolderLocationInfo(path, info, parent);
    }
}
