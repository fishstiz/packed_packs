package io.github.fishstiz.packed_packs.gui.intents;

import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.gui.Intent;
import io.github.fishstiz.packed_packs.pack.PackGroup;
import org.jspecify.annotations.Nullable;
// everything should be id-based
public sealed interface ProfileIntent extends Intent {
    @Override
    default boolean pushState() {
        return false;
    }

    @Override
    default boolean resetHistory() {
        return true;
    }

    // intent/requires services to fetch selected packs -> add action
    record SelectNone(PackGroup packs) implements ProfileIntent {
    }

    // intent/requires services to fetch selected packs -> add action + expose pack entries instead
    record Select(Profile profile) implements ProfileIntent {
    }

    // convert to action
    record ToggleRenaming() implements ProfileIntent {
        @Override
        public boolean resetHistory() {
            return false;
        }
    }

    // action with effect
    record Rename(Profile profile, String name) implements ProfileIntent {
        @Override
        public boolean resetHistory() {
            return false;
        }
    }

    //
    record Delete(Profile profile, @Nullable PackGroup fallback, Status status) implements ProfileIntent, Operation {
        public Delete(Profile profile) {
            this(profile, null, Status.LOADING);
        }

        public Delete withFail() {
            return new Delete(this.profile, this.fallback, Status.FAILURE);
        }

        public Delete withSuccess(PackGroup fallback) {
            return new Delete(this.profile, fallback, Status.SUCCESS);
        }

        @Override
        public boolean resetHistory() {
            return true;
        }
    }

    // convert to copy
    record Add(Profile profile) implements ProfileIntent {
    }

    record SetDefault(@Nullable Profile profile) implements ProfileIntent {
    }

    record ToggleLock(Profile profile) implements ProfileIntent {
    }
}
