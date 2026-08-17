package io.github.fishstiz.packed_packs.gui.components;

import net.minecraft.Util;

import static io.github.fishstiz.packed_packs.util.InputUtil.*;

class MouseStateHandler {
    private static final double DRAG_THRESHOLD = 1.0;
    private final PackList.Entry entry;
    private State state = State.INACTIVE;
    private long lastClickTime = 0;

    private enum State {
        INACTIVE,
        SELECTING_ONE,
        SELECTING_MANY
    }

    MouseStateHandler(PackList.Entry entry) {
        this.entry = entry;
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

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isLeftClick(button) || !entry.isMouseOver(mouseX, mouseY)) {
            this.state = State.INACTIVE;
            return false;
        }
        if (!isRangeModifierActive() && !isSelectModifierActive() && updateDoubleClick()) {
            entry.transferPack();
            return false; // do not take focus
        }
        if (isRangeModifierActive()) {
            this.state = State.SELECTING_MANY;
            entry.selectTowardsPack();
            return true;
        }
        if (isSelectModifierActive()) {
            this.state = State.SELECTING_MANY;
            entry.selectToggle();
            return true;
        }
        if (!entry.state.isSelected()) {
            this.state = State.SELECTING_ONE;
            entry.selectPackExclusively();
            return true;
        }
        if (!entry.state.isSelectedLast()) {
            this.state = State.SELECTING_ONE;
            entry.selectPack();
            return true;
        }

        this.state = State.SELECTING_ONE;
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (entry.isMouseOver(mouseX, mouseY)
            && this.state == State.SELECTING_ONE
            && entry.state.isSelectedLast()
            && !entry.state.isSelectedExclusively()) {
            this.state = State.INACTIVE;
            entry.selectPackExclusively();
            return true;
        }

        this.state = State.INACTIVE;
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!entry.isMouseOver(mouseX, mouseY)) {
            this.state = State.INACTIVE;
            return false;
        }

        if (exceedsDragThreshold(dragX, dragY) && entry.state.isSelected() && this.state == State.SELECTING_ONE) {
            entry.dragPack();
            return true;
        }

        return false;
    }
}
