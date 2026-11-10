package io.github.fishstiz.packed_packs.gui;

import io.github.fishstiz.fidgetz.v0.gui.components.FZDialog;
import io.github.fishstiz.fidgetz.v0.gui.components.FZDialogContainer;
import io.github.fishstiz.packed_packs.gui.states.PackListType;
import org.jspecify.annotations.Nullable;

import java.util.function.BiFunction;

public sealed interface UiEffect {
    // runnable = dialog closer
    record OpenDialog(BiFunction<FZDialogContainer, Runnable, FZDialog> factory) implements UiEffect {
    }

    record ScrollToTop(PackListType type) implements UiEffect {
    }

    record ScrollToLastSelected(PackListType type) implements UiEffect {
    }

    record FocusList(PackListType type) implements UiEffect {
    }

    record Focus(PackListType type, @Nullable String packId, boolean scroll) implements UiEffect {
        public Focus(PackListType type, boolean scroll) {
            this(type, null, scroll);
        }

        public Focus(PackListType type, @Nullable String packId) {
            this(type, packId, false);
        }

        public Focus(PackListType type) {
            this(type, null);
        }
    }
}
