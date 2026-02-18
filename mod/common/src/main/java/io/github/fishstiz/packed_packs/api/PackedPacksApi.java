package io.github.fishstiz.packed_packs.api;

import org.jetbrains.annotations.ApiStatus;

/**
 * The central gateway providing access to API services passed to entry points during initialization.
 * <p>
 * This interface and its members under the {@code api} package
 * constitute the stable public API. While internal packages are visible, they are
 * considered unstable and should be used with caution; any package
 * not explicitly under the {@code api} hierarchy is subject to breaking changes.
 */
@ApiStatus.NonExtendable
public interface PackedPacksApi {
    /**
     * @return the registry for defining and accessing configuration values.
     */
    PreferenceRegistry preferences();

    /**
     * @return the bus for subscribing to and posting events.
     */
    EventBus eventBus();
}
