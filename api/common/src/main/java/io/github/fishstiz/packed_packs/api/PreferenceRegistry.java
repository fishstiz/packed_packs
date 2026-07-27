package io.github.fishstiz.packed_packs.api;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.Optional;
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
     * Registers a new preference.
     */
    <T> Preference<T> register(ResourceLocation id, Class<T> type, T defaultValue);

    /**
     * Registers a new preference with a custom deserializer and serializer.
     */
    <T> Preference<T> register(ResourceLocation id, Class<T> type, T defaultValue, Function<String, T> deserializer, Function<T, String> serializer);

    /**
     * Registers a boolean preference
     */
    default Preference<Boolean> register(ResourceLocation id, boolean defaultValue) {
        return this.register(id, Boolean.class, defaultValue);
    }

    /**
     * Registers an integer preference.
     */
    default Preference<Integer> register(ResourceLocation id, int defaultValue) {
        return this.register(id, Integer.class, defaultValue);
    }

    /**
     * Registers a float preference.
     */
    default Preference<Float> register(ResourceLocation id, float defaultValue) {
        return this.register(id, Float.class, defaultValue);
    }

    /**
     * Registers a double preference.
     */
    default Preference<Double> register(ResourceLocation id, double defaultValue) {
        return this.register(id, Double.class, defaultValue);
    }

    /**
     * Registers a string preference.
     */
    default Preference<String> register(ResourceLocation id, String defaultValue) {
        return this.register(id, String.class, defaultValue);
    }

    /**
     * Looks up a preference by id.
     * Empty if no preference is registered with the given id.
     */
    Optional<Preference<?>> find(ResourceLocation id);

    /**
     * Looks up a preference by id with an expected type.
     * Empty if no preference is registered with the given id, or if the registered type
     * is not assignable to the requested type.
     */
    <T> Optional<Preference<T>> find(ResourceLocation id, Class<T> type);
}
