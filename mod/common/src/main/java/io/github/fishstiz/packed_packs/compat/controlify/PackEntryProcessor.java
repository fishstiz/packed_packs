package io.github.fishstiz.packed_packs.compat.controlify;

import com.mojang.blaze3d.platform.InputConstants;
import dev.isxander.controlify.bindings.ControlifyBindings;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.screenop.ComponentProcessor;
import dev.isxander.controlify.screenop.ScreenProcessor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

record PackEntryProcessor(GuiEventListener entry) implements ComponentProcessor {
    @Override
    public boolean overrideControllerButtons(ScreenProcessor<?> screen, ControllerEntity controller) {
        if (ControlifyBindings.GUI_PRESS.on(controller).guiPressed().get()) {
            entry.keyPressed(new KeyEvent(InputConstants.KEY_SPACE, GLFW.glfwGetKeyScancode(GLFW.GLFW_KEY_SPACE), 0));
            return true;
        }
        return false;
    }
}
