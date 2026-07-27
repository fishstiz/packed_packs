package io.github.fishstiz.packed_packs.api.events;

import io.github.fishstiz.packed_packs.api.Event;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Path;

/**
 * Fired when a file change is detected within a pack folder.
 * <p>
 * Used to prevent the pack repository from refreshing for specific paths.
 */
public final class WatchEvent extends ScreenEvent implements Event {
    private final Path path;
    private boolean canceled;

    @ApiStatus.Internal
    public WatchEvent(ScreenContext context, Path path) {
        super(context);
        this.path = path;
    }

    /**
     * @return the path of the file that was modified.
     */
    public Path getPath() {
        return this.path;
    }

    /**
     * Prevents the pack repository from refreshing in response to the file change.
     */
    public void cancel() {
        this.canceled = true;
    }

    public boolean isCanceled() {
        return this.canceled;
    }
}
