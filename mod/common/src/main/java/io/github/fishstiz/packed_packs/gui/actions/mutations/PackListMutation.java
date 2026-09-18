package io.github.fishstiz.packed_packs.gui.actions.mutations;

import io.github.fishstiz.packed_packs.config.PackOverride;
import io.github.fishstiz.packed_packs.gui.model.PackListKey;
import io.github.fishstiz.packed_packs.gui.model.Query;
import io.github.fishstiz.packed_packs.pack.PackNode;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.SequencedCollection;

public sealed interface PackListMutation extends Mutation {
    // state mutations that cross beyond pack list state
    sealed interface Cross extends PackListMutation {
    }

    record Enabled(
            PackListKey srcList,
            PackNode srcPack,
            SequencedCollection<PackNode> packs,
            int index
    ) implements Cross {
    }

    record Disabled(
            PackListKey srcList,
            PackNode srcPack,
            SequencedCollection<PackNode> packs
    ) implements Cross {
    }

    record Dragged(PackListKey srcList, PackNode srcPack, SequencedCollection<PackNode> packs) implements Cross {
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

    record RenameModalOpened(PackListKey srcList, PackNode pack) implements Cross {
    }

    record RenameModalClosed(PackListKey srcList, PackNode pack) implements Cross {
    }

    record VisibilityOverridden(
            PackListKey srcList,
            PackNode srcPack,
            SequencedCollection<PackNode> packs,
            boolean hidden
    ) implements Cross {
    }

    record RequirementOverridden(
            PackListKey srcList,
            PackNode srcPack,
            SequencedCollection<PackNode> packs,
            @Nullable Boolean required
    ) implements Cross {
        @Override
        public boolean resetHistory() {
            return Boolean.TRUE.equals(required) && srcList.type().available();
        }
    }

    record PositionOverridden(
            PackListKey srcList,
            PackNode srcPack,
            SequencedCollection<PackNode> packs,
            PackOverride.@Nullable Position position
    ) implements Cross {
    }

    record OverridesRemoved(
            PackListKey srcList,
            PackNode srcPack,
            SequencedCollection<PackNode> packs
    ) implements Cross {
    }

    record AliasesModalOpened(
            PackListKey srcList,
            PackNode srcPack,
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

    record Selected(PackListKey srcList, PackNode pack) implements Local {
    }

    record SelectedMultiple(PackListKey srcList, SequencedCollection<PackNode> packs) implements Local {
    }

    record SelectedExclusively(PackListKey srcList, PackNode pack) implements Local {
    }

    record SelectionToggled(PackListKey srcList, PackNode pack) implements Local {
    }

    record SelectedRange(PackListKey srcList, PackNode pack) implements Local {
    }

    record SelectedAll(PackListKey srcList, PackNode pack) implements Local {
    }

    record Moved(
            PackListKey srcList,
            PackNode srcPack,
            SequencedCollection<PackNode> packs,
            int index
    ) implements Local {
    }

    record MovedOnce(
            PackListKey srcList,
            PackNode srcPack,
            SequencedCollection<PackNode> packs,
            boolean upwards
    ) implements Local {
    }

    record FolderOpened(
            PackListKey srcList,
            PackNode.Parent pack,
            boolean locked,
            List<PackNode> children
    ) implements Local {
    }

    record FolderClosed(PackListKey srcList) implements Local {
    }
}
