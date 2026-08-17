package io.github.fishstiz.packed_packs.gui.actions.mutations;

import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.pack.PackSelection;
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

    record Selected(@Nullable Profile profile, PackSelection packs) implements ProfileMutation {
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

    record DeletedAndReset(Profile profile, PackSelection packs) implements ProfileMutation {
    }

    record Added(Profile profile) implements ProfileMutation {
    }

    record DefaultRemoved() implements ProfileMutation {
    }

    record DefaultChanged(Profile profile, @Nullable PackSelection packs) implements ProfileMutation {
    }

    record LockToggled(Profile profile) implements ProfileMutation {
    }
}
