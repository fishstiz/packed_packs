package io.github.fishstiz.packed_packs.gui.components;

import io.github.fishstiz.fidgetz.v0.gui.components.FZContextMenu;
import io.github.fishstiz.fidgetz.v0.gui.components.FZPopoverMenuItem;
import io.github.fishstiz.fidgetz.v0.gui.components.WrappedComponent;
import io.github.fishstiz.packed_packs.api.Preference;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.config.Preferences;
import io.github.fishstiz.packed_packs.util.Colors;
import io.github.fishstiz.packed_packs.util.GuiUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public class PreferenceHelper extends WrappedComponent implements FZContextMenu.Source {
    private static final int ENABLED_COLOR = Colors.alpha(Colors.GREEN_500, 0.5f);
    private static final int DISABLED_COLOR = Colors.alpha(Colors.RED_700, 0.5f);
    private static final int ENABLED_BORDER = Colors.GREEN_500;
    private static final int DISABLED_BORDER = Colors.RED_700;
    private final Preference<Boolean> preference;
    private final Component label;

    private PreferenceHelper(AbstractWidget widget, Preference<Boolean> preference, Component label) {
        super(widget);
        this.preference = preference;
        this.label = label;
    }

    private PreferenceHelper(AbstractWidget widget, Preferences.Option<Boolean> preference) {
        super(widget);
        this.preference = preference;
        this.label = getOptionLabel(preference);
    }

    public static Component getOptionLabel(Preferences.Option<Boolean> preference) {
        return Component.translatable("packed_packs.preferences.widgets." + preference.getKey());
    }

    public static Optional<AbstractWidget> wrap(Preferences.Option<Boolean> option, AbstractWidget widget) {
        if (Config.get().isDevMode()) {
            return Optional.of(new PreferenceHelper(widget, option));
        }
        return option.get() ? Optional.of(widget) : Optional.empty();
    }

    public static AbstractWidget wrapNonNull(Preferences.Option<Boolean> option, AbstractWidget widget) {
        if (Config.get().isDevMode()) {
            return new PreferenceHelper(widget, option);
        }
        return widget;
    }

    public static @Nullable AbstractWidget wrap(@Nullable AbstractWidget widget, Preference<Boolean> preference, Component label) {
        if (widget == null) {
            return null;
        }
        if (Config.get().isDevMode()) {
            return new PreferenceHelper(widget, preference, label);
        }
        return preference.get() ? widget : null;
    }

    public static FZPopoverMenuItem createEntry(Preference<Boolean> preference, Component label) {
        return GuiUtils.buildDevEntry(FZPopoverMenuItem.builder())
                .message(label)
                .icon(GuiUtils.toggleRect(preference::get))
                .onPress(() -> preference.set(!preference.get()))
                .closeOnInteraction(false)
                .build();
    }

    public static FZPopoverMenuItem createEntry(Preferences.Option<Boolean> preference) {
        return createEntry(preference, getOptionLabel(preference));
    }

    public static void extractOverlay(GuiGraphics graphics, Preference<Boolean> preference, int x, int y, int width, int height) {
        boolean enabled = preference.get();
        int color = enabled ? ENABLED_COLOR : DISABLED_COLOR;
        int border = enabled ? ENABLED_BORDER : DISABLED_BORDER;
        graphics.fill(x, y, x + width, y + height, color);
        graphics.renderOutline(x, y, width, height, border);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        if (widget.visible) {
            extractOverlay(graphics, preference, getX(), getY(), getWidth(), getHeight());
        }
    }

    @Override
    public void fidgetz$updateContextEntries(double x, double y, FZContextMenu.Collector collector) {
        FZContextMenu.Source.super.fidgetz$updateContextEntries(x, y, collector);
        if (widget.isActive()) {
            collector.addEntry(createEntry(preference, label));
        }
    }
}
