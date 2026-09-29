package io.github.fishstiz.packed_packs.gui.actions.mutations;

import io.github.fishstiz.packed_packs.config.PackOverride;
import io.github.fishstiz.packed_packs.gui.components.SortOption;
import io.github.fishstiz.packed_packs.gui.states.PackListKey;
import io.github.fishstiz.packed_packs.pack.PackNode;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.SequencedCollection;

public sealed interface PackListMutation extends Mutation {
    PackListKey srcList();

    sealed interface Cross extends PackListMutation { // state mutations that cross beyond pack list state
    }

    sealed interface Transfer extends PackListMutation {
        @Nullable PackNode srcPack();

        SequencedCollection<PackNode> packs();

        int index();
    }

    record Enabled(
            PackListKey srcList,
            @Nullable PackNode srcPack,
            SequencedCollection<PackNode> packs,
            int index
    ) implements Cross, Transfer {
    }

    record Disabled(
            PackListKey srcList,
            @Nullable PackNode srcPack,
            SequencedCollection<PackNode> packs,
            int index
    ) implements Cross, Transfer {
        public Disabled(PackListKey srcList, @Nullable PackNode srcPack, SequencedCollection<PackNode> packs) {
            this(srcList, srcPack, packs, 0);
        }
    }

    record Dragged(PackListKey srcList, PackNode srcPack, SequencedCollection<PackNode> packs) implements Cross {
        @Override
        public boolean pushState() {
            return false;
        }
    }

    record Dropped(
            PackListKey srcList,
            SequencedCollection<PackNode> packs,
            @Nullable PackListKey targetList,
            int index
    ) implements Cross {
        @Override
        public boolean pushState() {
            return targetList != null;
        }
    }

    record ModuleUpdated(PackListKey srcList, boolean module) implements Cross {
        @Override
        public boolean resetHistory() {
            return true;
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

    record AliasesModalClosed(PackListKey srcList) implements Cross {
    }

    sealed interface Local extends PackListMutation { // state mutations local to srcList
    }

    record Searched(PackListKey srcList, String search) implements Local {
    }

    record Sorted(PackListKey srcList, SortOption sort) implements Local {
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

    static boolean isEnableAction(Mutation mutation) {
        return mutation instanceof PackListMutation.Enabled
               || (mutation instanceof PackListMutation.Dropped dropped
                   && dropped.targetList != null && dropped.targetList.type().enabled() && dropped.srcList.type().available())
               || (mutation instanceof PackListMutation.RequirementOverridden override
                   && Boolean.TRUE.equals(override.required()));
    }

    static boolean isDisableAction(Mutation mutation) {
        return mutation instanceof PackListMutation.Disabled
               || (mutation instanceof PackListMutation.Dropped dropped
                   && dropped.targetList != null && dropped.targetList.type().available() && dropped.srcList.type().enabled());
    }

    static boolean isMoveAction(Mutation mutation) {
        return mutation instanceof PackListMutation.Moved
               || mutation instanceof PackListMutation.MovedOnce
               || (mutation instanceof PackListMutation.Dropped dropped && dropped.targetList != null
                   && (dropped.targetList.equals(dropped.srcList) || dropped.targetList.depth() > 0));
    }

    static @Nullable PackListKey getTarget(Mutation mutation) {
        if (!(mutation instanceof PackListMutation listMutation)) {
            return null;
        }
        if (mutation instanceof Transfer transfer) {
            return PackListKey.head(transfer.srcList().type().other());
        }
        if (mutation instanceof Dropped dropped) {
            return dropped.targetList;
        }
        return listMutation.srcList();
    }
}
