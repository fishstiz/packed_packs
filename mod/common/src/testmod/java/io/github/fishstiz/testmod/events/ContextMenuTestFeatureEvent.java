package io.github.fishstiz.testmod.events;

import io.github.fishstiz.packed_packs.api.Event;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.api.events.ContextMenuEvent;
import io.github.fishstiz.packed_packs.api.gui.ContextMenuItemSpec;

import java.util.function.Consumer;

public final class ContextMenuTestFeatureEvent extends ContextMenuEvent implements Event {
    private final ContextMenuItemSpec parent;
    private boolean hasItem;

    public ContextMenuTestFeatureEvent(ScreenContext context, ContextMenuItemSpec parent) {
        super(context);
        this.parent = parent;
    }

    @Override
    public void addItem(Consumer<ContextMenuItemSpec> configurator) {
        parent.child(configurator);
        hasItem = true;
    }

    public boolean hasChildren() {
        return hasItem;
    }
}
