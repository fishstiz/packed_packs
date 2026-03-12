package io.github.fishstiz.fidgetz.gui.components;

import io.github.fishstiz.fidgetz.gui.renderables.sprites.ButtonSprites;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ToggleButton<E> extends FidgetzButton<E> {
    private final List<Consumer<Boolean>> listeners;
    private final boolean prefixMessage;
    private final Sprites toggleSprites;
    private Component prefix;
    private boolean value;

    protected ToggleButton(ToggleBuilder<E> builder) {
        super(builder);

        this.toggleSprites = builder.toggleSprites;
        this.listeners = builder.listeners;
        this.prefixMessage = builder.prefixMessage;
        this.value = builder.value;
        this.prefix = this.getMessage();

        this.updateMessage();
    }

    private void setValue(boolean value, boolean silent) {
        this.value = value;

        if (!silent) {
            for (var listener : this.listeners) {
                listener.accept(this.getValue());
            }
        }

        this.updateMessage();
    }

    public void setValue(boolean value) {
        this.setValue(value, false);
    }

    public void setValueSilently(boolean value) {
        this.setValue(value, true);
    }

    public boolean getValue() {
        return this.value;
    }

    @Override
    public void onPress(@NonNull InputWithModifiers inputWithModifiers) {
        super.onPress(inputWithModifiers);
        this.setValue(!this.getValue());
    }

    public void addListener(Consumer<Boolean> listener) {
        this.listeners.add(listener);
    }

    @Override
    public void setMessage(@NonNull Component message) {
        this.prefix = message;
        this.updateMessage();
    }

    public Component getPrefix() {
        return this.prefix;
    }

    private void updateMessage() {
        Component valueText = this.getValue() ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF;

        if (this.prefixMessage) {
            super.setMessage(CommonComponents.optionNameValue(this.getPrefix(), valueText));
        } else {
            super.setMessage(valueText);
        }
    }

    @Override
    protected boolean hasSprite() {
        return super.hasSprite() || this.toggleSprites != null;
    }

    @Override
    protected void renderSprite(GuiGraphics guiGraphics, int x, int y, int width, int height, float partialTick) {
        if (this.toggleSprites == null) {
            super.renderSprite(guiGraphics, x, y, width, height, partialTick);
            return;
        }

        this.toggleSprites.get(this.value).render(guiGraphics, x, y, width, height, this.active, partialTick);
    }

    public static <E> ToggleBuilder<E> builder() {
        return new ToggleBuilder<>();
    }

    public static class ToggleBuilder<E> extends Builder<E, ToggleBuilder<E>> {
        private final List<Consumer<Boolean>> listeners = new ArrayList<>();
        private boolean value = false;
        private boolean prefixMessage = true;
        private Sprites toggleSprites;

        protected ToggleBuilder() {
        }

        public ToggleBuilder<E> setValue(boolean value) {
            this.value = value;
            return this;
        }

        public ToggleBuilder<E> setPrefixMessage(boolean prefixMessage) {
            this.prefixMessage = prefixMessage;
            return this;
        }

        public ToggleBuilder<E> addListener(Consumer<Boolean> listener) {
            this.listeners.add(listener);
            return this;
        }

        public ToggleBuilder<E> setSprite(Sprites toggleSprites) {
            this.toggleSprites = toggleSprites;
            return this;
        }

        @Override
        public ToggleButton<E> build() {
            return new ToggleButton<>(this);
        }
    }

    public record Sprites(ButtonSprites toggled, ButtonSprites untoggled) {
        public static Sprites of(Sprite toggled, Sprite untoggled) {
            return new Sprites(ButtonSprites.of(toggled), ButtonSprites.of(untoggled));
        }

        public Sprite get(boolean toggled, boolean active) {
            return this.get(toggled).get(active);
        }

        public ButtonSprites get(boolean toggled) {
            return toggled ? this.toggled : this.untoggled;
        }
    }
}
