package io.github.fishstiz.packed_packs.api;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.function.Consumer;

/**
 * A synchronous event dispatcher used to register and post {@link Event} instances.
 * <p>
 * Listeners are sorted via topological sort based on their dependencies before the bus is frozen.
 * <p>
 * Registration is only permitted during the API initialization phase.
 */
@ApiStatus.NonExtendable
public interface EventBus {
    /**
     * Registers a listener for a specific event class.
     *
     * @throws IllegalStateException if called after initialization.
     */
    <T extends Event> void register(Class<T> eventClass, Identifier id, Consumer<T> listener);

    /**
     * Registers a listener that must execute after the specified dependency.
     *
     * @throws IllegalStateException if called after initialization.
     */
    <T extends Event> void register(Class<T> eventClass, Identifier id, Identifier loadAfter, Consumer<T> listener);

    /**
     * Registers a listener that must execute after multiple specified dependencies.
     *
     * @throws IllegalStateException if called after initialization.
     */
    <T extends Event> void register(Class<T> eventClass, Identifier id, List<Identifier> loadAfter, Consumer<T> listener);

    /**
     * Dispatches an event to all registered listeners for its class.
     *
     * @throws IllegalStateException if called during initialization.
     */
    void post(Event event);
}
