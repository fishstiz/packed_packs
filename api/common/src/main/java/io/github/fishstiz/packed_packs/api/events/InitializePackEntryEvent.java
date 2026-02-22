package io.github.fishstiz.packed_packs.api.events;

import io.github.fishstiz.fidgetz.gui.components.AnchoredWidget;
import io.github.fishstiz.packed_packs.api.Event;
import io.github.fishstiz.packed_packs.api.context.PackContext;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.LayoutElement;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Fired when a pack entry is created.
 * <p>
 * Used to add custom widgets on top of entries.
 */
public final class InitializePackEntryEvent extends ScreenEvent implements PackEntryEvent, Event {
    /**
     * Represents the anchor positions available within a pack entry.
     */
    public enum Pos {
        START,
        CENTER,
        END;

        private float asFloat() {
            return switch (this) {
                case START -> 0.0f;
                case CENTER -> 0.5f;
                case END -> 1.0f;
            };
        }
    }

    private final @Nullable AnchoredWidget[] rows = new AnchoredWidget[3];
    private final PackContext packContext;
    private final LayoutElement container;
    private final Consumer<AbstractWidget> add;

    @ApiStatus.Internal
    public InitializePackEntryEvent(ScreenContext context, PackContext packContext, LayoutElement container, Consumer<AbstractWidget> add) {
        super(context);
        this.packContext = packContext;
        this.container = container;
        this.add = add;
    }

    /**
     * Returns the context of the pack associated with this entry.
     *
     * @return the pack context
     */
    @Override
    public PackContext packContext() {
        return this.packContext;
    }

    /**
     * Adds a widget to a row, automatically stacking it to the left of the
     * previously added widget in that same row.
     *
     * @param row         the vertical row to stack in
     * @param marginRight additional spacing to the right of this widget
     * @param widget      the widget to stack
     */
    public void addWidget(Pos row, int marginRight, AbstractWidget widget) {
        AnchoredWidget previous = this.rows[row.ordinal()];
        AnchoredWidget current = new AnchoredWidget(widget, this.container, row.asFloat(), Pos.END.asFloat(), Pos.END.asFloat(), row.asFloat());

        if (previous != null) {
            current.setOffsetX(previous.getOffsetX() - previous.getWidth() - marginRight);
        }

        this.rows[row.ordinal()] = current;
        this.add.accept(current);
    }

    /**
     * Places a widget at a specific offset relative to the right-hand side of a row.
     * Unlike {@link #addWidget}, this does not account for previously added widgets.
     *
     * @param row     the vertical row to anchor to
     * @param offsetX the horizontal offset from the right edge (positive moves left)
     * @param offsetY the vertical offset from the row anchor
     * @param widget  the widget to add
     */
    public void addAnchoredWidget(Pos row, int offsetX, int offsetY, AbstractWidget widget) {
        this.add.accept(new AnchoredWidget(widget, this.container, row.asFloat(), Pos.END.asFloat(), Pos.END.asFloat(), row.asFloat(), -offsetX, offsetY));
    }

    /**
     * Adds a widget that is manually managed.
     *
     * @param widgetFactory a function that takes the container and returns a widget
     */
    public void addDetachedWidget(Function<LayoutElement, @Nullable AbstractWidget> widgetFactory) {
        AbstractWidget widget = widgetFactory.apply(this.container);
        if (widget != null) this.add.accept(widget);
    }


    public void addTopRight(int marginRight, AbstractWidget widget) {
        this.addWidget(Pos.START, marginRight, widget);
    }

    public void addTopRight(AbstractWidget widget) {
        this.addTopRight(0, widget);
    }

    public void addCenterRight(int marginRight, AbstractWidget widget) {
        this.addWidget(Pos.CENTER, marginRight, widget);
    }

    public void addCenterRight(AbstractWidget widget) {
        this.addCenterRight(0, widget);
    }

    public void addBottomRight(int marginRight, AbstractWidget widget) {
        this.addWidget(Pos.END, marginRight, widget);
    }

    public void addBottomRight(AbstractWidget widget) {
        this.addBottomRight(0, widget);
    }

    public void anchorTopRight(int offsetX, int offsetY, AbstractWidget widget) {
        this.addAnchoredWidget(Pos.START, offsetX, offsetY, widget);
    }

    public void anchorTopRight(int offsetX, AbstractWidget widget) {
        this.anchorTopRight(offsetX, 0, widget);
    }

    public void anchorCenterRight(int offsetX, int offsetY, AbstractWidget widget) {
        this.addAnchoredWidget(Pos.CENTER, offsetX, offsetY, widget);
    }

    public void anchorCenterRight(int offsetX, AbstractWidget widget) {
        this.anchorCenterRight(offsetX, 0, widget);
    }

    public void anchorBottomRight(int offsetX, int offsetY, AbstractWidget widget) {
        this.addAnchoredWidget(Pos.END, offsetX, offsetY, widget);
    }

    public void anchorBottomRight(int offsetX, AbstractWidget widget) {
        this.anchorBottomRight(offsetX, 0, widget);
    }
}
