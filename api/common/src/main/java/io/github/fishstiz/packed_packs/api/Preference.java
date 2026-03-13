package io.github.fishstiz.packed_packs.api;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;

/**
 * Represents a registered preference value.
 * <p>
 * <b>Note:</b> Changes made via {@code set} are kept in memory and are only
 * saved to disk when the pack selection screen is closed.
 *
 * @param <T> the type of the value associated with this preference
 */
@ApiStatus.NonExtendable
public interface Preference<T> {
    /**
     * @return the unique identifier for this preference
     */
    Identifier id();

    /**
     * @return the class type of this preference value
     */
    Class<T> type();

    /**
     * @return the current value of this preference
     */
    T get();

    /**
     * Updates the value of this preference.
     */
    void set(T value);

    /**
     * @return the default value of this preference
     */
    T getDefault();

    /**
     * Resets this preference to its default value.
     */
    void reset();
}
