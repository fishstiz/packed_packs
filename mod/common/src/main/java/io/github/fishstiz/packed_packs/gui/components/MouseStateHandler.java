package io.github.fishstiz.packed_packs.gui.components;

import io.github.fishstiz.packed_packs.gui.model.PackListViewModel;
import net.minecraft.Util;
import net.minecraft.client.gui.components.events.GuiEventListener;

import static io.github.fishstiz.packed_packs.util.InputUtil.*;
import static io.github.fishstiz.packed_packs.util.InputUtil.isRangeModifierActive;
import static io.github.fishstiz.packed_packs.util.InputUtil.isSelectModifierActive;

public class MouseStateHandler {
    private static final double DRAG_THRESHOLD = 1.0;
    private final GuiEventListener entry;
    private final PackListViewModel.Entry model;
    private State state = State.INACTIVE;
    private long lastClickTime = 0;

    private enum State {
        INACTIVE,
        SELECTING_ONE,
        SELECTING_MANY
    }

    public MouseStateHandler(GuiEventListener entry, PackListViewModel.Entry model) {
        this.entry = entry;
        this.model = model;
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
            model.transfer();
            return false; // do not take focus
        }
        if (isRangeModifierActive()) {
            this.state = State.SELECTING_MANY;
            model.selectRange();
            return true;
        }
        if (isSelectModifierActive()) {
            this.state = State.SELECTING_MANY;
            model.selectToggle();
            return true;
        }
        if (!model.selected()) {
            this.state = State.SELECTING_ONE;
            model.selectExclusive();
            return true;
        }
        if (!model.selectedLast()) {
            this.state = State.SELECTING_ONE;
            model.select();
            return true;
        }

        this.state = State.SELECTING_ONE;
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (entry.isMouseOver(mouseX, mouseY)
            && this.state == State.SELECTING_ONE
            && model.selectedLast()
            && !model.selectedExclusive()) {
            this.state = State.INACTIVE;
            model.selectExclusive();
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

        if (exceedsDragThreshold(dragX, dragY) && model.selected() && this.state == State.SELECTING_ONE) {
            model.drag();
            return true;
        }

        return false;
    }
}
