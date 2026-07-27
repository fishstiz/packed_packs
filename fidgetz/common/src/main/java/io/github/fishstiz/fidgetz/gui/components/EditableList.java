package io.github.fishstiz.fidgetz.gui.components;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.fishstiz.fidgetz.util.ARGBColor;
import io.github.fishstiz.fidgetz.util.debounce.PollingDebouncer;
import io.github.fishstiz.fidgetz.util.debounce.SimplePollingDebouncer;
import io.github.fishstiz.fidgetz.util.lang.CollectionsUtil;
import io.github.fishstiz.fidgetz.util.text.TextStylizer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class EditableList<T> extends AbstractFixedListWidget<EditableList<T>.AbstractEntry> {
    private static final int DEFAULT_MAX_TEXT_LENGTH = 32;
    private static final int UPDATE_DELAY_MS = 100;
    private final PollingDebouncer<Void> updateListener;
    private final Function<T, String> textMapper;
    private final Function<String, T> valueMapper;
    private final @Nullable Predicate<String> textFilter;
    private final @Nullable Predicate<String> saveValidator;
    private final List<TextStylizer> textStylizers;
    private final int maxLength;

    protected EditableList(Builder<T> builder) {
        super(builder.itemHeight);
        this.textMapper = builder.textMapper;
        this.valueMapper = builder.valueMapper;

        Consumer<List<T>> listener = builder.updateListener;
        this.updateListener = new SimplePollingDebouncer<>(() -> {
            if (listener != null) listener.accept(this.extractItems());
        }, UPDATE_DELAY_MS);

        this.textFilter = builder.textFilter;
        this.saveValidator = builder.saveValidator;
        this.maxLength = builder.maxTextLength;
        this.textStylizers = builder.textStylizers.isEmpty() ? Collections.emptyList() : builder.textStylizers;

        this.setPosition(builder.x, builder.y);
        this.setSize(builder.width, builder.height);
        this.setItems(builder.items);
    }

    public void setItems(List<T> items) {
        List<AbstractEntry> entries = this.children();
        AbstractEntry lastEntry = !entries.isEmpty() ? entries.getLast() : null;

        this.clearEntries();

        for (T item : items) {
            this.addEntry(new Entry(item));
        }

        if (lastEntry instanceof DirtyEntry dirtyEntry && !dirtyEntry.removed) {
            this.addEntry(new DirtyEntry(dirtyEntry.editBox.getValue()));
        } else {
            this.addEntry(new DirtyEntry());
        }

        this.clampScrollAmount();
    }

    public List<T> extractItems() {
        return CollectionsUtil.mapIf(this.children(), AbstractEntry::canSave, AbstractEntry::dirtyItem, ObjectArrayList::new);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        this.updateListener.poll();
    }

    protected abstract class AbstractEntry extends AbstractFixedListWidget<AbstractEntry>.Entry {
        protected final List<AbstractWidget> widgets;
        protected final ToggleableEditBox<Void> editBox;
        protected final FidgetzButton<Void> actionButton;
        protected final T item;

        protected AbstractEntry(@Nullable T item, int textColor, Component actionIcon) {
            this.item = item;
            this.actionButton = FidgetzButton.<Void>builder()
                    .makeSquare()
                    .setMessage(actionIcon)
                    .setOnPress(this::runAction)
                    .build();
            this.editBox = ToggleableEditBox.<Void>builder()
                    .setValue(item != null ? EditableList.this.textMapper.apply(item) : "")
                    .setEditable(true)
                    .setHint(CommonComponents.ELLIPSIS)
                    .setFilter(EditableList.this.textFilter)
                    .setMaxLength(EditableList.this.maxLength)
                    .addTextStylizer(EditableList.this.textStylizers)
                    .setTextColor(textColor)
                    .addListener(value -> {
                        this.actionButton.active = this.canRun(value);
                        EditableList.this.updateListener.run();
                    })
                    .build();
            this.editBox.allowPastingSectionSign(true);
            this.actionButton.active = !this.editBox.getValue().isEmpty();

            this.widgets = List.of(editBox, actionButton);
        }

        protected abstract boolean canSave();

        protected abstract boolean canRun(String value);

        protected abstract void runAction();

        public T dirtyItem() {
            return EditableList.this.valueMapper.apply(this.editBox.getValue());
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            this.editBox.setWidth(this.getWidth() - this.actionButton.getWidth());
            this.editBox.setPosition(this.getX(), this.getY());
            this.actionButton.setPosition(this.editBox.getRight(), this.getY());

            this.editBox.render(guiGraphics, mouseX, mouseY, partialTick);
            this.actionButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public @NotNull List<AbstractWidget> children() {
            return this.widgets;
        }

        @Override
        public @NotNull List<AbstractWidget> narratables() {
            return this.widgets;
        }

        @Override
        public void visitWidgets(Consumer<AbstractWidget> consumer) {
            this.widgets.forEach(consumer);
        }
    }

    protected class Entry extends AbstractEntry {
        private static final Component ACTION_ICON = Component.literal("-");
        private boolean removed = false;

        Entry(T value) {
            super(value, ARGBColor.WHITE, ACTION_ICON);
        }

        @Override
        protected boolean canRun(String value) {
            return true;
        }

        @Override
        protected boolean canSave() {
            return !this.removed && !this.editBox.getValue().isBlank();
        }

        @Override
        protected void runAction() {
            this.removed = true;
            EditableList.this.setItems(EditableList.this.extractItems());
            EditableList.this.updateListener.run();
        }
    }

    protected class DirtyEntry extends AbstractEntry {
        private static final int TEXT_COLOR = ARGBColor.withAlpha(ARGBColor.WHITE, 0.65f);
        private static final Component ACTION_ICON = Component.literal("+");
        private boolean removed = false;

        protected DirtyEntry() {
            super(null, TEXT_COLOR, ACTION_ICON);
        }

        protected DirtyEntry(String value) {
            this();
            this.editBox.setValueSilently(value);
            this.actionButton.active = !this.editBox.getValue().isEmpty();
        }

        @Override
        protected boolean canSave() {
            return false;
        }

        @Override
        protected boolean canRun(String value) {
            return value != null &&
                   !value.isBlank() &&
                   (EditableList.this.saveValidator == null || EditableList.this.saveValidator.test(value));
        }

        @Override
        protected void runAction() {
            this.removed = true;

            List<T> items = EditableList.this.extractItems();
            items.add(this.dirtyItem());

            EditableList.this.setItems(items);
            EditableList.this.updateListener.run();
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == InputConstants.KEY_RETURN && this.canRun(this.editBox.getValue())) {
                this.runAction();
                return true;
            }

            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    public static <T> Builder<T> builder(List<T> items, Function<T, String> textMapper, Function<String, T> valueMapper) {
        return new Builder<>(items, textMapper, valueMapper);
    }

    public static Builder<String> builder(List<String> items) {
        return builder(items, Function.identity(), Function.identity());
    }

    public static Builder<String> builder() {
        return builder(Collections.emptyList(), Function.identity(), Function.identity());
    }

    public static class Builder<T> extends AbstractWidgetBuilder<Builder<T>> {
        private final List<T> items;
        private final Function<T, String> textMapper;
        private final Function<String, T> valueMapper;
        private Consumer<List<T>> updateListener;
        private Predicate<String> textFilter;
        private Predicate<String> saveValidator;
        private int itemHeight = DEFAULT_HEIGHT;
        private int maxTextLength = DEFAULT_MAX_TEXT_LENGTH;
        private final List<TextStylizer> textStylizers = new ObjectArrayList<>();

        private Builder(List<T> items, Function<T, String> textMapper, Function<String, T> valueMapper) {
            this.items = items;
            this.textMapper = textMapper;
            this.valueMapper = valueMapper;
        }

        public Builder<T> setItemHeight(int itemHeight) {
            this.itemHeight = itemHeight;
            return this;
        }

        public Builder<T> setListener(Consumer<List<T>> listener) {
            this.updateListener = listener;
            return this;
        }

        public Builder<T> setTextFilter(Predicate<String> textFilter) {
            this.textFilter = textFilter;
            return this;
        }

        public Builder<T> addTextStylizer(TextStylizer textStylizer) {
            this.textStylizers.add(textStylizer);
            return this;
        }

        public Builder<T> addTextStylizer(Collection<TextStylizer> textStylizers) {
            this.textStylizers.addAll(textStylizers);
            return this;
        }

        public Builder<T> setSaveValidator(Predicate<String> saveValidator) {
            this.saveValidator = saveValidator;
            return this;
        }

        public Builder<T> setMaxTextLength(int maxTextLength) {
            this.maxTextLength = maxTextLength;
            return this;
        }

        public EditableList<T> build() {
            return new EditableList<>(this);
        }
    }
}
