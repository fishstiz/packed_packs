package io.github.fishstiz.packed_packs.impl.context;

import io.github.fishstiz.packed_packs.api.context.PackContext;
import io.github.fishstiz.packed_packs.pack.PackAssetManager;
import io.github.fishstiz.packed_packs.pack.PackFileOperations;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.Pack;

public class PackContextImpl implements PackContext {
    private final PackAssetManager assetManager;
    private final PackFileOperations fileOps;
    private final Pack pack;

    public PackContextImpl(Pack pack, PackAssetManager assetManager, PackFileOperations fileOps) {
        this.assetManager = assetManager;
        this.fileOps = fileOps;
        this.pack = pack;
    }

    @Override
    public Pack pack() {
        return this.pack;
    }

    @Override
    public Identifier icon() {
        return this.assetManager.getIcon(this.pack).location;
    }

    @Override
    public boolean fileModifiable() {
        return this.fileOps.isOperable(this.pack);
    }
}
