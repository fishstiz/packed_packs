package io.github.fishstiz.packed_packs.api.gui;

import java.util.function.Consumer;

/**
 * A sink for adding items to a context menu.
 */
@FunctionalInterface
public interface ContextMenuSink {
    /**
     * Adds a custom item to the context menu.
     *
     * @param configurator a consumer used to configure the menu item
     */
    void addItem(Consumer<ContextMenuItemSpec> configurator);
}
