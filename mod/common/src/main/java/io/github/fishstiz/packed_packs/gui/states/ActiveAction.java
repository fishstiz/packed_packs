package io.github.fishstiz.packed_packs.gui.states;

import io.github.fishstiz.packed_packs.gui.model.PackListKey;
import io.github.fishstiz.packed_packs.models.PackEntry;

import java.util.List;
import java.util.SequencedCollection;

public sealed interface ActiveAction {
    PackListKey target();

    record RenamingPack(PackListKey target, PackEntry pack, boolean loading) implements ActiveAction {
        public RenamingPack withLoading(boolean loading) {
            return new RenamingPack(target, pack, loading);
        }
    }

    record EditingAliases(PackListKey target, PackEntry pack, List<String> aliases) implements ActiveAction {
    }

    record Dragging(PackListKey target, PackEntry srcPack, SequencedCollection<PackEntry> packs) implements ActiveAction {
    }
}