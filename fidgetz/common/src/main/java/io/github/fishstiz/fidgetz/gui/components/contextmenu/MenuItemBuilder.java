package io.github.fishstiz.fidgetz.gui.components.contextmenu;

import io.github.fishstiz.fidgetz.gui.renderables.ColoredRect;
import io.github.fishstiz.fidgetz.gui.renderables.RenderableRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.util.ARGBColor;
import io.github.fishstiz.fidgetz.util.lang.FunctionsUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class MenuItemBuilder {
    private final List<MenuItem> children = new ObjectArrayList<>();
    private Component text;
    private Runnable action = FunctionsUtil.nop();
    private RenderableRect background;
    private Supplier<@Nullable Sprite> iconSupplier;
    private Supplier<Tooltip> tooltipSupplier;
    private boolean shouldCloseOnInteract = true;
    private boolean shouldAutoSeparate = true;
    private BooleanSupplier activeSupplier;
    private IntSupplier textColorSupplier;

    MenuItemBuilder(Component text) {
        this.text = text;
    }

    public void text(Component text) {
        this.text = text;
    }

    public MenuItemBuilder action(Runnable action) {
        this.action = action;
        return this;
    }

    public MenuItemBuilder background(@Nullable RenderableRect background) {
        this.background = background;
        return this;
    }

    public MenuItemBuilder background(int backgroundColor) {
        this.background = new ColoredRect(backgroundColor);
        return this;
    }

    public MenuItemBuilder icon(@Nullable Sprite icon) {
        this.iconSupplier = () -> icon;
        return this;
    }

    public MenuItemBuilder icon(Supplier<@Nullable Sprite> icon) {
        this.iconSupplier = icon;
        return this;
    }

    public MenuItemBuilder tooltip(Supplier<@Nullable Tooltip> tooltipSupplier) {
        this.tooltipSupplier = tooltipSupplier;
        return this;
    }

    public MenuItemBuilder tooltip(Tooltip tooltip) {
        return this.tooltip(() -> tooltip);
    }

    public MenuItemBuilder closeOnInteract(boolean value) {
        this.shouldCloseOnInteract = value;
        return this;
    }

    public MenuItemBuilder autoSeparate(boolean value) {
        this.shouldAutoSeparate = value;
        return this;
    }

    public MenuItemBuilder activeWhen(BooleanSupplier activeSupplier) {
        this.activeSupplier = activeSupplier;
        return this;
    }

    public MenuItemBuilder textColor(IntSupplier textColorSupplier) {
        this.textColorSupplier = textColorSupplier;
        return this;
    }

    public MenuItemBuilder textColor(int textColor) {
        this.textColorSupplier = () -> textColor;
        return this;
    }

    public MenuItemBuilder addChild(MenuItem child) {
        this.children.add(child);
        return this;
    }

    public MenuItemBuilder addChildren(Collection<MenuItem> children) {
        this.children.addAll(children);
        return this;
    }

    private void setDefaults() {
        if (this.iconSupplier == null) {
            this.iconSupplier = FunctionsUtil.nullSupplier();
        }
        if (this.tooltipSupplier == null) {
            this.tooltipSupplier = FunctionsUtil.nullSupplier();
        }
        if (this.activeSupplier == null) {
            this.activeSupplier = () -> true;
        }
        if (this.textColorSupplier == null) {
            this.textColorSupplier = () -> this.activeSupplier.getAsBoolean() ? ARGBColor.WHITE : ContextMenu.DEFAULT_TEXT_INACTIVE_COLOR;
        }
    }

    public MenuItem build() {
        this.setDefaults();

        return new MenuItemImpl(
                this.text,
                this.action,
                this.background,
                this.iconSupplier,
                this.tooltipSupplier,
                this.shouldCloseOnInteract,
                this.shouldAutoSeparate,
                this.activeSupplier,
                this.textColorSupplier,
                this.children
        );
    }

    private record MenuItemImpl(
            Component text,
            Runnable action,
            RenderableRect background,
            Supplier<@Nullable Sprite> iconSupplier,
            Supplier<@Nullable Tooltip> tooltipSupplier,
            boolean shouldCloseOnInteract,
            boolean shouldAutoSeparate,
            BooleanSupplier activeSupplier,
            IntSupplier textColorSupplier,
            List<MenuItem> children
    ) implements MenuItem {
        @Override
        public void run() {
            this.action.run();
        }

        @Override
        public boolean active() {
            return this.activeSupplier.getAsBoolean();
        }

        @Override
        public int textColor() {
            return this.textColorSupplier.getAsInt();
        }

        @Override
        public @Nullable Sprite icon() {
            return this.iconSupplier.get();
        }

        @Override
        public @Nullable Tooltip tooltip() {
            return this.tooltipSupplier.get();
        }
    }
}
