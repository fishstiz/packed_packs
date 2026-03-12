package io.github.fishstiz.packed_packs.gui.intents;

import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.gui.Intent;
import io.github.fishstiz.packed_packs.pack.PackGroup;
import org.jspecify.annotations.Nullable;

public sealed interface ProfileIntent extends Intent {
    @Override
    default boolean pushState() {
        return false;
    }

    @Override
    default boolean resetHistory() {
        return true;
    }

    record SelectNone(PackGroup packs) implements ProfileIntent {
    }

    record Select(Profile profile) implements ProfileIntent {
    }

    record Rename(Profile profile, String name) implements ProfileIntent {
        @Override
        public boolean resetHistory() {
            return false;
        }
    }

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

    record Add(Profile profile) implements ProfileIntent {
    }

    record SetDefault(@Nullable Profile profile) implements ProfileIntent {
    }

    record ToggleLock(Profile profile) implements ProfileIntent {
    }
}
