package io.github.fishstiz.packed_packs.api.events;

import io.github.fishstiz.packed_packs.api.context.ScreenContext;

/**
 * Base class for all events related to the pack selection screen.
 *
 * @see InitializeLayoutEvent
 * @see InitializePackEntryEvent
 * @see ClosingEvent
 * @see ContextMenuEvent
 * @see WatchEvent
 */
public abstract class ScreenEvent {
    private final ScreenContext context;

    ScreenEvent(ScreenContext context) {
        this.context = context;
    }

    /**
     * @return the context associated with the screen firing this event.
     */
    public final ScreenContext screenContext() {
        return this.context;
    }
}
