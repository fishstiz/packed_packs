package io.github.fishstiz.packed_packs.api.events;

import io.github.fishstiz.packed_packs.api.Event;
import io.github.fishstiz.packed_packs.api.context.PackContext;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.api.gui.ElementSink;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.layouts.LayoutElement;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

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

        private float value() {
            return switch (this) {
                case START -> 0.0f;
                case CENTER -> 0.5f;
                case END -> 1.0f;
            };
        }
    }

    private final @Nullable LayoutElement[] rows = new LayoutElement[3];
    private final PackContext packContext;
    private final LayoutElement container;
    private final ElementSink sink;

    @ApiStatus.Internal
    public InitializePackEntryEvent(ScreenContext context, PackContext packContext, LayoutElement container, ElementSink sink) {
        super(context);
        this.packContext = packContext;
        this.container = container;
        this.sink = sink;
    }

    /**
     * Returns the context of the pack associated with this entry.
     *
     * @return the pack context
     */
    @Override
    public PackContext packContext() {
        return packContext;
    }

    private void stackElement(Pos row, int marginRight, LayoutElement element) {
        LayoutElement previous = rows[row.ordinal()];

        float anchorX = container.getX() + container.getWidth();
        float anchorY = container.getY() + (container.getHeight() * row.value());
        int offsetX = previous != null ? previous.getX() - element.getWidth() - marginRight : Math.round(anchorX) - element.getWidth() - marginRight;
        int y = Math.round(anchorY - (element.getHeight() * row.value()));

        element.setPosition(offsetX, y);
        rows[row.ordinal()] = element;
    }

    /**
     * Adds a widget to a row, automatically stacking it to the left of the
     * previously added widgets and renderable elements in that same row.
     *
     * @param row         the vertical row to stack in
     * @param marginRight additional spacing to the right of this widget
     * @param widget      the widget to stack
     */
    public void addWidget(Pos row, int marginRight, AbstractWidget widget) {
        stackElement(row, marginRight, widget);
        sink.acceptWidget(widget);
        sink.acceptRenderable(widget);
        sink.acceptElement(widget);
    }

    /**
     * Adds a renderable to a row, automatically stacking it to the left of the
     * previously added widgets and renderable elements in that same row.
     * <p>
     * The renderable element will not receive/consume any mouse events
     *
     * @param row         the vertical row to stack in
     * @param marginRight additional spacing to the right of this widget
     * @param renderable  the renderable element to stack
     */
    public <T extends Renderable & LayoutElement> void addRenderable(Pos row, int marginRight, T renderable) {
        stackElement(row, marginRight, renderable);
        sink.acceptRenderable(renderable);
        sink.acceptElement(renderable);
    }

    private void anchorElement(Pos row, int offsetX, int offsetY, LayoutElement element) {
        float anchorX = container.getX() + container.getWidth();
        float anchorY = container.getY() + (container.getHeight() * row.value());
        int x = Math.round(anchorX - (element.getWidth() * Pos.END.value())) - offsetX;
        int y = Math.round(anchorY - (element.getHeight() * row.value())) + offsetY;
        element.setPosition(x, y);
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
        anchorElement(row, offsetX, offsetY, widget);
        sink.acceptWidget(widget);
        sink.acceptRenderable(widget);
        sink.acceptElement(widget);
    }

    /**
     * Places a renderable element at a specific offset relative to the right-hand side of a row.
     * Unlike {@link #addWidget}, this does not account for previously added widgets and renderable elements.
     * <p>
     * The renderable element will not receive/consume any mouse events
     *
     * @param row        the vertical row to anchor to
     * @param offsetX    the horizontal offset from the right edge (positive moves left)
     * @param offsetY    the vertical offset from the row anchor
     * @param renderable the renderable to add
     */
    public <T extends Renderable & LayoutElement> void addAnchoredRenderable(Pos row, int offsetX, int offsetY, T renderable) {
        anchorElement(row, offsetX, offsetY, renderable);
        sink.acceptRenderable(renderable);
        sink.acceptElement(renderable);
    }

    /**
     * Adds a widget that is manually managed.
     *
     * @param widgetFactory a function that takes the container and returns a widget
     */
    public void addDetachedWidget(Function<LayoutElement, @Nullable AbstractWidget> widgetFactory) {
        AbstractWidget widget = widgetFactory.apply(container);
        if (widget != null) {
            sink.acceptWidget(widget);
            sink.acceptRenderable(widget);
        }
    }

    /**
     * Adds a renderable that is manually managed.
     * <p>
     * The renderable will not receive/consume any mouse events
     *
     * @param renderableFactory a function that takes the container and returns a renderable element
     */
    public void addDetachedRenderable(Function<LayoutElement, @Nullable Renderable> renderableFactory) {
        Renderable renderable = renderableFactory.apply(container);
        if (renderable != null) sink.acceptRenderable(renderable);
    }

    public void addTopRight(int marginRight, AbstractWidget widget) {
        addWidget(Pos.START, marginRight, widget);
    }

    public void addTopRight(AbstractWidget widget) {
        addTopRight(0, widget);
    }

    public void addCenterRight(int marginRight, AbstractWidget widget) {
        addWidget(Pos.CENTER, marginRight, widget);
    }

    public void addCenterRight(AbstractWidget widget) {
        addCenterRight(0, widget);
    }

    public void addBottomRight(int marginRight, AbstractWidget widget) {
        addWidget(Pos.END, marginRight, widget);
    }

    public void addBottomRight(AbstractWidget widget) {
        addBottomRight(0, widget);
    }

    public void anchorTopRight(int offsetX, int offsetY, AbstractWidget widget) {
        addAnchoredWidget(Pos.START, offsetX, offsetY, widget);
    }

    public void anchorTopRight(int offsetX, AbstractWidget widget) {
        anchorTopRight(offsetX, 0, widget);
    }

    public void anchorCenterRight(int offsetX, int offsetY, AbstractWidget widget) {
        addAnchoredWidget(Pos.CENTER, offsetX, offsetY, widget);
    }

    public void anchorCenterRight(int offsetX, AbstractWidget widget) {
        anchorCenterRight(offsetX, 0, widget);
    }

    public void anchorBottomRight(int offsetX, int offsetY, AbstractWidget widget) {
        addAnchoredWidget(Pos.END, offsetX, offsetY, widget);
    }

    public void anchorBottomRight(int offsetX, AbstractWidget widget) {
        anchorBottomRight(offsetX, 0, widget);
    }

    public <T extends Renderable & LayoutElement> void addTopRightRenderable(int marginRight, T renderable) {
        addRenderable(Pos.START, marginRight, renderable);
    }

    public <T extends Renderable & LayoutElement> void addTopRightRenderable(T renderable) {
        addTopRightRenderable(0, renderable);
    }

    public <T extends Renderable & LayoutElement> void addCenterRightRenderable(int marginRight, T renderable) {
        addRenderable(Pos.CENTER, marginRight, renderable);
    }

    public <T extends Renderable & LayoutElement> void addCenterRightRenderable(T renderable) {
        addCenterRightRenderable(0, renderable);
    }

    public <T extends Renderable & LayoutElement> void addBottomRightRenderable(int marginRight, T renderable) {
        addRenderable(Pos.END, marginRight, renderable);
    }

    public <T extends Renderable & LayoutElement> void addBottomRightRenderable(T renderable) {
        addBottomRightRenderable(0, renderable);
    }

    public <T extends Renderable & LayoutElement> void anchorTopRightRenderable(int offsetX, int offsetY, T renderable) {
        addAnchoredRenderable(Pos.START, offsetX, offsetY, renderable);
    }

    public <T extends Renderable & LayoutElement> void anchorTopRightRenderable(int offsetX, T renderable) {
        anchorTopRightRenderable(offsetX, 0, renderable);
    }

    public <T extends Renderable & LayoutElement> void anchorCenterRightRenderable(int offsetX, int offsetY, T renderable) {
        addAnchoredRenderable(Pos.CENTER, offsetX, offsetY, renderable);
    }

    public <T extends Renderable & LayoutElement> void anchorCenterRightRenderable(int offsetX, T renderable) {
        anchorCenterRightRenderable(offsetX, 0, renderable);
    }

    public <T extends Renderable & LayoutElement> void anchorBottomRightRenderable(int offsetX, int offsetY, T renderable) {
        addAnchoredRenderable(Pos.END, offsetX, offsetY, renderable);
    }

    public <T extends Renderable & LayoutElement> void anchorBottomRightRenderable(int offsetX, T renderable) {
        anchorBottomRightRenderable(offsetX, 0, renderable);
    }
}
