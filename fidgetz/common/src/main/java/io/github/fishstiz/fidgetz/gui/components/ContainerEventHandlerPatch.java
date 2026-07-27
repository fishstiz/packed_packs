package io.github.fishstiz.fidgetz.gui.components;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;

public interface ContainerEventHandlerPatch extends ContainerEventHandler {
    @Override
    default boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.isMouseClickHandled(mouseX, mouseY, button);
    }

    /**
     * {@link ContainerEventHandler#mouseClicked(double, double, int)}, except it only
     * returns true if mouse click is actually handled instead of when child is present.
     */
    private boolean isMouseClickHandled(double mouseX, double mouseY, int button) {
        return this.getChildAt(mouseX, mouseY).map(child -> {
            if (child.mouseClicked(mouseX, mouseY, button)) {
                this.setFocused(child);
                if (button == InputConstants.MOUSE_BUTTON_LEFT) {
                    this.setDragging(true);
                }
                return true;
            }
            return false;
        }).orElse(false);
    }

    @Override
    default boolean mouseReleased(double mouseX, double mouseY, int button) {
        return ContainerEventHandler.super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    default boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return ContainerEventHandler.super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
}
