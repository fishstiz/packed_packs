package io.github.fishstiz.packed_packs.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ToastUtil {
    private static final SystemToast.SystemToastId FILE_OPS_FAIL_ID = new SystemToast.SystemToastId();
    private static final SystemToast.SystemToastId DEV_MODE_ID = new SystemToast.SystemToastId(750);

    private ToastUtil() {
    }

    public static void onDevModeToggleToast(boolean enabled) {
        Component enableText = enabled ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF;
        SystemToast.addOrUpdate(
                Minecraft.getInstance().getToastManager(),
                DEV_MODE_ID,
                ResourceUtil.getModName(),
                ResourceUtil.getText("dev_mode", enableText)
        );
    }

    public static void onFileFailToast(Component message) {
        SystemToast.addOrUpdate(Minecraft.getInstance().getToastManager(), FILE_OPS_FAIL_ID, ResourceUtil.getText("file.fail"), message);
    }

    public static Component getRenameFailText(String from, String to) {
        return ResourceUtil.getText("file.rename.fail", from, to);
    }

    public static Component getDeleteFailText(String fileName) {
        return ResourceUtil.getText("file.delete.fail", fileName);
    }

    public static void onDeleteFailToast(Component fileName) {
        onFileFailToast(getDeleteFailText(fileName.getString()));
    }

    public static void onRenameFailToast(Component from, String to) {
        onFileFailToast(getRenameFailText(from.getString(), to));
    }
}
