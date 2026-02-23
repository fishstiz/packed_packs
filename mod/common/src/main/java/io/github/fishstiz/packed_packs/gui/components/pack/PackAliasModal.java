package io.github.fishstiz.packed_packs.gui.components.pack;

import io.github.fishstiz.fidgetz.gui.components.*;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.fidgetz.util.lang.ObjectsUtil;
import io.github.fishstiz.fidgetz.util.text.PatternStylizer;
import io.github.fishstiz.fidgetz.util.text.TextStylizer;
import io.github.fishstiz.packed_packs.config.DevConfig;
import io.github.fishstiz.packed_packs.gui.intents.PackListIntent;
import io.github.fishstiz.packed_packs.gui.model.PackedPacksViewModel;
import io.github.fishstiz.packed_packs.gui.states.ActiveAction;
import io.github.fishstiz.packed_packs.gui.states.PackedPacksState;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import io.github.fishstiz.packed_packs.util.text.GroupCloseStylizer;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.SPACING;

public class PackAliasModal extends Modal<PackAliasModal.DelegatedLayout> {
    private final PackedPacksViewModel viewModel;

    public <S extends Screen & ToggleableDialogContainer> PackAliasModal(S screen, PackedPacksViewModel viewModel) {
        super(Modal.builder(screen, new DelegatedLayout(viewModel::dispatch)).padding(SPACING).setFocusOnOpen(true));
        this.viewModel = viewModel;
        this.viewModel.subscribe(PackedPacksState::editingAliases, this::refresh);
    }

    private void refresh(ActiveAction.@Nullable EditingAliases editContext) {
        if (editContext == null) {
            super.setOpen(false);
            this.root().layout().clear();
            this.clearWidgets();
            ObjectsUtil.ifPresent(this.getCurrentFocusPath(), p -> p.applyFocus(false));
            this.setFocused(null);
            return;
        }

        this.root().layout().refresh(editContext, this::addRenderableWidget, this::addRenderableOnly);
        this.repositionElements();
        super.setOpen(true);
    }

