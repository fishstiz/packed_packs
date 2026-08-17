package io.github.fishstiz.packed_packs.gui2.intents;

import io.github.fishstiz.packed_packs.config.PackOverride;
import io.github.fishstiz.packed_packs.gui.model.PackListKey;
import io.github.fishstiz.packed_packs.gui.model.Query;
import io.github.fishstiz.packed_packs.gui2.models.PackEntry;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.Nullable;

import java.util.SequencedCollection;

// everything should be id-based/or not for optimization so we dont have to keep mapping
// should remove some payload that can be gotten from state
public sealed interface PackListIntent extends Intent {
    record Search(PackListKey srcList, String query) implements PackListIntent {
    }

    record Sort(PackListKey srcList, Query.SortOption sort) implements PackListIntent {
    }

    record HideIncompatible(PackListKey srcList, boolean hide) implements PackListIntent {
    }

    record Select(PackListKey srcList, PackEntry pack) implements PackListIntent {
    }

    record SelectExclusive(PackListKey srcList, PackEntry pack) implements PackListIntent {
    }

    record SelectToggle(PackListKey srcList, PackEntry pack) implements PackListIntent {
    }

    record SelectRange(PackListKey srcList, PackEntry pack) implements PackListIntent {
    }

    record SelectAll(PackListKey srcList, @Nullable PackEntry pack) implements PackListIntent {
    }

    record Enable(
            PackListKey srcList,
            PackEntry srcPack,
            SequencedCollection<Pack> packs,
            int index
    ) implements PackListIntent {
        public Enable(PackListKey srcList, PackEntry srcPack, SequencedCollection<Pack> payload) {
            this(srcList, srcPack, payload, 0);
        }
    }

    record Disable(PackListKey srcList, PackEntry srcPack, SequencedCollection<Pack> packs) implements PackListIntent {
    }

    record Move(
            PackListKey srcList,
            PackEntry srcPack,
            SequencedCollection<Pack> packs,
            int index
    ) implements PackListIntent {
    }

    record MoveOnce(
            PackListKey srcList,
            PackEntry srcPack,
            SequencedCollection<Pack> packs,
            boolean upwards
    ) {
        public static MoveOnce up(PackListKey srcList, PackEntry srcPack, SequencedCollection<Pack> packs) {
            return new MoveOnce(srcList, srcPack, packs, true);
        }

        public static MoveOnce down(PackListKey srcList, PackEntry srcPack, SequencedCollection<Pack> packs) {
            return new MoveOnce(srcList, srcPack, packs, false);
        }

        public boolean downwards() {
            return !upwards;
        }
    }

    record Drag(PackListKey srcList, PackEntry srcPack, SequencedCollection<Pack> packs) implements PackListIntent {
    }

    record Drop(PackListKey srcList, @Nullable PackListKey targetList, int index) implements PackListIntent {
    }

    record OpenRenameModal(PackListKey srcList, PackEntry pack) implements PackListIntent {
    }

    record CloseRenameModal(PackListKey srcList, PackEntry pack) implements PackListIntent {
    }

    record OpenFolder(PackListKey srcList, PackEntry.Parent pack) implements PackListIntent {
    }

    record CloseFolder(PackListKey srcList) implements PackListIntent {
    }

    record Rename(PackListKey srcList, PackEntry pack, String newName) implements PackListIntent {
    }

    record Delete(PackListKey srcList, PackEntry pack) implements PackListIntent {
    }

    record OverrideHidden(
            PackListKey srcList,
            PackEntry srcPack,
            SequencedCollection<Pack> packs,
            boolean hidden
    ) implements PackListIntent {
    }

    record OverrideRequirement(
            PackListKey srcList,
            PackEntry srcPack,
            SequencedCollection<Pack> packs,
            @Nullable Boolean required
    ) implements PackListIntent {
    }

    record OverridePosition(
            PackListKey srcList,
            PackEntry srcPack,
            SequencedCollection<Pack> packs,
            PackOverride.@Nullable Position position
    ) implements PackListIntent {
    }

    record RemoveOverrides(
            PackListKey srcList,
            PackEntry srcPack,
            SequencedCollection<Pack> packs
    ) implements PackListIntent {
    }

    record OpenAliasesModal(PackListKey srcList, PackEntry pack) implements PackListIntent {
    }

    record CloseAliases() implements PackListIntent {
    }
}
