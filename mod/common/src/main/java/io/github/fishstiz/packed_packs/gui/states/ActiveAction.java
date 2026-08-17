package io.github.fishstiz.packed_packs.gui.states;

import io.github.fishstiz.packed_packs.gui.model.PackListKey;
import io.github.fishstiz.packed_packs.api.context.PackContext;
import io.github.fishstiz.packed_packs.gui2.models.PackEntry;
import net.minecraft.server.packs.repository.Pack;

import java.util.List;
import java.util.SequencedCollection;

public sealed interface ActiveAction {
    PackListKey target();

    record RenamingPack(PackListKey target, PackEntry pack, boolean loading) implements ActiveAction {
    }

    record EditingAliases(PackListKey target, PackContext ctx, List<String> aliases) implements ActiveAction {
    }

    record Dragging(PackListKey target, PackContext ctx, SequencedCollection<Pack> payload) implements ActiveAction {
    }
}