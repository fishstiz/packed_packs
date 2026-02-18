package io.github.fishstiz.packed_packs.api.events;

import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.MenuItem;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.MenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.GuiSprite;
import io.github.fishstiz.packed_packs.api.Event;
import io.github.fishstiz.packed_packs.gui.components.ToggleableHelper;
import io.github.fishstiz.packed_packs.gui.components.pack.PackList;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Base class for all events related to the pack selection screen.
 */
public class ScreenEvent {
    protected final ScreenContext context;

    protected ScreenEvent(ScreenContext context) {
        this.context = context;
    }

    /**
     * @return the context associated with the screen firing this event.
     */
    public ScreenContext ctx() {
        return this.context;
    }

    /**
     * Fired during the screen's layout initialization.
     * Used to inject custom UI elements into specific screen regions
     * <p>
     * Elements added via this event are automatically positioned within the bounds
     * and layout constraints of their respective target areas.
     */
    public static class InitLayout extends ScreenEvent implements Event {
        private final BiConsumer<Phase, LayoutElement> add;

        public enum Phase {
            /**
             * After the title in the header. Most common injection phase for mod extensions.
             */
            AFTER_HEADER_TITLE,

            /**
             * At the very start of the footer's left column.
             */
            BEFORE_FOOTER,

            /**
             * After the 'Open Folder' button in the footer, constrained to the left column.
             */
            AFTER_FOOTER_OPEN_FOLDER,

            /**
             * At the very end of the footer's right column, after the 'Done' button.
             */
            AFTER_FOOTER
        }

        @ApiStatus.Internal
        public InitLayout(ScreenContext context, BiConsumer<Phase, LayoutElement> add) {
            super(context);
            this.add = add;
        }

        /**
         * Injects an element into the layout at the specified phase.
         */
        public void addElement(Phase phase, LayoutElement element) {
            this.add.accept(phase, element);
        }
    }

    /**
     * Fired when a pack entry is created.
     * Used to add custom widgets (like status icons or buttons) directly to entries.
     */
    public static class InitPackEntry extends ScreenEvent implements Event {
        private final PackList.Entry entry;

        @ApiStatus.Internal
        public InitPackEntry(ScreenContext context, PackList.Entry entry) {
            super(context);
            this.entry = entry;
        }

        /**
         * Adds a widget to the top layer of the entry.
         */
        public <T extends GuiEventListener & Renderable> void addWidget(T widget) {
            this.entry.addTopRenderableOnly(this.entry.prependWidget(widget));
        }

        /**
         * Returns the entry container.
         * <p>
         * Used to calculate relative positioning for added widgets.
         */
        public LayoutElement getContainer() {
            return this.entry;
        }

        public Pack getPack() {
            return this.entry.pack();
        }

        /**
         * @return {@code true} if the pack is restricted from modifications or
         * file-system operations within the current context.
         */
        public boolean isFileLocked() {
            return !this.entry.canOperateFile();
        }
    }

    /**
     * Fired when a file change is detected in the pack folder.
     * Can be used to prevent the pack repository from refreshing for specific paths.
     */
    public static class FileWatch extends ScreenEvent implements Event {
        private final Path path;
        private boolean canceled = false;

        @ApiStatus.Internal
        public FileWatch(ScreenContext context, Path path) {
            super(context);
            this.path = path;
        }

        public Path getPath() {
            return this.path;
        }

        /**
         * Cancels the pack repository refresh triggered by this file change.
         */
        public void cancel() {
            this.canceled = true;
        }

        public boolean isCanceled() {
            return this.canceled;
        }
    }

    /**
     * Fired when the screen is closing.
     * Allows performing logic or force committing changes before the screen is closed.
     */
    public static class Closing extends ScreenEvent implements Event {
        private boolean commited = false;

        public Closing(ScreenContext context) {
            super(context);
        }

        /**
         * Marks that changes from this screen should be committed.
         */
        public void commit() {
            this.commited = true;
        }

        public boolean isCommited() {
            return this.commited;
        }
    }

    /**
     * Represents a context menu being built.
     * Used to inject custom context menu items.
     */
    public static class CtxMenu extends ScreenEvent {
        private final ContextMenuItemBuilder builder;

        CtxMenu(ScreenContext context, ContextMenuItemBuilder builder) {
            super(context);
            this.builder = builder;
        }

        @ApiStatus.Experimental
        public final ContextMenuItemBuilder getBuilder() {
            return this.builder;
        }

