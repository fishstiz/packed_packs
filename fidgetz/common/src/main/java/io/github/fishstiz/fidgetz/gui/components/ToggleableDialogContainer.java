package io.github.fishstiz.fidgetz.gui.components;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.fishstiz.fidgetz.util.lang.CollectionsUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.List;

import static io.github.fishstiz.fidgetz.util.GuiUtil.isDescendant;

public interface ToggleableDialogContainer extends ContainerEventHandlerPatch {
    List<ToggleableDialog<?>> getDialogs();

    default List<ToggleableDialog<?>> getOpenDialogs() {
        return CollectionsUtil.filter(this.getDialogs(), ToggleableDialog::isOpen, ObjectArrayList::new);
    }

    default boolean isChildCovered(GuiEventListener child) {
        boolean isDialogChild = false;
        boolean isEnclosed = false;

        for (ToggleableDialog<?> dialog : this.getOpenDialogs()) {
            if (dialog != child && (dialog.isCaptureClick() || dialog.isCaptureFocus()) && !isDescendant(dialog, child)) {
                return true;
            }
            if (!isEnclosed && isDescendant(dialog, child)) {
                isDialogChild = true;
                break;
            }
            if (!isEnclosed && dialog != child && dialog.encloses(child)) {
                isEnclosed = true;
            }
        }

        return !isDialogChild && isEnclosed;
    }

    @Override
    default boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
        boolean propagateNonDialogs = true;

        for (ToggleableDialog<?> dialog : this.getDialogs()) {
            if (dialog.mouseClicked(mouseButtonEvent, doubleClicked)) {
                if (dialog.isOpen() && dialog.shouldTakeFocusAfterInteraction()) {
                    this.setFocused(dialog);
                }
                if (mouseButtonEvent.button() == InputConstants.MOUSE_BUTTON_LEFT) {
                    this.setDragging(true);
                }
                return true;
            }
            if (dialog.isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y())) {
                propagateNonDialogs = false;
            }
        }

        return propagateNonDialogs && ContainerEventHandlerPatch.super.mouseClicked(mouseButtonEvent, doubleClicked);
    }
}
