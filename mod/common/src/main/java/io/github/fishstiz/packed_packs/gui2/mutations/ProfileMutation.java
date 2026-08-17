package io.github.fishstiz.packed_packs.gui2.mutations;

import io.github.fishstiz.packed_packs.gui2.models.PackEntryLists;
import org.jspecify.annotations.Nullable;

public sealed interface ProfileMutation extends Mutation {
    @Override
    default boolean pushState() {
        return false;
    }

    @Override
    default boolean resetHistory() {
        return true;
    }

    record SelectedChanged(@Nullable String id, PackEntryLists packEntries) implements ProfileMutation {
    }

    record RenamingToggled() implements ProfileMutation {
        @Override
        public boolean resetHistory() {
            return false;
        }
    }

    record Renamed(String id, String name) implements ProfileMutation {
        @Override
        public boolean resetHistory() {
            return false;
        }
    }

    record DeleteSuccess(String id, @Nullable PackEntryLists packEntries) implements ProfileMutation {
        // todo reset history if selected = deleted
    }

    record DeleteFailed(String id) implements ProfileMutation {
        @Override
        public boolean resetHistory() {
            return false;
        }
    }

    record SelectedCopied() {
    }

    record DefaultChanged(@Nullable String id) implements ProfileMutation {
    }

    record LockToggled(String id) implements ProfileMutation {
    }
}
