package io.github.fishstiz.fidgetz.gui.components;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.fishstiz.fidgetz.util.lang.CollectionsUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.components.events.GuiEventListener;

import java.util.Comparator;
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

        for (ToggleableDialog<?> dialog : this.getOpenDialogsFromTop()) {
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

    private List<ToggleableDialog<?>> getOpenDialogsFromTop() {
        List<ToggleableDialog<?>> dialogs = this.getOpenDialogs();
        dialogs.sort(Comparator.<ToggleableDialog<?>, Float>comparing(ToggleableDialog::getZ).reversed());
        return dialogs;
    }

    @Override
    default boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean propagateNonDialogs = true;

        for (ToggleableDialog<?> dialog : this.getDialogs()) {
            if (dialog.mouseClicked(mouseX, mouseY, button)) {
                if (dialog.isOpen()) {
                    this.setFocused(dialog);
                }
                if (button == InputConstants.MOUSE_BUTTON_LEFT) {
                    this.setDragging(true);
                }
                return true;
            }
            if (dialog.isMouseOver(mouseX, mouseY)) {
                propagateNonDialogs = false;
            }
        }

        return propagateNonDialogs && ContainerEventHandlerPatch.super.mouseClicked(mouseX, mouseY, button);
    }
}
