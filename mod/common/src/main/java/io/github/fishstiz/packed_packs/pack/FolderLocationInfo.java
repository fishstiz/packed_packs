package io.github.fishstiz.packed_packs.pack;

import io.github.fishstiz.packed_packs.util.PackUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Optional;

public record FolderLocationInfo(Path path, PackLocationInfo location, @Nullable FolderLocationInfo parent) {
    public static FolderLocationInfo fromPath(Path path, @Nullable FolderLocationInfo parent) {
        String name = PackUtil.generatePackName(path);
        String baseId = PackUtil.generatePackId(name);
        // id concat is needed otherwise nested folders cant have same names
        // which is probably going to be common
        String id = parent == null ? baseId : parent.location().id() + '/' + baseId;
        PackLocationInfo info = new PackLocationInfo(id, Component.literal(name), PackUtil.PACK_SOURCE, Optional.empty());
        return new FolderLocationInfo(path, info, parent);
    }
}
