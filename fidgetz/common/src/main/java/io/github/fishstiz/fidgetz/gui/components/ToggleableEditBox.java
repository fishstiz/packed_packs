package io.github.fishstiz.fidgetz.gui.components;

import io.github.fishstiz.fidgetz.util.lang.FunctionsUtil;
import io.github.fishstiz.fidgetz.util.text.AsyncStylizerFormatter;
import io.github.fishstiz.fidgetz.transform.mixin.EditBoxAccess;
import io.github.fishstiz.fidgetz.gui.Metadata;
import io.github.fishstiz.fidgetz.util.LogUtil;
import io.github.fishstiz.fidgetz.util.text.TextStylizer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ToggleableEditBox<E> extends EditBox implements Fidgetz, Metadata<E> {
    private static final int DEFAULT_MAX_LENGTH = 32;
    private final List<Consumer<String>> listeners;
    private final int hintColor;
    private E metadata;
    private int focusedTextColor;
    private String previousValue;
    private Component inactiveText = CommonComponents.EMPTY;

    private ToggleableEditBox(Builder<E> builder) {
        super(builder.font, builder.x, builder.y, builder.width, builder.height, Component.literal(builder.value));

        this.hintColor = builder.hintColor != null ? builder.hintColor : ((EditBoxAccess) this).getTextColorUneditable();

        this.focusedTextColor = builder.textColor != null ? builder.textColor : ((EditBoxAccess) this).getTextColor();
        this.previousValue = builder.value;
        this.metadata = builder.metadata;

        if (builder.filter != null) this.setFilter(builder.filter);

        this.setMaxLength(builder.maxLength);
        this.setValue(builder.value);
        this.setEditable(builder.editable);
        this.setHint(builder.hint);
        this.setTextShadow(builder.textShadow);
        this.updateTextColor();

        if (!builder.textStylizers.isEmpty()) {
            this.addFormatter(new AsyncStylizerFormatter(builder.textStylizers, this::getValue));
        }

        this.listeners = builder.listeners;

        super.setResponder(this::onRespond);
    }

    public void toggle() {
        this.setEditable(!this.isEditing());
    }

    public boolean isEditing() {
        return ((EditBoxAccess) this).invokeIsEditable();
    }

    @Override
    public void setEditable(boolean enabled) {
        super.setEditable(enabled);
        this.moveCursorToStart(false);
        this.active = enabled;
        this.updateInactiveText(this.getValue());
        if (!enabled) this.setFocused(false);
    }

    public Component getInactiveText() {
        return this.inactiveText;
    }

    @Override
    public void setTextColor(int color) {
        super.setTextColor(color);
        this.setTextColorUneditable(color);
        this.focusedTextColor = color;
    }

    private void updateTextColor() {
        int color = !this.getValue().isEmpty() || this.isFocused() ? this.focusedTextColor : this.hintColor;
        super.setTextColor(color);
        this.setTextColorUneditable(color);
    }

    public void setValueSilently(String value) {
        value = value != null ? value : "";
        super.setResponder(FunctionsUtil.nopConsumer());
        this.setValue(value);
        this.updateTextColor();

        String newValue = this.getValue();
        if (!Objects.equals(this.previousValue, newValue)) {
            this.previousValue = this.getValue();
            this.updateInactiveText(value);
        }

        super.setResponder(this::onRespond);
    }

    private void onRespond(String value) {
        this.updateTextColor();

        if (!Objects.equals(this.previousValue, value)) {
            this.previousValue = value;
            this.updateInactiveText(value);

            for (var listener : listeners) {
                listener.accept(value);
            }
        }
    }

    private void updateInactiveText(String value) {
        if (!this.isEditing()) {
            this.inactiveText = Component.literal(value);
        }
    }

    @Override
    public final void setResponder(Consumer<String> responder) {
        LogUtil.logUnsupported("Use addListener instead of setResponder.");
    }

    public void addListener(Consumer<String> listener) {
        this.listeners.add(listener);
    }

    @Override
    public boolean isBordered() {
        return this.isEditing();
    }

    @Override
    public boolean isFocused() {
        return this.isEditing() && super.isFocused();
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        this.updateTextColor();
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.isVisible() && Fidgetz.super.isMouseOver(mouseX, mouseY);
    }

    @Override
    public boolean isHovered() {
        return this.isEditing() && super.isHovered();
    }

    @Override
    public boolean isActive() {
        return this.isEditing() && this.active && this.visible;
    }

    @Override
    public void onClick(MouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
        if (this.isEditing()) super.onClick(mouseButtonEvent, doubleClicked);
    }

    public void allowPastingSectionSign(boolean allow) {
        ((EditBoxAccess) this).fidgetz$allowPastingSectionSign(allow);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (this.getValue().isEmpty()) {
            if (keyEvent.isLeft()) {
                return false;
            }
            if (keyEvent.isRight()) {
                return false;
            }
        }

        return super.keyPressed(keyEvent);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
        return this.isEditing() && super.mouseClicked(mouseButtonEvent, doubleClicked);
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
        if (!this.isEditing()) return null;
        return super.nextFocusPath(event);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.isHovered = this.isHovered && Fidgetz.super.isHovered(mouseX, mouseY);
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public E getMetadata() {
        return this.metadata;
    }

    @Override
    public void setMetadata(E metadata) {
        this.metadata = metadata;
    }

    public static <E> Builder<E> builder(Font font) {
        return new Builder<>(font);
    }

    public static <E> Builder<E> builder() {
        return builder(Minecraft.getInstance().font);
    }

    public static class Builder<E> extends AbstractWidgetBuilder<Builder<E>> {
        private final Font font;
        private final List<Consumer<String>> listeners = new ObjectArrayList<>();
        private final List<TextStylizer> textStylizers = new ObjectArrayList<>();
        private String value = "";
        private Component hint = CommonComponents.EMPTY;
        private boolean textShadow = true;
        private Integer textColor;
        private Integer hintColor;
        private boolean editable = false;
        private int maxLength = DEFAULT_MAX_LENGTH;
        private Predicate<String> filter;
        private E metadata;

        private Builder(Font font) {
            this.font = font;
        }

        public Builder<E> setValue(String value) {
            this.value = value;
            return this;
        }

        public Builder<E> setHint(Component hint) {
            this.hint = hint;
            return this;
        }

        public Builder<E> setEditable(boolean editable) {
            this.editable = editable;
            return this;
        }

        public Builder<E> setTextShadow(boolean textShadow) {
            this.textShadow = textShadow;
            return this;
        }

        public Builder<E> setTextColor(Integer textColor) {
            this.textColor = textColor;
            return this;
        }

        public Builder<E> setHintColor(Integer hintColor) {
            this.hintColor = hintColor;
            return this;
        }

        public Builder<E> setMaxLength(int maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        public Builder<E> setFilter(Predicate<String> filter) {
            this.filter = filter;
            return this;
        }

        public Builder<E> addListener(Consumer<String> listener) {
            this.listeners.add(listener);
            return this;
        }

        public Builder<E> addTextStylizer(TextStylizer textStylizer) {
            this.textStylizers.add(textStylizer);
            return this;
        }

        public Builder<E> addTextStylizer(Collection<TextStylizer> textStylizer) {
            this.textStylizers.addAll(textStylizer);
            return this;
        }

        public Builder<E> setMetadata(E metadata) {
            this.metadata = metadata;
            return this;
        }

        public ToggleableEditBox<E> build() {
            return new ToggleableEditBox<>(this);
        }
    }
}
