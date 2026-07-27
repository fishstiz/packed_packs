package io.github.fishstiz.packed_packs.util.constants;

import io.github.fishstiz.fidgetz.gui.components.contextmenu.MenuItem;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.MenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.renderables.ColoredRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import net.minecraft.network.chat.Component;

public class GuiConstants {
    public static final int SPACING = 8;
    public static final ColoredRect WHITE_OVERLAY = new ColoredRect(Theme.WHITE.withAlpha(0.25f));
    public static final ColoredRect DEVELOPER_MODE_ITEM_BACKGROUND = new ColoredRect(Theme.BLACK.withAlpha(0.25f));
    public static final Sprite CROSS_SPRITE = Sprite.of16(ResourceUtil.getIcon("cross"));
    public static final Sprite HAMBURGER_SPRITE = Sprite.of16(ResourceUtil.getIcon("hamburger"));
    public static final Sprite LOCK_SPRITE = new Sprite(ResourceUtil.getVanillaSprite("widget/locked_button_disabled"), 20, 20);
    public static final Sprite UNLOCK_SPRITE_SMALL = Sprite.of16(ResourceUtil.getIcon("unlock"));
    public static final Sprite LOCK_SPRITE_SMALL = Sprite.of16(ResourceUtil.getIcon("lock"));
    public static final Sprite STAR_SPRITE = Sprite.of16(ResourceUtil.getIcon("star"));
    public static final Sprite RADIO_OFF_SPRITE = Sprite.of16(ResourceUtil.getIcon("radio_off"));
    public static final Sprite RADIO_ON_SPRITE = Sprite.of16(ResourceUtil.getIcon("radio_on"));
    public static final Component OPTIONS_TEXT = ResourceUtil.getText("options.title");
    public static final Component OPEN_FILE_TEXT = ResourceUtil.getText("file.open");
    public static final Component OPEN_PARENT_TEXT = ResourceUtil.getText("file.parent.open");
    public static final Component RENAME_FILE_TEXT = ResourceUtil.getText("file.rename");
    public static final Component DELETE_FILE_TEXT = ResourceUtil.getText("file.delete");

    private GuiConstants() {
    }

    public static MenuItemBuilder devItem(Component text) {
        return MenuItem.builder(text).background(GuiConstants.DEVELOPER_MODE_ITEM_BACKGROUND);
    }

    public static Sprite getToggleIcon(boolean toggled) {
        return toggled ? RADIO_ON_SPRITE : RADIO_OFF_SPRITE;
    }
}
