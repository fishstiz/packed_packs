package io.github.fishstiz.testmod.tests;

import io.github.fishstiz.packed_packs.api.PackedPacksApi;
import io.github.fishstiz.packed_packs.api.Preference;
import io.github.fishstiz.packed_packs.api.events.InitializeLayoutEvent;
import io.github.fishstiz.testmod.TestFeature;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

public final class PreferenceRegistryTest {
    private enum OptionKey {
        BOOL, INT, FLOAT, DOUBLE, STRING
    }

    private static Identifier appendPath(Identifier parentId, String path) {
        return parentId.withPath(p -> p + "." + path);
    }

    public static void initialize(PackedPacksApi api, List<Identifier> dependencies) {
        TestFeature feature = TestFeature.builder(api.preferences(), "preference_registry", "Preference Registry")
                .after(dependencies)
                .register(api.eventBus());

        Identifier visibleOptionId = appendPath(feature.id(), "visible_option");
        Preference<OptionKey> visibleOptions = api.preferences().register(visibleOptionId, OptionKey.class, OptionKey.BOOL);
        Map<OptionKey, Option<?>> optionMap = Map.of(
                OptionKey.BOOL, new Option<>(OptionKey.BOOL, api.preferences().register(appendPath(visibleOptionId, "pref_bool"), true), v -> !v),
                OptionKey.INT, new Option<>(OptionKey.INT, api.preferences().register(appendPath(visibleOptionId, "pref_int"), 0), v -> v + 1),
                OptionKey.FLOAT, new Option<>(OptionKey.FLOAT, api.preferences().register(appendPath(visibleOptionId, "pref_float"), 3.14f), v -> v + 0.1f),
                OptionKey.DOUBLE, new Option<>(OptionKey.DOUBLE, api.preferences().register(appendPath(visibleOptionId, "pref_double"), 1.618), v -> v + 0.1),
                OptionKey.STRING, new Option<>(OptionKey.STRING, api.preferences().register(appendPath(visibleOptionId, "pref_string"), "hello world"), v -> v + "!")
        );

        api.eventBus().register(InitializeLayoutEvent.class, feature.id(), dependencies, event -> {
            if (!feature.isEnabled()) return;

            PreferenceWidget widget = new PreferenceWidget(visibleOptions, optionMap);
            event.addWidget(
                    InitializeLayoutEvent.Pos.AFTER_TITLE,
                    event.screenContext().wrapWithContextMenu(widget, (w, sink) -> {
                        sink.addItem(item -> item
                                .label(Component.literal("Visible Options"))
                                .applyDevStyle()
                                .separatorAbove()
                                .children(widget.options.values(), (option, child) -> child
                                        .label(Component.literal(option.optionKey().name()))
                                        .action(() -> w.setVisible(option.optionKey())))
                                .applyDevStyle()
                                .closeOnInteract(false)
                                .child(reset -> reset
                                        .label(Component.literal("Reset All"))
                                        .action(() -> widget.options.values().forEach(Option::reset))
                                        .closeOnInteract(false)
                                        .applyDevStyle()));
                        sink.addItem(item -> item
                                .label(Component.literal("Reset Option"))
                                .applyDevStyle()
                                .separatorBelow()
                                .closeOnInteract(false)
                                .action(widget::resetCurrent));
                    })
            );
        });

        dependencies.add(feature.id());
    }

    private record Option<T>(OptionKey optionKey, Preference<T> preference, UnaryOperator<T> cycler) {
        T getValue() {
            return preference.get();
        }

        void cycle() {
            preference.set(cycler.apply(getValue()));
        }

        void reset() {
            preference.reset();
        }
    }

    private static final class PreferenceWidget extends AbstractButton {
        private final Preference<OptionKey> visibleOption;
        private final Map<OptionKey, Option<?>> options;

        private PreferenceWidget(Preference<OptionKey> visibleOption, Map<OptionKey, Option<?>> options) {
            super(0, 0, 150, 20, CommonComponents.EMPTY);
            this.visibleOption = visibleOption;
            this.options = options;
            this.updateMessage();
        }

        private Option<?> getCurrent() {
            return Objects.requireNonNull(options.get(visibleOption.get()));
        }

        public void setVisible(OptionKey optionKey) {
            visibleOption.set(optionKey);
            updateMessage();
        }

        public void resetCurrent() {
            getCurrent().reset();
            updateMessage();
        }

        private void updateMessage() {
            Option<?> current = getCurrent();
            setMessage(CommonComponents.optionNameValue(
                    Component.literal(current.optionKey().name()),
                    Component.literal(String.valueOf(current.getValue()))));
        }

        @Override
        public void onPress(@NonNull InputWithModifiers inputWithModifiers) {
            getCurrent().cycle();
            updateMessage();
        }

        @Override
        protected void renderContents(@NonNull GuiGraphics guiGraphics, int i, int i1, float v) {
            renderDefaultSprite(guiGraphics);
            renderDefaultLabel(guiGraphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE));
        }

        @Override
        protected void updateWidgetNarration(@NonNull NarrationElementOutput narrationElementOutput) {
            defaultButtonNarrationText(narrationElementOutput);
        }
    }
}
