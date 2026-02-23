package io.github.fishstiz.packed_packs.gui.components;

import net.minecraft.util.Util;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.function.BooleanSupplier;

import static io.github.fishstiz.packed_packs.util.InputUtil.*;

public class MouseSelectionHandler {
    private static final double DRAG_THRESHOLD = 1.0;
    private final GuiEventListener inputListener;
    private final BooleanSupplier selected;
    private final BooleanSupplier selectedLast;
    private final BooleanSupplier selectedOnly;
    private MouseSelectionState mouseSelectionState = MouseSelectionState.INACTIVE;
    private long lastClickTime = 0;

    public enum Action {
        NONE,
        FOCUS,
        SELECT,
        SELECT_TOGGLE,
        SELECT_EXCLUSIVE,
        SELECT_RANGE,
        TRANSFER,
        DRAG;

        public boolean shouldDispatch() {
            return this != NONE;
        }
    }

    private enum MouseSelectionState {
        INACTIVE,
        SELECTING_ONE,
        SELECTING_MANY
    }

    public MouseSelectionHandler(GuiEventListener inputListener, BooleanSupplier selected, BooleanSupplier selectedLast, BooleanSupplier selectedOnly) {
        this.inputListener = inputListener;
        this.selected = selected;
        this.selectedLast = selectedLast;
        this.selectedOnly = selectedOnly;
    }

    private static boolean exceedsDragThreshold(double dragX, double dragY) {
        return Math.hypot(dragX, dragY) > DRAG_THRESHOLD;
    }

    private boolean updateDoubleClick() {
        long currentTime = Util.getMillis();
        boolean doubleClicked = (currentTime - this.lastClickTime) < DOUBLE_CLICK_THRESHOLD_MS;
        this.lastClickTime = currentTime;
        return doubleClicked;
    }

    public Action mouseClicked(MouseButtonEvent mouseButtonEvent) {
        if (!isLeftClick(mouseButtonEvent) || !this.inputListener.isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y())) {
            this.mouseSelectionState = MouseSelectionState.INACTIVE;
            return Action.NONE;
        }
        if (!isRangeModifierActive() && !isSelectModifierActive() && this.updateDoubleClick()) {
            return Action.TRANSFER;
        }
        if (isRangeModifierActive()) {
            this.mouseSelectionState = MouseSelectionState.SELECTING_MANY;
            return Action.SELECT_RANGE;
        }
        if (isSelectModifierActive()) {
            this.mouseSelectionState = MouseSelectionState.SELECTING_MANY;
            return Action.SELECT_TOGGLE;
        }
        if (!this.selected.getAsBoolean()) {
            this.mouseSelectionState = MouseSelectionState.SELECTING_ONE;
            return Action.SELECT_EXCLUSIVE;
        }
        if (!this.selectedLast.getAsBoolean()) {
            this.mouseSelectionState = MouseSelectionState.SELECTING_ONE;
            return Action.SELECT;
        }

        this.mouseSelectionState = MouseSelectionState.SELECTING_ONE;
        return Action.FOCUS;
    }

    public Action mouseReleased(MouseButtonEvent mouseButtonEvent) {
        if (this.inputListener.isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y())
            && this.mouseSelectionState == MouseSelectionState.SELECTING_ONE
            && this.selectedLast.getAsBoolean()
            && !this.selectedOnly.getAsBoolean()) {
            this.mouseSelectionState = MouseSelectionState.INACTIVE;
            return Action.SELECT_EXCLUSIVE;
        }
        this.mouseSelectionState = MouseSelectionState.INACTIVE;
        return Action.NONE;
    }

    public Action mouseDragged(MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
        if (!this.inputListener.isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y())) {
            this.mouseSelectionState = MouseSelectionState.INACTIVE;
            return Action.NONE;
        }

        if (exceedsDragThreshold(dragX, dragY) &&
            this.selected.getAsBoolean() &&
            this.mouseSelectionState == MouseSelectionState.SELECTING_ONE) {
            return Action.DRAG;
        }

        return Action.NONE;
    }
}
