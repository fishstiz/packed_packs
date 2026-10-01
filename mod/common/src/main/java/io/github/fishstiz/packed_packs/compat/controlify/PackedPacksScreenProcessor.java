package io.github.fishstiz.packed_packs.compat.controlify;

import com.mojang.blaze3d.platform.InputConstants;
import dev.isxander.controlify.api.bind.InputBinding;
import dev.isxander.controlify.bindings.ControlifyBindings;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.controller.input.ControllerStateView;
import dev.isxander.controlify.controller.input.GamepadInputs;
import dev.isxander.controlify.controller.input.InputComponent;
import dev.isxander.controlify.screenop.ScreenProcessor;
import dev.isxander.controlify.utils.HoldRepeatHelper;
import io.github.fishstiz.packed_packs.gui.components.PackList;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksScreen;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.Supplier;

class PackedPacksScreenProcessor extends ScreenProcessor<PackedPacksScreen> {
    public PackedPacksScreenProcessor(PackedPacksScreen screen) {
        super(screen);
    }

    @Override
    protected boolean handleComponentButtonOverride(ControllerEntity controller) {
        if (ControlifyBindings.GUI_BACK.on(controller).guiPressed().get()) {
            screen.keyPressed(InputConstants.KEY_ESCAPE, GLFW.glfwGetKeyScancode(GLFW.GLFW_KEY_ESCAPE), 0);
            return true;
        }

        InputComponent input = controller.input().orElse(null);
        if (input != null) {
            ControllerStateView state = input.stateNow();
            ControllerStateView prevState = input.stateThen();

            if (justPressed(GamepadInputs.LEFT_SHOULDER_BUTTON, state, prevState)) {
                screen.toggleSidebar();
                return true;
            } else if (justPressed(GamepadInputs.START_BUTTON, state, prevState)) {
                screen.openOptions();
                return true;
            } else if (justPressed(GamepadInputs.RIGHT_SHOULDER_BUTTON, state, prevState)) {
                GuiEventListener leaf = leafComponent(screen.getCurrentFocusPath());
                if (leaf == null) {
                    screen.openContextMenu(0, 0, true);
                } else {
                    ScreenRectangle rect = leaf.getRectangle();
                    screen.openContextMenu(rect.right() - 1, rect.top(), true);
                }
                return true;
            }
        }

        return super.handleComponentButtonOverride(controller);
    }

    @Override
    protected @Nullable Supplier<Boolean> createScreenNavigationFunc(ScreenDirection direction) {
        ComponentPath path = screen.nextFocusPath(new FocusNavigationEvent.ArrowNavigation(direction));
        if (path == null) return null;

        return () -> {
            screen.changeFocus(unselectListPath(path));
            return true;
        };
    }

    HoldRepeatHelper holdRepeatHelper() {
        return holdRepeatHelper;
    }

    private static ComponentPath unselectListPath(ComponentPath path) {
        if (path instanceof ComponentPath.Path(ContainerEventHandler component, ComponentPath childPath)) {
            return ComponentPath.path(component, unselectListPath(childPath));
        } else if (path instanceof PackList.ListPath listPath) {
            return new PackList.ListPath(listPath.component(), listPath.child(), listPath.scroll(), false);
        } else {
            return path;
        }
    }

    private static GuiEventListener leafComponent(@Nullable ComponentPath path) {
        if (path instanceof ComponentPath.Path containerPath) {
            return leafComponent(containerPath.childPath());
        } else if (path instanceof PackList.ListPath listPath) {
            return listPath.child();
        } else if (path != null) {
            return path.component();
        } else {
            return null;
        }
    }

    static @Nullable ScreenDirection getVerticalDirection(
            PackedPacksScreenProcessor screen,
            ControllerEntity controller
    ) {
        InputComponent input = controller.input().orElse(null);
        if (input == null) return null;

        HoldRepeatHelper holdRepeatHelper = screen.holdRepeatHelper();
        boolean canRepeat = holdRepeatHelper.canNavigate();

        ControllerStateView state = input.stateNow();
        ControllerStateView prevState = input.stateThen();

        ScreenDirection direction = null;

        if (canNavigate(ControlifyBindings.GUI_NAVI_UP.on(controller), canRepeat, holdRepeatHelper)) {
            direction = ScreenDirection.UP;
        } else if (canNavigate(ControlifyBindings.GUI_NAVI_DOWN.on(controller), canRepeat, holdRepeatHelper)) {
            direction = ScreenDirection.DOWN;
        } else if (canPress(GamepadInputs.DPAD_UP_BUTTON, state, prevState, canRepeat, holdRepeatHelper)) {
            direction = ScreenDirection.UP;
        } else if (canPress(GamepadInputs.DPAD_DOWN_BUTTON, state, prevState, canRepeat, holdRepeatHelper)) {
            direction = ScreenDirection.DOWN;
        }

        return direction;
    }

    static boolean canNavigate(InputBinding binding, boolean canRepeat, HoldRepeatHelper holdRepeatHelper) {
        if (binding.digitalNow() && (canRepeat || !binding.digitalPrev())) {
            if (!binding.digitalPrev()) {
                holdRepeatHelper.reset();
            }
            return true;
        }
        return false;
    }

    static boolean canPress(
            ResourceLocation input,
            ControllerStateView state,
            ControllerStateView prevState,
            boolean canRepeat,
            HoldRepeatHelper holdRepeatHelper
    ) {
        if (state.isButtonDown(input) && (canRepeat || !prevState.isButtonDown(input))) {
            if (!prevState.isButtonDown(input)) {
                holdRepeatHelper.reset();
            }
            return true;
        }
        return false;
    }

    static boolean justPressed(
            ResourceLocation input,
            ControllerStateView state,
            ControllerStateView prevState
    ) {
        return state.isButtonDown(input) && !prevState.isButtonDown(input);
    }

    static boolean justReleased(
            ResourceLocation input,
            ControllerStateView state,
            ControllerStateView prevState
    ) {
        return !state.isButtonDown(input) && prevState.isButtonDown(input);
    }
}
