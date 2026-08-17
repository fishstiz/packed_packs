package io.github.fishstiz.packed_packs.gui.actions.intents;

import io.github.fishstiz.packed_packs.config.Profile;
import org.jspecify.annotations.Nullable;

public sealed interface ProfileIntent extends Intent {
    record Select(@Nullable Profile profile) implements ProfileIntent {
    }

    record ToggleRenaming() implements ProfileIntent {
    }

    record Rename(Profile profile, String name) implements ProfileIntent {
    }

    record Delete(Profile profile) implements ProfileIntent {
    }

    record CopySelected() implements ProfileIntent {
    }

    record SetDefault(@Nullable Profile profile) implements ProfileIntent {
    }

    record ToggleLock(Profile profile) implements ProfileIntent {
    }
}
