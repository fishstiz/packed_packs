package io.github.fishstiz.packed_packs.gui2.actions.intents;

public sealed interface Intent permits Intent.Reset, PackListIntent, ProfileIntent {
    record Reset() implements Intent {
    }
}
