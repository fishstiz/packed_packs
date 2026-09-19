package io.github.fishstiz.packed_packs.gui.states;

import io.github.fishstiz.packed_packs.pack.PackNode;

import java.util.List;
import java.util.SequencedCollection;

public sealed interface ActiveAction {
    PackListKey src();

    record RenamingPack(PackListKey src, PackNode pack, boolean loading) implements ActiveAction {
        public RenamingPack withLoading(boolean loading) {
            return new RenamingPack(src, pack, loading);
        }
    }

    record EditingAliases(PackListKey src, PackNode pack, List<String> aliases) implements ActiveAction {
    }

    record Dragging(
            PackListKey src,
            boolean srcModule,
            PackNode srcPack,
            SequencedCollection<PackNode> packs
    ) implements ActiveAction {
    }
}