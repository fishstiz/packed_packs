package io.github.fishstiz.packed_packs.api.events;

import io.github.fishstiz.packed_packs.api.Event;
import io.github.fishstiz.packed_packs.api.context.PackContext;
import io.github.fishstiz.packed_packs.api.PreferenceRegistry;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Base class for events that allow adding items to a context menu.
 *
 * @see Screen
 * @see Preferences
 * @see PackEntry
 */
@ApiStatus.NonExtendable
public abstract class ContextMenuEvent<P extends Enum<P>> extends ScreenEvent {
    protected ContextMenuEvent(ScreenContext context) {
        super(context);
    }

    /**
     * The context menu item configuration.
     */
    @ApiStatus.NonExtendable
    public interface Item {
        /**
         * Sets the display text of the item.
         */
        Item setLabel(Component label);

        /**
         * Sets the action to execute when the item is clicked.
         */
        Item setAction(Runnable action);

        /**
         * Sets the sprite icon displayed next to the label.
         */
        Item setIcon(Identifier sprite);

        /**
         * Sets the tooltip displayed when hovering over the item.
         */
        Item setTooltip(Tooltip tooltip);

        /**
         * Creates a sub-menu for this item.
         */
        Item addChild(Consumer<Item> item);

        /**
         * Adds a horizontal separator line below this item.
         */
        Item addSeparatorBelow();

        /**
         * Adds a horizontal separator line above this item.
         */
        Item addSeparatorAbove();

        /**
         * Applies a distinct visual style intended for developer mode.
         */
        Item applyDevStyle();
    }

    /**
     * Adds a custom item to the menu at the specified position.
     *
     * @param pos  The placement position.
     * @param item A consumer to configure the {@link Item}.
     */
    public abstract void addItem(P pos, Consumer<Item> item);

    /**
     * Adds a toggleable (checkbox) item to the menu at the specified position.
     *
     * @param pos           The placement position.
     * @param label         The text to display.
     * @param valueSupplier Provides the current state of the toggle.
     * @param onChange      Called when the toggle state is changed by the user.
     */
    public abstract void addToggle(P pos, Component label, BooleanSupplier valueSupplier, BooleanConsumer onChange);

    private abstract static class DelegatedContextMenuEvent<P extends Enum<P>> extends ContextMenuEvent<P> {
        final ContextMenuEvent<P> delegate;

        DelegatedContextMenuEvent(ContextMenuEvent<P> delegate) {
            super(delegate.screenContext());
            this.delegate = delegate;
        }

        abstract P lastPos();

        /**
         * Adds a custom item to the menu at the specified position.
         *
         * @param pos  the placement position within the menu
         * @param item a consumer used to configure the menu item
         */
        public void addItem(P pos, Consumer<Item> item) {
            this.delegate.addItem(pos, item);
        }

        /**
         * Adds a custom item to the menu at the default position.
         *
         * @param item a consumer used to configure the menu item
         */
        public void addItem(Consumer<Item> item) {
            this.addItem(this.lastPos(), item);
        }

        /**
         * Adds a toggleable (checkbox) item to the menu at the specified position.
         *
         * @param pos           the placement position within the menu
         * @param label         the display text for the toggle item
         * @param valueSupplier a supplier providing the current boolean state
         * @param onChange      a consumer called with the new state when the user interacts with the toggle
         */
        public void addToggle(P pos, Component label, BooleanSupplier valueSupplier, BooleanConsumer onChange) {
            this.delegate.addToggle(pos, label, valueSupplier, onChange);
        }

        /**
         * Adds a toggleable (checkbox) item to the menu at the default position.
         *
         * @param label         the display text for the toggle item
         * @param valueSupplier a supplier providing the current boolean state
         * @param onChange      a consumer called with the new state when the user interacts with the toggle
         */
        public void addToggle(Component label, BooleanSupplier valueSupplier, BooleanConsumer onChange) {
            this.addToggle(this.lastPos(), label, valueSupplier, onChange);
        }
    }

    /**
     * Fired when the global screen context menu is being constructed.
     */
    public static final class Screen extends DelegatedContextMenuEvent<Screen.Pos> implements Event {
        public enum Pos {
            /**
             * The top of the context menu.
             */
            TOP,
            /**
             * The bottom of the context menu.
             */
            BOTTOM
        }

        @ApiStatus.Internal
        public Screen(ContextMenuEvent<Screen.Pos> delegate) {
            super(delegate);
        }

        @Override
        Pos lastPos() {
            return Pos.BOTTOM;
        }
    }

    /**
     * Fired when the Preferences sub menu is being constructed.
     */
    public static final class Preferences extends DelegatedContextMenuEvent<Preferences.Pos> implements Event {
        private final PreferenceRegistry registry;

        public enum Pos {
            /**
             * Above the standard preferences.
             */
            TOP,
            /**
             * Below the standard preferences.
             */
            BOTTOM
        }

        @ApiStatus.Internal
        public Preferences(PreferenceRegistry registry, ContextMenuEvent<Preferences.Pos> delegate) {
            super(delegate);
            this.registry = registry;
        }

        /**
         * Links a boolean preference directly to a menu toggle.
         *
         * @param pos   The placement position.
         * @param key   The specific preference key.
         * @param label The display text.
         */
        public void addToggle(Pos pos, PreferenceRegistry.Key<Boolean> key, Component label) {
            this.addToggle(pos, label, () -> Boolean.TRUE.equals(this.registry.get(key)), value -> this.registry.set(key, value));
        }

        /**
         * Links a boolean preference to a menu toggle positioned below the standard preferences.
         */
        public void addToggle(PreferenceRegistry.Key<Boolean> key, Component label) {
            this.addToggle(this.lastPos(), key, label);
        }

        @Override
        Pos lastPos() {
            return Pos.BOTTOM;
        }
    }

    /**
     * Fired when a context menu for a specific pack entry is being constructed.
     */
    public static final class PackEntry extends DelegatedContextMenuEvent<PackEntry.Pos> implements PackEntryEvent, Event {
        private final PackContext packContext;

        public enum Pos {
            /**
             * Above the entry header.
             */
            BEFORE_HEADER,
            /**
             * Directly below the entry header.
             */
            AFTER_HEADER,
            /**
             * After the developer mode options.
             */
            AFTER_DEV,
            /**
             * At the end of pack options.
             */
            AFTER_PACK
        }

        @ApiStatus.Internal
        public PackEntry(ContextMenuEvent<PackEntry.Pos> delegate, PackContext packContext) {
            super(delegate);
            this.packContext = packContext;
        }

        /**
         * Returns the context of the pack associated with this menu.
         */
        @Override
        public PackContext packContext() {
            return this.packContext;
        }

        @Override
        Pos lastPos() {
            return Pos.AFTER_PACK;
        }
    }
}
