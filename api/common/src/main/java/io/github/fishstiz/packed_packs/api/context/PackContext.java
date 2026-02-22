package io.github.fishstiz.packed_packs.api.context;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.ApiStatus;

/**
 * Provides contextual information about a specific resource or data pack.
 */
@ApiStatus.NonExtendable
public interface PackContext {
    /**
     * @return the underlying pack instance associated with this context
     */
    Pack pack();

    Identifier icon();

    /**
     * Determines if the underlying file for this pack is modifiable.
     * <p>
     * If {@code true}, UI elements that involve editing, renaming, or deleting
     * the pack file should be enabled.
     *
     * @return {@code true} if the pack file can be modified, {@code false} otherwise
     */
    boolean fileModifiable();
}
