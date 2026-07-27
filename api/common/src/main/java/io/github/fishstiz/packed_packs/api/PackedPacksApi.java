package io.github.fishstiz.packed_packs.api;

import org.jetbrains.annotations.ApiStatus;

/**
 * Provides the Packed Packs services.
 */
@ApiStatus.NonExtendable
public interface PackedPacksApi {
    /**
     * @return the registry for defining and accessing shared preference values.
     */
    PreferenceRegistry preferences();

    /**
     * @return the bus for subscribing to and posting events.
     */
    EventBus eventBus();
}
