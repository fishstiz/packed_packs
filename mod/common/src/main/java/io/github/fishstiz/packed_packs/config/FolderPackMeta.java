package io.github.fishstiz.packed_packs.config;

import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.packed_packs.util.Utils;
import net.minecraft.server.packs.repository.Pack;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FolderPackMeta {
    private List<String> packIds = new ArrayList<>();

    public boolean trySetPacks(List<Pack> packs) {
        return this.trySetPackIds(PackUtil.extractPackIds(packs));
    }

    public boolean trySetPackIds(List<String> newPackIds) {
        if (!Utils.orderEquals(packIds, newPackIds)) {
            this.packIds = newPackIds;
            return true;
        }
        return false;
    }

    public List<String> getPackIds() {
        // filter non-null for corrupted/incorrectly formatted metadata
        return this.packIds.stream().filter(Objects::nonNull).toList();
    }

    public void save(Path path) {
        JsonLoader.saveJson(this, path);
    }
}
