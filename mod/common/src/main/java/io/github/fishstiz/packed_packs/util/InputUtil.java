package io.github.fishstiz.packed_packs.util;

import static com.mojang.blaze3d.platform.InputConstants.*;
import static net.minecraft.client.gui.screens.Screen.hasControlDown;
import static net.minecraft.client.gui.screens.Screen.hasShiftDown;

public class InputUtil {
    public static final String DEV_MODE_SHORTCUT = "CTRL + SHIFT + I";
    public static final long DOUBLE_CLICK_THRESHOLD_MS = 250;
    public static final int MOUSE_BUTTON_BACK = 3;
    public static final int MOUSE_BUTTON_FORWARD = 4;
    public static final int MOD_SHIFT = 1;
    public static final int MOD_ALT = 4;

    private InputUtil() {
    }

    public static boolean isLeftClick(int button) {
        return button == MOUSE_BUTTON_LEFT;
    }

    public static boolean isRightClick(int button) {
        return button == MOUSE_BUTTON_RIGHT;
    }

    public static boolean isClickBack(int button) {
        return button == MOUSE_BUTTON_BACK;
    }

    public static boolean isClickForward(int button) {
        return button == MOUSE_BUTTON_FORWARD;
    }

    public static boolean isUndo(int keyCode, int modifiers) {
        return keyCode == KEY_Z && hasControlDown() && modifiers == MOD_CONTROL;
    }

    public static boolean isRedo(int keyCode, int modifiers) {
        if (keyCode == KEY_Z) {
            return hasControlDown() && hasShiftDown() && modifiers == MOD_CONTROL + MOD_SHIFT;
        } else if (keyCode == KEY_Y) {
            return hasControlDown() && modifiers == MOD_CONTROL;
        }

        return false;
    }

    public static boolean isTransfer(int keyCode, int modifiers) {
        return noModifiers(modifiers) && (keyCode == KEY_SPACE || keyCode == KEY_RETURN);
    }

    public static boolean isUp(int keyCode) {
        return keyCode == KEY_UP;
    }

    public static boolean isDown(int keyCode) {
        return keyCode == KEY_DOWN;
    }

    public static boolean isHome(int keyCode) {
        return keyCode == KEY_HOME;
    }

    public static boolean isEnd(int keyCode) {
        return keyCode == KEY_END;
    }

    public static boolean isPageUp(int keyCode) {
        return keyCode == KEY_PAGEUP;
    }

    public static boolean isPageDown(int keyCode) {
        return keyCode == KEY_PAGEDOWN;
    }

    public static boolean isMoveDown(int keyCode, int modifiers) {
        return keyCode == KEY_DOWN && moveModifiers(modifiers);
    }

    public static boolean isMoveUp(int keyCode, int modifiers) {
        return keyCode == KEY_UP && moveModifiers(modifiers);
    }

    public static boolean isExpandFolder(int keyCode, int modifiers) {
        return noModifiers(modifiers) && keyCode == KEY_RETURN;
    }

    public static boolean isDelete(int keyCode, int modifiers) {
        return noModifiers(modifiers) && keyCode == KEY_DELETE;
    }

    public static boolean isRename(int keyCode, int modifiers) {
        return (noModifiers(modifiers) && keyCode == KEY_F2) || (modifiers == MOD_CONTROL && keyCode == KEY_R);
    }

    public static boolean isRefresh(int keyCode, int modifiers) {
        return noModifiers(modifiers) && keyCode == KEY_F5;
    }

    public static boolean isOpenFile(int keyCode, int modifiers) {
        return modifiers == MOD_CONTROL && keyCode == KEY_RETURN;
    }

    public static boolean isOpenFolder(int keyCode, int modifiers) {
        return modifiers == MOD_ALT + MOD_SHIFT && keyCode == KEY_R;
    }

    public static boolean isDeveloperMode(int keyCode, int modifiers) {
        return  noModifiers(modifiers) && keyCode == KEY_F12 ||
                modifiers == MOD_CONTROL + MOD_SHIFT && keyCode == KEY_I;
    }

    public static boolean isSelectAll(int keyCode, int modifiers) {
        return modifiers == MOD_CONTROL && keyCode == KEY_A;
    }

    public static boolean isSwitchDefaultProfile(int keyCode, int modifiers) {
        return noModifiers(modifiers) && keyCode == KEY_F1;
    }

    public static boolean isOpenProfiles(int keyCode, int modifiers) {
        return modifiers == MOD_CONTROL && keyCode == KEY_GRAVE;
    }

    public static boolean noModifiers(int modifiers) {
        return modifiers == 0;
    }

    public static boolean moveModifiers(int modifiers) {
        return modifiers == MOD_CONTROL || modifiers == MOD_ALT;
    }

    public static boolean isRangeModifierActive() {
        return hasShiftDown();
    }

    public static boolean isSelectModifierActive() {
        return hasControlDown();
    }
}
