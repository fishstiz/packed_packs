package io.github.fishstiz.packed_packs.gui.intents;

import io.github.fishstiz.packed_packs.gui.Intent;

public interface Operation extends Intent {
    Status status();
    
    default boolean resetHistory() {
        return true;
    }
}
