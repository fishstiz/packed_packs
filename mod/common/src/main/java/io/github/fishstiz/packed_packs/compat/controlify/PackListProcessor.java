package io.github.fishstiz.packed_packs.compat.controlify;

import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.controller.input.GamepadInputs;
import dev.isxander.controlify.screenop.ComponentProcessor;
import dev.isxander.controlify.screenop.ScreenProcessor;
import io.github.fishstiz.packed_packs.gui.components.PackList;
import net.minecraft.client.gui.navigation.ScreenDirection;

public record PackListProcessor(PackList packList) implements ComponentProcessor {
    @Override
    public boolean overrideControllerNavigation(ScreenProcessor<?> screen, ControllerEntity controller) {
        if (!packList.isFocused() || !(screen instanceof PackedPacksScreenProcessor screenProcessor)) {
            return false;
        }

        if (controller.input().filter(input -> input.stateNow().isButtonDown(GamepadInputs.NORTH_BUTTON)).isPresent()) {
            ScreenDirection direction = PackedPacksScreenProcessor.getVerticalDirection(screenProcessor, controller);
            if (direction != null) {
                PackList.Entry focused = packList.getFocused();
                screenProcessor.holdRepeatHelper().onNavigate();
                if (focused != null) focused.selectPack();
                packList.selectRangeAtDirection(direction, true);
                return true;
            }
        }

        return false;
    }
}
