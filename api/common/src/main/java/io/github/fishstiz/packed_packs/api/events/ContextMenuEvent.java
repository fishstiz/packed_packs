package io.github.fishstiz.packed_packs.api.events;

import io.github.fishstiz.packed_packs.api.Event;
import io.github.fishstiz.packed_packs.api.Preference;
import io.github.fishstiz.packed_packs.api.context.PackContext;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.api.gui.ContextMenuItemSpec;
import io.github.fishstiz.packed_packs.api.gui.ContextMenuSink;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;

/**
 * Base class for events that allow adding items to a context menu.
 *
 * @see Screen
 * @see Preferences
 * @see PackEntry
 */
public abstract class ContextMenuEvent extends ScreenEvent implements ContextMenuSink {
    protected ContextMenuEvent(ScreenContext context) {
        super(context);
    }

    /**
     * Base class for events that allow adding items to a context menu at certain positions.
     */
    public abstract static class Positioned<P extends Enum<P>> extends ContextMenuEvent {
        protected Positioned(ScreenContext context) {
            super(context);
        }

        protected abstract P defaultPosition();

        /**
         * Adds a custom item to the menu at the specified position.
         *
         * @param pos          The placement position.
         * @param configurator A consumer to configure the {@link ContextMenuItemSpec}.
         */
        public abstract void addItem(P pos, Consumer<ContextMenuItemSpec> configurator);

        @Override
        public void addItem(Consumer<ContextMenuItemSpec> configurator) {
            this.addItem(this.defaultPosition(), configurator);
        }
    }

    private abstract static class DelegatedPositioned<P extends Enum<P>> extends Positioned<P> {
        private final Positioned<P> delegate;

        DelegatedPositioned(Positioned<P> delegate) {
            super(delegate.screenContext());
            this.delegate = delegate;
        }

        public void addItem(P pos, Consumer<ContextMenuItemSpec> configurator) {
            this.delegate.addItem(pos, configurator);
        }
    }

    /**
     * Fired when the global screen context menu is being constructed.
     */
    public static final class Screen extends DelegatedPositioned<Screen.Pos> implements Event {
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
        public Screen(Positioned<Screen.Pos> delegate) {
            super(delegate);
        }

        @Override
        protected Pos defaultPosition() {
            return Pos.BOTTOM;
        }
    }

    /**
     * Fired when the Preferences sub menu is being constructed.
     */
    public static final class Preferences extends DelegatedPositioned<Preferences.Pos> implements Event {
        private final Consumer<Preference<?>> includeForReset;

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
        public Preferences(Positioned<Preferences.Pos> delegate, Consumer<Preference<?>> includeForReset) {
            super(delegate);
            this.includeForReset = includeForReset;
        }

        @Override
        public void addItem(Pos pos, Consumer<ContextMenuItemSpec> configurator) {
            super.addItem(pos, configurator.andThen(ContextMenuItemSpec::applyDevStyle));
        }

        /**
         * Configures an item as a toggle linked to the specified boolean preference.
         * <p>
         * Automatically sets the action and icon to reflect and control the preference state.
         *
         * @param item       the item to configure
         * @param preference the preference to link
         * @return the configured item
         */
        public ContextMenuItemSpec applyToggle(ContextMenuItemSpec item, Preference<Boolean> preference) {
            return item.asToggle(preference::get, preference::set).closeOnInteract(false);
        }

        /**
         * Adds a preference toggle to the menu at the specified position.
         *
         * @param pos        The placement position.
         * @param preference The specific preference.
         * @param label      The display text.
         */
        public void addToggle(Pos pos, Preference<Boolean> preference, Component label) {
            this.includeForReset(preference);
            this.addItem(pos, item -> this.applyToggle(item, preference).label(label));
        }

        /**
         * Adds a preference toggle at the default position.
         */
        public void addToggle(Preference<Boolean> key, Component label) {
            this.addToggle(this.defaultPosition(), key, label);
        }

        /**
         * Includes the preference in the `Reset Preferences` option.
         * <p>
         * Preferences registered via {@link #addToggle} are automatically included.
         *
         * @param preference the preference to include.
         */
        public void includeForReset(Preference<Boolean> preference) {
            this.includeForReset.accept(preference);
        }

        @Override
        protected Pos defaultPosition() {
            return Pos.BOTTOM;
        }
    }

    /**
     * Fired when a context menu for a specific pack entry is being constructed.
     */
    public static final class PackEntry extends DelegatedPositioned<PackEntry.Pos> implements PackEntryEvent, Event {
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
        public PackEntry(Positioned<PackEntry.Pos> delegate, PackContext packContext) {
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
        protected Pos defaultPosition() {
            return Pos.AFTER_PACK;
        }
    }
}
