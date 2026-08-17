package io.github.fishstiz.packed_packs.gui2.intents;

import io.github.fishstiz.packed_packs.gui2.models.PackEntryLists;

public sealed interface Intent permits Intent.Reset, PackListIntent, ProfileIntent {
    record Reset(PackEntryLists packs) implements Intent {
    }
}
