package io.github.fishstiz.fidgetz.gui.components;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.input.MouseButtonEvent;
import org.jspecify.annotations.NonNull;

public interface ContainerEventHandlerPatch extends ContainerEventHandler {
    @Override
    default boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
        return this.isMouseClickHandled(mouseButtonEvent, doubleClicked);
    }

    /**
     * {@link ContainerEventHandler#mouseClicked}, except it only
     * returns {@code true} if mouse click is actually handled instead of when child is present.
     * This is to allow elements to not focus by returning false.
     * Note that #shouldTakeFocusAfterInteraction does not exist in older versions.
     */
    private boolean isMouseClickHandled(MouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
        return this.getChildAt(mouseButtonEvent.x(), mouseButtonEvent.y()).map(child -> {
            if (child.mouseClicked(mouseButtonEvent, doubleClicked)) {
                if (child.shouldTakeFocusAfterInteraction()) {
                    this.setFocused(child);
                }
                if (mouseButtonEvent.button() == InputConstants.MOUSE_BUTTON_LEFT) {
                    this.setDragging(true);
                }
                return true;
            }
            return false;
        }).orElse(false);
    }

    @Override
    default boolean mouseReleased(@NonNull MouseButtonEvent mouseButtonEvent) {
        return ContainerEventHandler.super.mouseReleased(mouseButtonEvent);
    }

    @Override
    default boolean mouseDragged(@NonNull MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
        return ContainerEventHandler.super.mouseDragged(mouseButtonEvent, dragX, dragY);
    }
}
