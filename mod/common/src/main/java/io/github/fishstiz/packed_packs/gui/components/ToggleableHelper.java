package io.github.fishstiz.packed_packs.gui.components;

import io.github.fishstiz.fidgetz.gui.components.FidgetzButton;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.MenuItem;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.MenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.renderables.RenderableRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.util.DrawUtil;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.config.Preferences;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import it.unimi.dsi.fastutil.booleans.Boolean2ObjectFunction;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;

import java.util.List;
import java.util.function.BooleanSupplier;

public record ToggleableHelper(
        BooleanConsumer toggler,
        BooleanSupplier toggled,
        Boolean2ObjectFunction<Component> text
) implements RenderableRect {
    public static final Sprite RADIO_OFF_SPRITE = Sprite.of16(ResourceUtil.getIcon("radio_off"));
    public static final Sprite RADIO_ON_SPRITE = Sprite.of16(ResourceUtil.getIcon("radio_on"));

    public ToggleableHelper(Preferences.Option<Boolean> pref) {
        this(pref::set, pref::get, enabled -> ResourceUtil.getText("preferences.widgets." + pref.getKey()));
    }

    public void toggle() {
        this.toggler.accept(!this.toggled.getAsBoolean());
    }

    public void buildContext(ContextMenuItemBuilder builder) {
        builder.add(this.itemBuilder().build());
    }

    public MenuItemBuilder itemBuilder() {
        return MenuItem.builder(this.text.apply(this.toggled.getAsBoolean()))
                .background(GuiConstants.DEVELOPER_MODE_ITEM_BACKGROUND)
                .icon(() -> getDefaultIcon(this.toggled.getAsBoolean()))
                .action(this::toggle);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int width, int height, float partialTick) {
        final boolean toggled = this.toggled.getAsBoolean();
        guiGraphics.fill(x, y, x + width, y + height, getDefaultForeground(toggled));
        DrawUtil.renderOutline(guiGraphics, x, y, width, height, getDefaultBorder(toggled));
    }

    public static int getDefaultForeground(boolean enabled) {
        return enabled ? Theme.GREEN_500.withAlpha(0.5f) : Theme.RED_700.withAlpha(0.5f);
    }

    public static int getDefaultBorder(boolean enabled) {
        return enabled ? Theme.GREEN_500.getARGB() : Theme.RED_700.getARGB();
    }

    public static Sprite getDefaultIcon(boolean enabled) {
        return enabled ? RADIO_ON_SPRITE : RADIO_OFF_SPRITE;
    }

    public static MenuItem fromPref(Preferences.Option<Boolean> pref) {
        return new ToggleableHelper(pref)
                .itemBuilder()
                .closeOnInteract(false)
                .build();
    }

    public static <T extends FidgetzButton.Builder<?, ?>> T applyPref(Preferences.Option<Boolean> pref, T builder) {
        if (Config.get().isDevMode()) {
            ToggleableHelper toggleablePref = new ToggleableHelper(pref);
            builder.setForeground(toggleablePref).setContextMenuBuilder((btn, b) -> toggleablePref.buildContext(b.separatorIfNonEmpty()));
        }
        return builder;
    }

    public static List<MenuItem> preferences() {
        Preferences prefs = Preferences.INSTANCE;
        ContextMenuItemBuilder builder = new ContextMenuItemBuilder();

        builder.add(fromPref(prefs.originalScreenWidget));
        builder.add(fromPref(prefs.optionsWidget));
        builder.add(fromPref(prefs.actionBarWidget));
        builder.add(fromPref(prefs.toggleIncompatibleWidget));
        builder.add(fromPref(prefs.folderPackWidget));

        return builder.build();
    }
}
