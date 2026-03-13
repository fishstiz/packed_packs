package io.github.fishstiz.packed_packs.api.gui;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * The context menu item specifications.
 */
@ApiStatus.NonExtendable
public interface ContextMenuItemSpec {
    /**
     * Sets the display text of the item.
     */
    ContextMenuItemSpec label(Component label);

    /**
     * Sets the action to execute when the item is clicked.
     */
    ContextMenuItemSpec action(Runnable action);

    /**
     * Sets the sprite icon displayed next to the label. Uses the gui sprite path.
     */
    ContextMenuItemSpec icon(Identifier guiSprite);

    /**
     * Sets the tooltip displayed when hovering over the item.
     */
    ContextMenuItemSpec tooltip(Tooltip tooltip);

    /**
     * Sets the active state of the context menu item.
     */
    ContextMenuItemSpec active(BooleanSupplier active);

    /**
     * Sets whether the context menu should close when this item is interacted with.
     * <p>
     * Defaults to {@code true}.
     */
    ContextMenuItemSpec closeOnInteract(boolean closeOnInteract);

    /**
     * Adds a child item to create a sub-menu.
     */
    ContextMenuItemSpec child(Consumer<ContextMenuItemSpec> configurator);

    /**
     * Adds multiple child items from an iterable.
     */
    default <E> ContextMenuItemSpec children(Iterable<E> iterable, BiConsumer<E, ContextMenuItemSpec> configurator) {
        for (E entry : iterable) {
            this.child(child -> configurator.accept(entry, child));
        }
        return this;
    }

    /**
     * Adds multiple child items from an array.
     */
    default <E> ContextMenuItemSpec children(E[] array, BiConsumer<E, ContextMenuItemSpec> configurator) {
        for (E entry : array) {
            this.child(child -> configurator.accept(entry, child));
        }
        return this;
    }

    /**
     * Adds a horizontal separator line below this item.
     */
    ContextMenuItemSpec separatorBelow();

    /**
     * Adds a horizontal separator line above this item.
     */
    ContextMenuItemSpec separatorAbove();

    /**
     * Adds a horizontal separator line above and below this item.
     */
    default ContextMenuItemSpec separators() {
        return this.separatorAbove().separatorBelow();
    }

    /**
     * Automatically sets the {@link #action(Runnable) action} to flip the state,
     * and the {@link #icon(Identifier) icon} to reflect the current value.
     */
    ContextMenuItemSpec asToggle(BooleanSupplier value, BooleanConsumer onChange);

    /**
     * Applies a distinct visual style intended for developer mode.
     */
    ContextMenuItemSpec applyDevStyle();
}
