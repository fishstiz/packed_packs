package io.github.fishstiz.packed_packs.gui.components;

import io.github.fishstiz.fidgetz.gui.components.ContainedWidget;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuContainer;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.MenuItem;
import io.github.fishstiz.fidgetz.gui.renderables.RenderableRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.util.DrawUtil;
import io.github.fishstiz.packed_packs.api.PreferenceRegistry;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.config.Preferences;
import io.github.fishstiz.packed_packs.impl.PackedPacksApiImpl;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public record PreferenceToggle(Preferences.Option<Boolean> option, Component text) implements RenderableRect, MenuItem {
    public PreferenceToggle(Preferences.Option<Boolean> preference) {
        this(preference, ResourceUtil.getText("preferences.widgets." + preference.getKey()));
    }

    public static @Nullable PreferenceToggle fromKey(PreferenceRegistry.Key<Boolean> key) {
        Preferences.Option<Boolean> option = PackedPacksApiImpl.getInstance().preferences().getOption(key);
        if (option == null) return null;
        return new PreferenceToggle(option);
    }

    public static List<PreferenceToggle> standardOptions() {
        return List.of(
                new PreferenceToggle(Preferences.ORIGINAL_SCREEN_WIDGET),
                new PreferenceToggle(Preferences.OPTIONS_WIDGET),
                new PreferenceToggle(Preferences.ACTION_BAR_WIDGET),
                new PreferenceToggle(Preferences.INCOMPATIBLE_TOGGLE_WIDGET),
                new PreferenceToggle(Preferences.FOLDER_PACK_WIDGET)
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int width, int height, float partialTick) {
        final boolean toggled = this.option.get();
        guiGraphics.fill(x, y, x + width, y + height, getForeground(toggled));
        DrawUtil.renderOutline(guiGraphics, x, y, width, height, getBorder(toggled));
    }

    @Override
    public void run() {
        this.option.set(!this.option.get());
    }

    @Override
    public @Nullable Sprite icon() {
        return GuiConstants.getToggleIcon(this.option.get());
    }

    @Override
    public @Nullable RenderableRect background() {
        return GuiConstants.DEVELOPER_MODE_ITEM_BACKGROUND;
    }

    @Override
    public boolean shouldCloseOnInteract() {
        return false;
    }

    public void updateBuilder(ContextMenuItemBuilder builder) {
        builder.separatorIfNonEmpty().add(this);
    }

    private static int getForeground(boolean enabled) {
        return enabled ? Theme.GREEN_500.withAlpha(0.5f) : Theme.RED_700.withAlpha(0.5f);
    }

    private static int getBorder(boolean enabled) {
        return enabled ? Theme.GREEN_500.getARGB() : Theme.RED_700.getARGB();
    }

    public static @Nullable AbstractWidget wrap(Preferences.@Nullable Option<Boolean> option, @Nullable AbstractWidget widget) {
        if (widget == null || option == null) {
            return null;
        }
        if (Config.get().isDevMode()) {
            return new Wrapped(new PreferenceToggle(option), widget);
        }
        if (Boolean.TRUE.equals(option.get())) {
            return widget;
        }
        return null;
    }

    public static @Nullable AbstractWidget wrap(PreferenceRegistry.Key<Boolean> key, @Nullable AbstractWidget widget) {
        return wrap(PackedPacksApiImpl.getInstance().preferences().getOption(key), widget);
    }

    public static <T> Optional<Bound<T>> bind(Preferences.Option<Boolean> option, T obj) {
        if (obj == null) {
            return Optional.empty();
        }
        if (Config.get().isDevMode()) {
            return Optional.of(new Bound<>(obj, new PreferenceToggle(option)));
        }
        if (Boolean.TRUE.equals(option.get())) {
            return Optional.of(new Bound<>(obj, null));
        }
        return Optional.empty();
    }

    public record Bound<T>(T value, @Nullable PreferenceToggle toggle) {
        public <U> @Nullable U apply(Function<@NonNull PreferenceToggle, U> function) {
            if (this.toggle == null) return null;
            return function.apply(this.toggle);
        }
    }

    private static final class Wrapped extends ContainedWidget implements ContextMenuContainer {
        final PreferenceToggle overlay;

        public Wrapped(PreferenceToggle overlay, AbstractWidget widget) {
            super(widget);
            this.overlay = overlay;
        }

        @Override
        public void buildItems(ContextMenuItemBuilder builder, int mouseX, int mouseY) {
            ContextMenuContainer.super.buildItems(builder.add(this.overlay), mouseX, mouseY);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
            this.overlay.render(guiGraphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), partialTick);
        }
    }
}
