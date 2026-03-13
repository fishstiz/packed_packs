package io.github.fishstiz.packed_packs.impl.gui;

import io.github.fishstiz.fidgetz.gui.components.ContainedWidget;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuContainer;
import io.github.fishstiz.packed_packs.api.gui.ContextMenuSink;
import net.minecraft.client.gui.components.AbstractWidget;

import java.util.function.BiConsumer;

public class ContextMenuWrappedWidget<T extends AbstractWidget> extends ContainedWidget implements ContextMenuContainer {
    private final BiConsumer<T, ContextMenuSink> configurator;

    public ContextMenuWrappedWidget(T widget, BiConsumer<T, ContextMenuSink> configurator) {
        super(widget);
        this.configurator = configurator;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void buildItems(ContextMenuItemBuilder builder, int mouseX, int mouseY) {
        this.configurator.accept((T) this.widget, itemConfigurator -> {
            ContextMenuItemSpecImpl itemSpec = new ContextMenuItemSpecImpl(false);
            itemConfigurator.accept(itemSpec);
            itemSpec.apply(builder);
        });
        ContextMenuContainer.super.buildItems(builder, mouseX, mouseY);
    }
}
