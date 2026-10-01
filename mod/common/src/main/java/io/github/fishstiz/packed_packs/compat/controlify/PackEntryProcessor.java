package io.github.fishstiz.packed_packs.compat.controlify;

import com.mojang.blaze3d.platform.InputConstants;
import dev.isxander.controlify.bindings.ControlifyBindings;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.controller.input.ControllerStateView;
import dev.isxander.controlify.controller.input.GamepadInputs;
import dev.isxander.controlify.controller.input.InputComponent;
import dev.isxander.controlify.screenop.ComponentProcessor;
import dev.isxander.controlify.screenop.ScreenProcessor;
import io.github.fishstiz.packed_packs.gui.components.PackList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenDirection;
import org.lwjgl.glfw.GLFW;

class PackEntryProcessor implements ComponentProcessor {
    private final PackList.Entry entry;
    private boolean pressed;

    PackEntryProcessor(PackList.Entry entry) {
        this.entry = entry;
    }

    @Override
    public boolean overrideControllerButtons(ScreenProcessor<?> screen, ControllerEntity controller) {
        if (!entry.isFocused()) {
            pressed = false;
            return false;
        }
        if (ControlifyBindings.GUI_PRESS.on(controller).guiPressed().get()) {
            entry.keyPressed(InputConstants.KEY_SPACE, GLFW.glfwGetKeyScancode(GLFW.GLFW_KEY_SPACE), 0);
            return true;
        }

        InputComponent input = controller.input().orElse(null);
        if (input == null) return false;

        ControllerStateView state = input.stateNow();
        ControllerStateView prevState = input.stateThen();

        if (PackedPacksScreenProcessor.justReleased(GamepadInputs.NORTH_BUTTON, state, prevState)) {
            if (pressed) {
                pressed = false;
                entry.selectToggle();
                return true;
            }
        } else if (PackedPacksScreenProcessor.justPressed(GamepadInputs.NORTH_BUTTON, state, prevState)) {
            pressed = true;
        }

        return false;
    }

    @Override
    public boolean overrideControllerNavigation(ScreenProcessor<?> screen, ControllerEntity controller) {
        if (!entry.isFocused() || !(screen instanceof PackedPacksScreenProcessor screenProcessor)) {
            return false;
        }

        if (controller.input().filter(input -> input.stateNow().isButtonDown(GamepadInputs.WEST_BUTTON)).isPresent()) {
            ScreenDirection direction = PackedPacksScreenProcessor.getVerticalDirection(screenProcessor, controller);
            if (direction != null) {
                screenProcessor.holdRepeatHelper().onNavigate();
                entry.movePack(direction == ScreenDirection.UP);
            }
            return true;
        }

        return false;
    }
}
