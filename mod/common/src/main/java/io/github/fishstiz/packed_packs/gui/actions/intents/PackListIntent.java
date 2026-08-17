package io.github.fishstiz.packed_packs.gui.actions.intents;

import io.github.fishstiz.packed_packs.config.PackOverride;
import io.github.fishstiz.packed_packs.gui.components.SortOption;
import io.github.fishstiz.packed_packs.gui.states.PackListKey;
import io.github.fishstiz.packed_packs.pack.PackNode;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.SequencedCollection;

public sealed interface PackListIntent extends Intent {
    record Search(PackListKey srcList, String query) implements PackListIntent {
    }

    record Sort(PackListKey srcList, SortOption sort) implements PackListIntent {
    }

    record HideIncompatible(PackListKey srcList, boolean hide) implements PackListIntent {
    }

    record Select(PackListKey srcList, PackNode pack) implements PackListIntent {
    }

    record SelectExclusive(PackListKey srcList, PackNode pack) implements PackListIntent {
    }

    record SelectToggle(PackListKey srcList, PackNode pack) implements PackListIntent {
    }

    record SelectRange(PackListKey srcList, PackNode pack) implements PackListIntent {
    }

    record SelectAll(PackListKey srcList, @Nullable PackNode pack) implements PackListIntent {
    }

    record Enable(
            PackListKey srcList,
            PackNode srcPack,
            SequencedCollection<PackNode> packs,
            int index
    ) implements PackListIntent {
        public Enable(PackListKey srcList, PackNode srcPack, SequencedCollection<PackNode> payload) {
            this(srcList, srcPack, payload, 0);
        }
    }

    record Disable(PackListKey srcList, PackNode srcPack, SequencedCollection<PackNode> packs) implements PackListIntent {
    }

    record Recall(PackListKey srcList, PackNode.Parent parent) implements PackListIntent {
    }

    record Move(
            PackListKey srcList,
            PackNode srcPack,
            SequencedCollection<PackNode> packs,
            int index
    ) implements PackListIntent {
    }

    record MoveOnce(
            PackListKey srcList,
            PackNode srcPack,
            SequencedCollection<PackNode> packs,
            boolean upwards
    ) implements PackListIntent {
        public static MoveOnce up(PackListKey srcList, PackNode srcPack, SequencedCollection<PackNode> packs) {
            return new MoveOnce(srcList, srcPack, packs, true);
        }

        public static MoveOnce down(PackListKey srcList, PackNode srcPack, SequencedCollection<PackNode> packs) {
            return new MoveOnce(srcList, srcPack, packs, false);
        }

        public boolean downwards() {
            return !upwards;
        }
    }

    record Drag(PackListKey srcList, PackNode srcPack, SequencedCollection<PackNode> packs) implements PackListIntent {
    }

    record Drop(PackListKey srcList, @Nullable PackListKey targetList, int index) implements PackListIntent {
        public Drop(PackListKey srcList) {
            this(srcList, null, 0);
        }
    }

    record OpenRenameModal(PackListKey srcList, PackNode pack) implements PackListIntent {
    }

    record CloseRenameModal(PackListKey srcList, PackNode pack) implements PackListIntent {
    }

    record OpenFolder(PackListKey srcList, PackNode.Parent pack) implements PackListIntent {
    }

    record CloseFolder(PackListKey srcList) implements PackListIntent {
    }

    record UpdateModule(PackListKey srcList, PackNode.Parent pack, boolean module) implements PackListIntent {
    }

    record Rename(PackListKey srcList, PackNode pack, String newName) implements PackListIntent {
    }

    record Delete(PackListKey srcList, PackNode pack) implements PackListIntent {
    }

    record OverrideHidden(
            PackListKey srcList,
            PackNode srcPack,
            SequencedCollection<PackNode> packs,
            boolean hidden
    ) implements PackListIntent {
    }

    record OverrideRequirement(
            PackListKey srcList,
            PackNode srcPack,
            SequencedCollection<PackNode> packs,
            @Nullable Boolean required
    ) implements PackListIntent {
    }

    record OverridePosition(
            PackListKey srcList,
            PackNode srcPack,
            SequencedCollection<PackNode> packs,
            PackOverride.@Nullable Position position
    ) implements PackListIntent {
    }

    record RemoveOverrides(
            PackListKey srcList,
            PackNode srcPack,
            SequencedCollection<PackNode> packs
    ) implements PackListIntent {
    }

    record OpenAliasesModal(PackListKey srcList, PackNode pack) implements PackListIntent {
    }

    record CloseAliases(List<String> newAliases) implements PackListIntent {
    }
}
