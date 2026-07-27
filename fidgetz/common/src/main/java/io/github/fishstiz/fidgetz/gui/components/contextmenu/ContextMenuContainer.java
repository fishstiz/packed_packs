package io.github.fishstiz.fidgetz.gui.components.contextmenu;

import net.minecraft.client.gui.components.events.ContainerEventHandler;

public interface ContextMenuContainer extends ContextMenuProvider, ContainerEventHandler {
    @Override
    default void buildItems(ContextMenuItemBuilder builder, int mouseX, int mouseY) {
        this.getChildAt(mouseX, mouseY)
                .filter(ContextMenuProvider.class::isInstance)
                .map(ContextMenuProvider.class::cast)
                .ifPresent(provider -> provider.buildItems(builder, mouseX, mouseY));
    }
}
