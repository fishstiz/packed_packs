package io.github.fishstiz.packed_packs.api.events;

import io.github.fishstiz.packed_packs.api.context.PackContext;

/**
 * A common interface for events associated with a specific pack entry.
 */
public interface PackEntryEvent {
    /**
     * @return the context and metadata for the pack associated with this event.
     */
    PackContext packContext();
}
