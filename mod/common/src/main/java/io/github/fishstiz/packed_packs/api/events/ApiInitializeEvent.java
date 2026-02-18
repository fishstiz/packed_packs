package io.github.fishstiz.packed_packs.api.events;

import io.github.fishstiz.packed_packs.api.Event;
import io.github.fishstiz.packed_packs.api.PackedPacksApi;
import io.github.fishstiz.packed_packs.api.PackedPacksInitializer;
import org.jetbrains.annotations.ApiStatus;

/**
 * Fired after all {@link PackedPacksInitializer} extensions have been processed
 * and the API registries have been frozen.
 * <p>
 * This event is intended for post-initialization logic that requires
 * access to the fully populated and read-only API state.
 */
public final class ApiInitializeEvent implements Event {
    private final PackedPacksApi api;

    @ApiStatus.Internal
    public ApiInitializeEvent(PackedPacksApi api) {
        this.api = api;
    }

    /**
     * @return the fully initialized and frozen API instance.
     */
    public PackedPacksApi getApi() {
        return this.api;
    }
}
