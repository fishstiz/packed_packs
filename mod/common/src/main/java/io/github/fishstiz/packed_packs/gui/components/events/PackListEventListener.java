package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.api.Event;
import io.github.fishstiz.packed_packs.api.events.ScreenContext;
import io.github.fishstiz.packed_packs.api.events.ScreenEvent;

public interface PackListEventListener {
    void onEvent(PackListEvent event);

    ScreenContext ctx();

    <T extends ScreenEvent & Event> void postApiEvent(T event);
}
