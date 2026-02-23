package io.github.fishstiz.packed_packs.gui.states;

import io.github.fishstiz.packed_packs.gui.model.PackListKey;
import io.github.fishstiz.packed_packs.impl.context.PackEntryContext;
import net.minecraft.server.packs.repository.Pack;

import java.util.List;
import java.util.SequencedCollection;

public sealed interface ActiveAction {
    PackListKey target();

    record RenamingPack(PackListKey target, PackEntryContext ctx) implements ActiveAction {
    }

    record EditingAliases(PackListKey target, PackEntryContext ctx, List<String> aliases) implements ActiveAction {
    }

    record Dragging(PackListKey target, PackEntryContext ctx, SequencedCollection<Pack> payload) implements ActiveAction {
    }
}