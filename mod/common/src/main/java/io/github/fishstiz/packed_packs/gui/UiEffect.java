package io.github.fishstiz.packed_packs.gui;

import io.github.fishstiz.packed_packs.gui.model.PackListType;
import org.jetbrains.annotations.Nullable;

public sealed interface UiEffect {
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
