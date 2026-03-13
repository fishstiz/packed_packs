package io.github.fishstiz.packed_packs.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import static com.mojang.blaze3d.platform.InputConstants.*;

public class InputUtil {
    public static final String DEV_MODE_SHORTCUT = "CTRL + SHIFT + I";
    public static final long DOUBLE_CLICK_THRESHOLD_MS = 250;

    private InputUtil() {
    }

    public static boolean isLeftClick(MouseButtonEvent mouseEvent) {
        return mouseEvent.button() == MOUSE_BUTTON_LEFT;
    }

    public static boolean isRightClick(MouseButtonEvent mouseEvent) {
        return mouseEvent.button() == MOUSE_BUTTON_RIGHT;
    }

    public static boolean isClickBack(MouseButtonEvent mouseEvent) {
        return mouseEvent.button() == MOUSE_BUTTON_4;
    }

    public static boolean isClickForward(MouseButtonEvent mouseEvent) {
        return mouseEvent.button() == MOUSE_BUTTON_5;
    }

    public static boolean isUndo(KeyEvent keyEvent) {
        return keyEvent.key() == KEY_Z && keyEvent.hasControlDown() && keyEvent.modifiers() == MOD_CONTROL;
    }

    public static boolean isRedo(KeyEvent keyEvent) {
        if (keyEvent.key() == KEY_Z) {
            return keyEvent.hasControlDown() && keyEvent.hasShiftDown() && keyEvent.modifiers() == MOD_CONTROL + MOD_SHIFT;
        } else if (keyEvent.key() == KEY_Y) {
            return keyEvent.hasControlDown() && keyEvent.modifiers() == MOD_CONTROL;
        }

        return false;
    }

    public static boolean isTransfer(KeyEvent keyEvent) {
        return noModifiers(keyEvent.modifiers()) && (keyEvent.key() == KEY_SPACE || keyEvent.key() == KEY_RETURN);
    }

    public static boolean isUp(KeyEvent keyEvent) {
        return keyEvent.isUp();
    }

    public static boolean isDown(KeyEvent keyEvent) {
        return keyEvent.isDown();
    }

    public static boolean isHome(KeyEvent keyEvent) {
        return keyEvent.key() == KEY_HOME;
    }

    public static boolean isEnd(KeyEvent keyEvent) {
        return keyEvent.key() == KEY_END;
    }

    public static boolean isPageUp(KeyEvent keyEvent) {
        return keyEvent.key() == KEY_PAGEUP;
    }

    public static boolean isPageDown(KeyEvent keyEvent) {
        return keyEvent.key() == KEY_PAGEDOWN;
    }

    public static boolean isMoveDown(KeyEvent keyEvent) {
        return keyEvent.key() == KEY_DOWN && moveModifiers(keyEvent.modifiers());
    }

    public static boolean isMoveUp(KeyEvent keyEvent) {
        return keyEvent.key() == KEY_UP && moveModifiers(keyEvent.modifiers());
    }

    public static boolean isExpandFolder(KeyEvent keyEvent) {
        return noModifiers(keyEvent.modifiers()) && keyEvent.key() == KEY_RETURN;
    }

    public static boolean isDelete(KeyEvent keyEvent) {
        return noModifiers(keyEvent.modifiers()) && keyEvent.key() == KEY_DELETE;
    }

    public static boolean isRename(KeyEvent keyEvent) {
        return (noModifiers(keyEvent.modifiers()) && keyEvent.key() == KEY_F2) ||
               (keyEvent.modifiers() == MOD_CONTROL && keyEvent.key() == KEY_R);
    }

    public static boolean isRefresh(KeyEvent keyEvent) {
        return noModifiers(keyEvent.modifiers()) && keyEvent.key() == KEY_F5;
    }

    public static boolean isOpenFile(KeyEvent keyEvent) {
        return keyEvent.modifiers() == MOD_CONTROL && keyEvent.key() == KEY_RETURN;
    }

    public static boolean isOpenFolder(KeyEvent keyEvent) {
        return keyEvent.modifiers() == MOD_ALT + MOD_SHIFT && keyEvent.key() == KEY_R;
    }

    public static boolean isDeveloperMode(KeyEvent keyEvent) {
        return noModifiers(keyEvent.modifiers()) && keyEvent.key() == KEY_F12 ||
               keyEvent.modifiers() == MOD_CONTROL + MOD_SHIFT && keyEvent.key() == KEY_I;
    }

    public static boolean isSelectAll(KeyEvent keyEvent) {
        return keyEvent.modifiers() == MOD_CONTROL && keyEvent.key() == KEY_A;
    }

    public static boolean isSwitchDefaultProfile(KeyEvent keyEvent) {
        return noModifiers(keyEvent.modifiers()) && keyEvent.key() == KEY_F1;
    }

    public static boolean isOpenProfiles(KeyEvent keyEvent) {
        return keyEvent.modifiers() == MOD_CONTROL && keyEvent.key() == KEY_GRAVE;
    }

    public static boolean noModifiers(int modifiers) {
        return modifiers == 0;
    }

    public static boolean moveModifiers(int modifiers) {
        return modifiers == MOD_CONTROL || modifiers == MOD_ALT;
    }

    public static boolean shiftOnly(int modifiers) {
        return modifiers == MOD_SHIFT;
    }

    public static boolean isRangeModifierActive() {
        return Minecraft.getInstance().hasShiftDown();
    }

    public static boolean isSelectModifierActive() {
        return Minecraft.getInstance().hasControlDown();
    }
}
