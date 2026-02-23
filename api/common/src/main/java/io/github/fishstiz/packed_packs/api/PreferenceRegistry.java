package io.github.fishstiz.packed_packs.api;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * Registry for defining and accessing shared, persistent preference values.
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
        Identifier id();

        /**
         * @return the class type of the preference value
         */
        Class<T> type();
    }

    /**
     * Registers a new preference.
     */
    <T> Key<T> register(Identifier id, Class<T> type, T defaultValue);

    /**
     * Registers a new preference with a custom deserializer and serializer.
     */
    <T> Key<T> register(Identifier id, Class<T> type, T defaultValue, Function<String, T> deserializer, Function<T, String> serializer);

    /**
     * Registers a new preference with a custom deserializer. The value is serialized using {@link Object#toString()}
     */
    default <T> Key<T> register(Identifier id, Class<T> type, T defaultValue, Function<String, T> deserializer) {
        return this.register(id, type, defaultValue, deserializer, Object::toString);
    }

    /**
     * Registers a boolean preference
     */
    default Key<Boolean> register(Identifier id, boolean defaultValue) {
        return this.register(id, Boolean.class, defaultValue);
    }

    /**
     * Registers an integer preference.
     */
    default Key<Integer> register(Identifier id, int defaultValue) {
        return this.register(id, Integer.class, defaultValue);
    }

    /**
     * Registers a float preference.
     */
    default Key<Float> register(Identifier id, float defaultValue) {
        return this.register(id, Float.class, defaultValue);
    }

    /**
     * Registers a double preference.
     */
    default Key<Double> register(Identifier id, double defaultValue) {
        return this.register(id, Double.class, defaultValue);
    }

    /**
     * Registers a string preference.
     */
    default Key<String> register(Identifier id, String defaultValue) {
        return this.register(id, String.class, defaultValue);
    }

    /**
     * Updates a preference value.
     */
    <T> void set(Key<T> key, T value);

    /**
     * Retrieves a preference value or {@code null} if none is set.
     */
    <T> @Nullable T get(Key<T> key);

    /**
     * Updates a preference value using its raw identifier.
     */
    void setUnsafe(Identifier id, Object value);

    /**
     * Retrieves a preference value using its raw identifier.
     */
    @Nullable Object getUnsafe(Identifier key);
}