        private static void add(
                ScreenContext context,
                ContextMenuItemBuilder builder,
                Component text,
                @Nullable Identifier sprite,
                @Nullable Runnable onClick,
                @Nullable Consumer<CtxMenu> onCreateChildren
        ) {
            MenuItemBuilder itemBuilder = MenuItem.builder(text);

            if (sprite != null) {
                itemBuilder.icon(new GuiSprite(sprite, 16, 16));
            }

            if (onCreateChildren != null) {
                ContextMenuItemBuilder subMenuBuilder = new ContextMenuItemBuilder();
                onCreateChildren.accept(new CtxMenu(context, subMenuBuilder));
                itemBuilder.addChildren(subMenuBuilder.build());
            } else if (onClick != null) {
                itemBuilder.action(onClick);
            }

            if (context.isDevMode()) {
                itemBuilder.background(GuiConstants.DEVELOPER_MODE_ITEM_BACKGROUND);
            }

            builder.add(itemBuilder.build());
        }

        private static void addToggle(
                ContextMenuItemBuilder builder,
                Component text,
                BooleanSupplier valueSupplier,
                BooleanConsumer onChange
        ) {
            builder.add(MenuItem.builder(text)
                    .icon(() -> ToggleableHelper.getDefaultIcon(valueSupplier.getAsBoolean()))
                    .action(() -> onChange.accept(!valueSupplier.getAsBoolean()))
                    .build());
        }

        public void add(Component text, Runnable onClick) {
            add(this.context, this.builder, text, null, onClick, null);
        }

        public void add(Component text, Identifier sprite, Runnable onClick) {
            add(this.context, this.builder, text, sprite, onClick, null);
        }

        public void addParent(Component text, Consumer<CtxMenu> onCreateChildren) {
            add(this.context, this.builder, text, null, null, onCreateChildren);
        }

        public void addParent(Component text, Identifier sprite, Consumer<CtxMenu> onCreateChildren) {
            add(this.context, this.builder, text, sprite, null, onCreateChildren);
        }

        public void addToggle(Component text, BooleanSupplier valueSupplier, BooleanConsumer onChange) {
            addToggle(this.builder, text, valueSupplier, onChange);
        }
    }

    abstract static class PhasedMenu<P extends Enum<P>> extends ScreenEvent {
        private final Function<P, ContextMenuItemBuilder> builderFactory;

        PhasedMenu(ScreenContext context, Function<P, ContextMenuItemBuilder> builderFactory) {
            super(context);
            this.builderFactory = builderFactory;
        }

        @ApiStatus.Experimental
        public final ContextMenuItemBuilder getBuilder(P phase) {
            return builderFactory.apply(phase);
        }

        public void add(P phase, Component text, Runnable onClick) {
            CtxMenu.add(this.context, this.builderFactory.apply(phase), text, null, onClick, null);
        }

        public void add(P phase, Component text, Identifier sprite, Runnable onClick) {
            CtxMenu.add(this.context, this.builderFactory.apply(phase), text, sprite, onClick, null);
        }

        public void addParent(P phase, Component text, Consumer<CtxMenu> onCreateChildren) {
            CtxMenu.add(this.context, this.builderFactory.apply(phase), text, null, null, onCreateChildren);
        }

        public void addParent(P phase, Component text, Identifier sprite, Consumer<CtxMenu> onCreateChildren) {
            CtxMenu.add(this.context, this.builderFactory.apply(phase), text, sprite, null, onCreateChildren);
        }

        public void addToggle(P phase, Component text, BooleanSupplier valueSupplier, BooleanConsumer onChange) {
            CtxMenu.addToggle(this.builderFactory.apply(phase), text, valueSupplier, onChange);
        }
    }

    /**
     * Fired when the context menu is opened.
     */
    public static class OpenCtxMenu extends PhasedMenu<OpenCtxMenu.Phase> implements Event {
        public enum Phase {
            /**
             * Top of the menu.
             */
            BEFORE_ALL,

            /**
             * Within the preferences sub menu.
             */
            PREFERENCES,

            /**
             * Bottom of the menu.
             */
            AFTER_ALL,
        }

        @ApiStatus.Internal
        public OpenCtxMenu(ScreenContext context, Function<Phase, ContextMenuItemBuilder> builderFactory) {
            super(context, builderFactory);
        }

        /**
         * Fired when a context menu is opened specifically for a pack entry.
         */
        public static class PackEntry extends PhasedMenu<PackEntry.Phase> implements Event {
            private final PackList.Entry entry;

            public enum Phase {
                BEFORE_ALL,
                AFTER_HEADER,

                /**
                 * After developer-specific actions.
                 */
                AFTER_DEV,
                AFTER_ALL;
            }

            @ApiStatus.Internal
            public PackEntry(ScreenContext context, PackList.Entry entry, Function<Phase, ContextMenuItemBuilder> builderFactory) {
                super(context, builderFactory);
                this.entry = entry;
            }

            public Pack getPack() {
                return this.entry.pack();
            }

            /**
             * @return {@code true} if the pack is restricted from modifications or
             * file-system operations within the current context.
             */
            public boolean isFileLocked() {
                return !this.entry.canOperateFile();
            }
        }
    }
}
