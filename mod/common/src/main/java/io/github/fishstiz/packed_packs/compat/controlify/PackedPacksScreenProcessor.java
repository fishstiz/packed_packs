package io.github.fishstiz.packed_packs.compat.controlify;

import com.mojang.blaze3d.platform.InputConstants;
import dev.isxander.controlify.bindings.ControlifyBindings;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.screenop.ScreenProcessor;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksScreen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

class PackedPacksScreenProcessor extends ScreenProcessor<PackedPacksScreen> {
    public PackedPacksScreenProcessor(PackedPacksScreen screen) {
        super(screen);
    }

    @Override
    protected void handleButtons(ControllerEntity controller) {
        super.handleButtons(controller);

        if (ControlifyBindings.GUI_BACK.on(controller).guiPressed().get()) {
            screen.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, GLFW.glfwGetKeyScancode(GLFW.GLFW_KEY_ESCAPE), 0));
        }
    }
}
