package io.github.fishstiz.packed_packs.gui2.actions.mutations;

import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.models.PackEntryLists;
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

    record Selected(@Nullable Profile profile, PackEntryLists packEntries) implements ProfileMutation {
    }

    record RenamingToggled() implements ProfileMutation {
        @Override
        public boolean resetHistory() {
            return false;
        }
    }

    record Renamed(Profile profile, String name) implements ProfileMutation {
        @Override
        public boolean resetHistory() {
            return false;
        }
    }

    record Deleted(Profile profile) implements ProfileMutation {
    }

    record DeletedAndReset(Profile profile, PackEntryLists packEntries) implements ProfileMutation {
    }

    record Added(Profile profile) implements ProfileMutation {
    }

    record DefaultRemoved() implements ProfileMutation {
    }

    record DefaultChanged(Profile profile, @Nullable PackEntryLists packEntries) implements ProfileMutation {
    }

    record LockToggled(Profile profile) implements ProfileMutation {
    }
}
