package io.github.fishstiz.packed_packs.gui.components;

import io.github.fishstiz.fidgetz.v0.gui.renderables.RenderableRectangle;
import io.github.fishstiz.fidgetz.v0.gui.renderables.Renderables;

public interface DropCandidate {
    boolean valid();

    RenderableRectangle badge();

    default DropCandidate orElse(DropCandidate candidate) {
        return valid() ? this : candidate;
    }

    enum Result implements DropCandidate {
        VALID,
        INVALID;

        @Override
        public boolean valid() {
            return this == VALID;
        }

        @Override
        public RenderableRectangle badge() {
            return Renderables.empty();
        }

        public static Result from(boolean valid) {
            return valid ? VALID : INVALID;
        }
    }
}
