package io.github.fishstiz.packed_packs.api.events;

import io.github.fishstiz.packed_packs.api.Event;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import org.jetbrains.annotations.ApiStatus;

/**
 * Fired when the screen is closing.
 */
public final class ClosingEvent extends ScreenEvent implements Event {
    private boolean committed;

    @ApiStatus.Internal
    public ClosingEvent(ScreenContext context) {
        super(context);
    }

    /**
     * Marks that changes from this screen should be committed.
     * <p>
     * <b>Note:</b>
     * <ul>
     * <li>For <b>Data Packs</b>, this does not do anything as changes are always committed.</li>
     * <li>For <b>Resource Packs</b>, the resource reload may still be skipped
     * if Minecraft does not detect changes when updating the Resource Pack list.</li>
     * </ul>
     */
    public void commit() {
        this.committed = true;
    }

    public boolean isCommitted() {
        return this.committed;
    }
}
