package io.github.fishstiz.packed_packs.config;

import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.packed_packs.util.Utils;
import net.minecraft.server.packs.repository.Pack;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
        return List.copyOf(this.packIds);
    }

    public void save(Path path) {
        JsonLoader.saveJson(this, path);
    }
}
