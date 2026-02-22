package io.github.fishstiz.packed_packs.api;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Registry for defining and accessing shared, persistent preference values.
 * <p>
 * Preferences must be registered during initialization before the registry is frozen.
 * Values are stored as strings and converted using the provided deserializers.
 * <p>
 * <b>Note:</b> Changes made via {@code set} are kept in memory and are only
 * saved to disk when the pack selection screen is closed.
 */
@ApiStatus.NonExtendable
public interface PreferenceRegistry {
    /**
     * A unique handle representing a registered preference.
     *
     * @param <T> the type of the value associated with this key
     */
    @ApiStatus.NonExtendable
    interface Key<T> {
        /**
         * @return the unique identifier for this preference
         */
        ResourceLocation id();

        /**
         * @return the class type of the preference value
         */
        Class<T> type();
    }

    /**
     * Registers a new preference with a custom deserializer.
     *
     * @throws IllegalStateException if called after the registry is frozen.
     */
    <T> Key<T> register(ResourceLocation id, Class<T> type, T defaultValue, Function<String, T> deserializer);

    /**
     * Registers a string-based preference.
     *
     * @throws IllegalStateException if called after the registry is frozen.
     */
    default Key<String> register(ResourceLocation id, String defaultValue) {
        return this.register(id, String.class, defaultValue, String::valueOf);
    }

    /**
     * Registers a boolean-based preference.
     *
     * @throws IllegalStateException if called after the registry is frozen.
     */
    default Key<Boolean> register(ResourceLocation id, boolean defaultValue) {
        return this.register(id, Boolean.class, defaultValue, Boolean::parseBoolean);
    }

    /**
     * Registers an integer-based preference.
     *
     * @throws IllegalStateException if called after the registry is frozen.
     */
    default Key<Integer> register(ResourceLocation id, int defaultValue) {
        return this.register(id, Integer.class, defaultValue, Integer::parseInt);
    }

    /**
     * Updates a preference value.
     *
     * @throws IllegalStateException if called during initialization.
     */
    <T> void set(Key<T> key, T value);

    /**
     * Retrieves a preference value or {@code null} if none is set.
     *
     * @throws IllegalStateException if called during initialization.
     */
    <T> @Nullable T get(Key<T> key);

    /**
     * Updates a preference value using its raw identifier.
     *
     * @throws IllegalStateException if called during initialization.
     */
    void setUnsafe(ResourceLocation id, Object value);

    /**
     * Retrieves a preference value using its raw identifier.
     *
     * @throws IllegalStateException if called during initialization.
     */
    @Nullable Object getUnsafe(ResourceLocation key);
}
