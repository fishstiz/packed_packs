package io.github.fishstiz.fidgetz.gui.components;

import io.github.fishstiz.fidgetz.util.GuiUtil;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.Nullable;

public interface HoverStateHandler extends ContainerEventHandler {
    @Nullable GuiEventListener getHovered();

    default @Nullable GuiEventListener findHovered(double mouseX, double mouseY) {
        return GuiUtil.findHovered(this, mouseX, mouseY);
    }
}
