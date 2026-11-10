package io.github.fishstiz.packed_packs.gui.actions.intents;

import io.github.fishstiz.packed_packs.config.Profile;
import org.jetbrains.annotations.Nullable;

public sealed interface ProfileIntent extends Intent {
    record Select(@Nullable Profile profile) implements ProfileIntent {
    }

    record ToggleRenaming() implements ProfileIntent {
    }

    record Rename(Profile profile, String name, String description) implements ProfileIntent {
        public Rename(Profile profile, String name) {
            this(profile, name, profile.getDescription());
        }
    }

    record Delete(Profile profile) implements ProfileIntent {
    }

    record CopySelected() implements ProfileIntent {
    }

    record SetDefault(@Nullable Profile profile) implements ProfileIntent {
    }

    record ToggleLock(Profile profile) implements ProfileIntent {
    }

    record Move(Profile profile, int index) implements ProfileIntent {
    }
}
