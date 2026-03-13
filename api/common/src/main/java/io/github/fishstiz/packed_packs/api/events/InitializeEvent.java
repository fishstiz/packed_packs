package io.github.fishstiz.packed_packs.api.events;

import io.github.fishstiz.packed_packs.api.Event;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;

/**
 * Base class for screen initialize events.
 *
 * @see Pre
 * @see Post
 */
public abstract sealed class InitializeEvent extends ScreenEvent {
    InitializeEvent(ScreenContext context) {
        super(context);
    }

    /**
     * Fired on screen {@code init}, before everything else runs.
     */
    public static final class Pre extends InitializeEvent implements Event {
        private final Consumer<Consumer<Post>> postActionCollector;

        @ApiStatus.Internal
        public Pre(ScreenContext context, Consumer<Consumer<Post>> postActionCollector) {
            super(context);
            this.postActionCollector = postActionCollector;
        }

        /**
         * Registers a listener for post-initialization.
         * <p>
         * Runs before {@link Post} is posted to the event bus.
         */
        public void afterInit(Consumer<Post> listener) {
            this.postActionCollector.accept(listener);
        }
    }

    /**
     * Fired on screen {@code init}, after everything else runs.
     * <p>
     * Listeners of this event can also register via {@link Pre#afterInit}.
     */
    public static final class Post extends InitializeEvent implements Event {
        @ApiStatus.Internal
        public Post(ScreenContext context) {
            super(context);
        }
    }
}
