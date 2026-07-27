package io.github.fishstiz.fidgetz.gui.components;

import io.github.fishstiz.fidgetz.gui.renderables.sprites.ButtonSprites;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CyclicButton<T extends CyclicButton.Option, E> extends FidgetzButton<E> {
    private final @Nullable Component prefix;
    private final List<Consumer<T>> listeners;
    private final T[] options;
    private final boolean allowReverseClick;
    private int value = 0;

    private CyclicButton(Builder<T, E> builder) {
        super(builder);

        this.listeners = builder.listeners;
        this.options = builder.options;
        this.prefix = builder.prefix;
        this.allowReverseClick = builder.allowReverseClick;

        this.setValueSilently(builder.value);
    }

    private void setValue(T value, boolean silent) {
        for (int i = 0; i < this.options.length; i++) {
            if (this.options[i] == value) {
                this.value = i;
                break;
            }
        }
        if (!silent) {
            this.informListeners();
        }
        this.updateMessage();
    }

    public void setValue(T value) {
        this.setValue(value, false);
    }

    public void setValueSilently(T value) {
        this.setValue(value, true);
    }

    public T getValue() {
        return this.options[this.value];
    }

    @Override
    public void onPress() {
        if (this.allowReverseClick && Screen.hasShiftDown()) {
            this.value = this.value <= 0 ? this.options.length - 1 : this.value - 1;
        } else {
            this.value = this.value >= this.options.length - 1 ? 0 : this.value + 1;
        }

        this.updateMessage();
        this.informListeners();
    }

    private void updateMessage() {
        this.setMessage(this.prefix != null
                ? this.prefix.copy().append(": ").append(this.getValue().text())
                : this.getValue().text()
        );
        this.setTooltip(this.getValue().tooltip());
    }

    private void informListeners() {
        T option = this.getValue();
        for (var listener : this.listeners) {
            listener.accept(option);
        }
    }

    public void addListener(Consumer<T> listener) {
        this.listeners.add(listener);
    }

    @Override
    protected boolean hasSprite() {
        return super.hasSprite() || this.getValue() instanceof SpriteOption;
    }

    @Override
    protected void renderSprite(GuiGraphics guiGraphics, int x, int y, int width, int height, float partialTick) {
        if (!(this.getValue() instanceof SpriteOption spriteOption)) {
            super.renderSprite(guiGraphics, x, y, width, height, partialTick);
            return;
        }

        ButtonSprites sprites = spriteOption.sprites();
        if (sprites != null) {
            sprites.render(guiGraphics, x, y, width, height, this.active, partialTick);
        }
    }

    public static <E> Builder<Option, E> builder(Component... components) {
        Option[] options = new Option[components.length];

        for (int i = 0; i < components.length; i++) {
            options[i] = Option.create(components[i]);
        }

        return new Builder<>(options);
    }

    public static <T extends Option, E> Builder<T, E> builder(T[] options) {
        return new Builder<>(options);
    }

    public static class Builder<T extends Option, E> extends FidgetzButton.Builder<E, Builder<T, E>> {
        private final List<Consumer<T>> listeners = new ArrayList<>();
        private final T[] options;
        private T value;
        private Component prefix;
        private boolean allowReverseClick = true;

        private Builder(T[] options) {
            this.options = options;
        }

        public Builder<T, E> setValue(T value) {
            this.value = value;
            return this;
        }

        public Builder<T, E> setPrefix(Component prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder<T, E> disableReverseClick() {
            this.allowReverseClick = false;
            return this;
        }

        public Builder<T, E> addListener(Consumer<T> listener) {
            this.listeners.add(listener);
            return this;
        }

        @Override
        public CyclicButton<T, E> build() {
            return new CyclicButton<>(this);
        }
    }

    public interface Option {
        @NotNull Component text();

        default @Nullable Tooltip tooltip() {
            return null;
        }

        static Option create(Component component) {
            return () -> component;
        }
    }

    public interface SpriteOption extends Option {
        @Nullable ButtonSprites sprites();
    }
}
