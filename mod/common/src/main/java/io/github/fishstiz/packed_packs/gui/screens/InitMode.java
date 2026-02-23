package io.github.fishstiz.packed_packs.gui.screens;

import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.pack.PackGroup;

public sealed interface InitMode {
    record Default() implements InitMode {
    }

    record WithProfile(Profile profile) implements InitMode {
    }

    record WithPacks(PackGroup packs) implements InitMode {
    }
}
