package io.github.fishstiz.packed_packs.compat.minecraftcursor;

import io.github.fishstiz.minecraftcursor.api.CursorController;
import io.github.fishstiz.minecraftcursor.api.CursorType;
import io.github.fishstiz.packed_packs.compat.Mod;

public class MinecraftCursor {
    private static final MinecraftCursor INSTANCE = Mod.MINECRAFT_CURSOR.isLoaded() ? new Impl() : new MinecraftCursor();

    private MinecraftCursor() {
    }

    public static MinecraftCursor get() {
        return INSTANCE;
    }

    public void setGrabbing() {
    }

    public void setNotAllowed() {
    }

    public void setPointingHand() {
    }

    private static final class Impl extends MinecraftCursor {
        private Impl() {
        }

        @Override
        public void setGrabbing() {
            CursorController.getInstance().setSingleCycleCursor(CursorType.GRABBING);
        }

        @Override
        public void setNotAllowed() {
            CursorController.getInstance().setSingleCycleCursor(CursorType.NOT_ALLOWED);
        }

        @Override
        public void setPointingHand() {
            CursorController.getInstance().setSingleCycleCursor(CursorType.POINTER);
        }
    }
}