    @Override
    public void setOpen(boolean open) {
        var editContext = this.viewModel.state().editingAliases();
        if (!open && editContext != null) {
            List<String> aliases = ObjectsUtil.mapOrNull(this.root().layout().aliases, EditableList::extractItems);
            if (aliases != null) {
                this.viewModel.dispatch(new PackListIntent.CloseAliases(editContext.target(), editContext.ctx(), aliases));
            }
        }
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(@NonNull FocusNavigationEvent event) {
        return super.nextFocusPath(event);
    }

    static class DelegatedLayout implements Layout {
        private static final int LIST_WIDTH = 296;
        private static final int LIST_HEIGHT = 128;
        private static final int MAX_ALIAS_LENGTH = 255;
        private final Pattern unescapedForwardSlash = Pattern.compile("(?<!\\\\)/");
        private final Pattern unclosedParenthesis = Pattern.compile("(?<!\\\\)\\((?=[^)]*$)");
        private final Pattern unclosedBracket = Pattern.compile("(?<!\\\\)\\[(?![^]\\[]*])");
        private final Pattern danglingBackslash = Pattern.compile("(?<=(?<!\\\\)(\\\\\\\\){0,128})\\\\$");
        private final Pattern boundary = Pattern.compile("(?<!(?<!\\\\)(\\\\\\\\){0,128}\\[[^]]{0,255})(?<=(?<!\\\\)(\\\\\\\\){0,128})(?<=(?<!\\\\)(\\\\\\\\){0,128})[$^]|(\\\\[bB])");
        private final Pattern anyChar = Pattern.compile("\\\\[wWdDsS]|(?<!(?<!\\\\)(\\\\\\\\){0,128}\\[[^]]{0,255})(?<=(?<!\\\\)(\\\\\\\\){0,128})(?<=(?<!\\\\)(\\\\\\\\){0,128})\\.");
        private final Pattern escapedChar = Pattern.compile("(\\\\u[0-9a-fA-F]{4})|(\\\\([0-3][0-7]{2}|[0-7]{1,2}))|(\\\\[^wWdDsS])");
        private final Pattern quantifier = Pattern.compile("(?<!(?<!\\\\)(\\\\\\\\){0,128}\\[[^]]{0,255})(?<=(?<!\\\\)(\\\\\\\\){0,128})(?<=(?<!\\\\)(\\\\\\\\){0,128})(\\{\\d+,?\\d*}|[+*?])");
        private final Pattern openCharSet = Pattern.compile("(?<=(?<!\\\\)(\\\\\\\\){0,128})\\[\\^?(?=[^]]*(?<=(?<!\\\\)(\\\\\\\\){0,128})])");
        private final Pattern openCaptureGroup = Pattern.compile("(?<=(?<!\\\\)(\\\\\\\\){0,128})\\((\\?(<\\w+>|:|!|=|<!|<=))?(?=.*(?<=(?<!\\\\)(\\\\\\\\){0,128})\\))");
        private final Pattern alternation = Pattern.compile("(?<!(?<!\\\\)(\\\\\\\\){0,128}\\[[^]]{0,255})(?<=(?<!\\\\)(\\\\\\\\){0,128})\\|");
        private final Consumer<PackListIntent.CloseAliases> onClose;
        private final LinearLayout stubLayout = LinearLayout.vertical();
        private LinearLayout layout = this.stubLayout;
        private @Nullable EditableList<String> aliases;

        DelegatedLayout(Consumer<PackListIntent.CloseAliases> onClose) {
            this.onClose = onClose;
        }

        void clear() {
            this.aliases = null;
            this.layout = this.stubLayout;
        }

        void refresh(
                ActiveAction.EditingAliases editAction,
                UnaryOperator<AbstractWidget> widgetAdder,
                UnaryOperator<AbstractWidget> renderableAdder
        ) {
            this.aliases = EditableList.builder(editAction.aliases())
                    .setDimensions(LIST_WIDTH, LIST_HEIGHT)
                    .setMaxTextLength(MAX_ALIAS_LENGTH)
                    .setSaveValidator(value -> !Objects.equals(value, editAction.ctx().pack().getId()))
                    .addTextStylizer(createStylizer(this.unescapedForwardSlash, Theme.RED_700.getARGB()))
                    .addTextStylizer(createStylizer(this.unclosedParenthesis, Theme.RED_700.getARGB()))
                    .addTextStylizer(createStylizer(this.unclosedBracket, Theme.RED_700.getARGB()))
                    .addTextStylizer(createStylizer(this.danglingBackslash, Theme.RED_700.getARGB()))
                    .addTextStylizer(createStylizer(this.anyChar, Theme.ORANGE_500.getARGB()))
                    .addTextStylizer(createStylizer(this.escapedChar, Theme.MAGENTA_500.getARGB()))
                    .addTextStylizer(createStylizer(this.boundary, Theme.BROWN_500.getARGB()))
                    .addTextStylizer(createStylizer(this.quantifier, Theme.BLUE_500.getARGB()))
                    .addTextStylizer(createStylizer(this.openCharSet, Theme.YELLOW_500.getARGB()))
                    .addTextStylizer(createClosingStylizer('[', ']', Theme.YELLOW_500.getARGB()))
                    .addTextStylizer(createStylizer(this.openCaptureGroup, Theme.GREEN_500.getARGB()))
                    .addTextStylizer(createClosingStylizer('(', ')', Theme.GREEN_500.getARGB()))
                    .addTextStylizer(createStylizer(this.alternation, Theme.GREEN_500.getARGB()))
                    .addTextStylizer(createStylizer(DevConfig.Packs.getRegexPrefixPattern(), Theme.GRAY_500.getARGB()))
                    .build();
            FidgetzText<Void> name = FidgetzText.<Void>builder()
                    .setMessage(editAction.ctx().pack().getTitle())
                    .setOffsetY(1)
                    .build();
            RenderableRectWidget<Void> icon = RenderableRectWidget.<Void>builder(editAction.ctx().sprite())
                    .makeSquare()
                    .build();
            FidgetzButton<Void> closeButton = FidgetzButton.<Void>builder()
                    .makeSquare()
                    .setSprite(GuiConstants.CROSS_SPRITE)
                    .setOnPress(() -> {
                        if (this.aliases != null) {
                            this.onClose.accept(new PackListIntent.CloseAliases(editAction.target(), editAction.ctx(), this.aliases.extractItems()));
                        }
                    })
                    .build();

            final FlexLayout titleLayout = FlexLayout.horizontal(this.aliases::getWidth).spacing(GuiConstants.SPACING);
            titleLayout.addChild(renderableAdder.apply(icon));
            titleLayout.addFlexChild(renderableAdder.apply(name));
            titleLayout.addChild(widgetAdder.apply(closeButton));

            this.layout = LinearLayout.vertical().spacing(GuiConstants.SPACING);
            this.layout.addChild(titleLayout);
            this.layout.addChild(widgetAdder.apply(this.aliases));
        }

        @Override
        public void setX(int x) {
            this.layout.setX(x);
        }

        @Override
        public void setY(int y) {
            this.layout.setY(y);
        }

        @Override
        public int getX() {
            return this.layout.getX();
        }

        @Override
        public int getY() {
            return this.layout.getY();
        }

        @Override
        public int getWidth() {
            return this.layout.getWidth();
        }

        @Override
        public int getHeight() {
            return this.layout.getHeight();
        }

        @Override
        public void visitChildren(@NonNull Consumer<LayoutElement> visitor) {
            this.layout.visitChildren(visitor);
        }

        @Override
        public void arrangeElements() {
            this.layout.arrangeElements();
        }

        private static TextStylizer createStylizer(Pattern pattern, int color) {
            return new PatternStylizer(DevConfig.Packs::isRegexPrefixed, pattern, color);
        }

        private static TextStylizer createClosingStylizer(char open, char close, int color) {
            return new GroupCloseStylizer(DevConfig.Packs::isRegexPrefixed, open, close, color);
        }
    }
}
