package io.github.fishstiz.packed_packs.gui2.intents;

import org.jspecify.annotations.Nullable;

public sealed interface ProfileIntent extends Intent {
    record Select(@Nullable String id) implements ProfileIntent {
    }

    record ToggleRenaming() implements ProfileIntent {
    }

    record Rename(String id, String name) implements ProfileIntent {
    }

    record Delete(String id) implements ProfileIntent {
    }

    record CopySelected() implements ProfileIntent {
    }

    record SetDefault(@Nullable String id) implements ProfileIntent {
    }

    record ToggleLock(String id) implements ProfileIntent {
    }
}
