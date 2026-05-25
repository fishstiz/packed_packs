package io.github.fishstiz.packed_packs.api.gui;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface ElementSink {
    void acceptWidget(GuiEventListener widget);

    void acceptRenderable(Renderable renderable);

    void acceptElement(LayoutElement element);
}