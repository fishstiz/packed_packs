package io.github.fishstiz.fidgetz.gui.components;

import io.github.fishstiz.fidgetz.gui.shapes.Padding;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.screens.Screen;

import static io.github.fishstiz.fidgetz.util.DrawUtil.DEMO_BACKGROUND;

public class Modal<T extends Layout> extends ToggleableDialog<LayoutWrapper<T>> {
    private static final int MIN_SIZE = 50;

    protected Modal(Builder<T> builder) {
        super(builder);

        this.root().setPadding(builder.padding);
        this.root().visitWidgets(this::addRenderableWidget);
    }

    public void repositionElements() {
        this.root().arrangeElements();
        this.root().setX(this.screen.width / 2 - this.root().getWidth() / 2);
        this.root().setY(this.screen.height / 2 - this.root().getHeight() / 2);
    }

    public void closeModal() {
        this.setOpen(false);
    }

    public static <S extends Screen & ToggleableDialogContainer, T extends Layout> Builder<T> builder(S screen, T layout) {
        return new Builder<>(screen, new LayoutWrapper<>(layout, MIN_SIZE, MIN_SIZE));
    }

    public static class Builder<T extends Layout> extends ToggleableDialog.Builder<LayoutWrapper<T>, Builder<T>> {
        protected Padding padding = Padding.empty();

        protected <S extends Screen & ToggleableDialogContainer> Builder(S screen, LayoutWrapper<T> root) {
            super(screen, root);

            this.background = DEMO_BACKGROUND;
        }

        public Builder<T> padding(Padding padding) {
            this.padding = padding;
            return this;
        }

        public Builder<T> padding(int padding) {
            return this.padding(new Padding(padding));
        }

        @Override
        public Modal<T> build() {
            return new Modal<>(this);
        }
    }
}
