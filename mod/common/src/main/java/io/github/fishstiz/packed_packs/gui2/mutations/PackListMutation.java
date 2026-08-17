package io.github.fishstiz.packed_packs.gui2.mutations;

import io.github.fishstiz.packed_packs.config.PackOverride;
import io.github.fishstiz.packed_packs.gui.model.PackListKey;
import io.github.fishstiz.packed_packs.gui.model.Query;
import io.github.fishstiz.packed_packs.gui2.models.PackEntry;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.SequencedCollection;
// todo maybe combine with intent, this is too much boilerplate
public sealed interface PackListMutation extends Mutation {
    // state mutations that cross beyond pack list state
    sealed interface Cross extends PackListMutation {
    }

    record Enabled(
            PackListKey srcList,
            PackEntry srcPack,
            SequencedCollection<Pack> packs,
            int index
    ) implements Cross {
    }

    record Disabled(
            PackListKey srcList,
            PackEntry srcPack,
            SequencedCollection<Pack> packs
    ) implements Cross {
    }

    record Dragged(PackListKey srcList, PackEntry srcPack, SequencedCollection<Pack> packs) implements Cross {
        @Override
        public boolean pushState() {
            return false;
        }
    }

    record Dropped(PackListKey srcList, @Nullable PackListKey targetList, int index) implements Cross {
        @Override
        public boolean pushState() {
            return targetList != null;
        }
    }

    record RenameModalOpened(PackListKey srcList, PackEntry pack) implements Cross {
    }

    record RenameModalClosed(PackListKey srcList, PackEntry pack) implements Cross {
    }

    record VisibilityOverridden(
            PackListKey srcList,
            PackEntry srcPack,
            SequencedCollection<Pack> payload,
            boolean hidden
    ) implements Cross {
    }

    record RequirementOverridden(
            PackListKey srcList,
            PackEntry srcPack,
            SequencedCollection<Pack> pack,
            @Nullable Boolean required
    ) implements Cross {
        @Override
        public boolean resetHistory() {
            return Boolean.TRUE.equals(required) && srcList.type().available();
        }
    }

    record PositionOverridden(
            PackListKey srcList,
            PackEntry srcPack,
            SequencedCollection<Pack> payload,
            PackOverride.@Nullable Position position
    ) implements Cross {
    }

    record OverridesRemoved(
            PackListKey srcList,
            PackEntry srcPack,
            SequencedCollection<Pack> packs
    ) implements Cross {
    }

    record AliasesModalOpened(
            PackListKey srcList,
            PackEntry srcPack,
            List<String> aliases
    ) implements Cross {
    }

    record AliasesModalClosed() implements Cross {
    }

    // state mutations local to srcList
    sealed interface Local extends PackListMutation {
        PackListKey srcList();
    }

    record Searched(PackListKey srcList, String search) implements Local {
    }

    record Sorted(PackListKey srcList, Query.SortOption sort) implements Local {
    }

    record IncompatibleHidden(PackListKey srcList, boolean hidden) implements Local {
    }

    record Selected(PackListKey srcList, PackEntry pack) implements Local {
    }

    record SelectedExclusively(PackListKey srcList, PackEntry pack) implements Local {
    }

    record SelectionToggled(PackListKey srcList, PackEntry pack) implements Local {
    }

    record SelectedRange(PackListKey srcList, PackEntry pack) implements Local {
    }

    record SelectedAll(PackListKey srcList, PackEntry pack) implements Local {
    }

    record Moved(
            PackListKey srcList,
            PackEntry srcPack,
            SequencedCollection<Pack> packs,
            int index
    ) implements Local {
    }

    record MovedOnce(
            PackListKey srcList,
            PackEntry srcPack,
            SequencedCollection<Pack> packs,
            boolean upwards
    ) implements Local {
    }

    record FolderOpened(
            PackListKey srcList,
            PackEntry.Parent pack,
            boolean locked,
            List<PackEntry> children
    ) implements Local {
    }

    record FolderClosed(PackListKey srcList) implements Local {
    }
}
