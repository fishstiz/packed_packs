package io.github.fishstiz.packed_packs.gui.actions.intents;

import io.github.fishstiz.packed_packs.config.Profile;
import org.jspecify.annotations.Nullable;

public sealed interface Intent permits Intent.Reset, PackListIntent, ProfileIntent {
    record Reset(@Nullable Profile profile) implements Intent {
        public Reset() {
            this(null);
        }
    }
}
