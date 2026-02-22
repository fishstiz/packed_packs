package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.api.context.ScreenContext;

public interface PackListEventListener {
    void onEvent(PackListEvent event);

    ScreenContext ctx();
}
