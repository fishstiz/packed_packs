package io.github.fishstiz.fidgetz.gui.components;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.cursor.CursorType;
import io.github.fishstiz.fidgetz.gui.renderables.ColoredRect;
import io.github.fishstiz.fidgetz.gui.renderables.RenderableRect;
import io.github.fishstiz.fidgetz.gui.shapes.GuiRectangle;
import io.github.fishstiz.fidgetz.util.GuiUtil;
import io.github.fishstiz.fidgetz.util.debounce.PollingDebouncer;
import io.github.fishstiz.fidgetz.util.debounce.SimplePollingDebouncer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.TabOrderedElement;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

import static net.minecraft.client.gui.screens.Screen.findNarratableWidget;

public class ToggleableDialog<T extends LayoutElement> extends AbstractContainerEventHandler implements Renderable, NarratableEntry {
    protected final Screen screen;
    private final PollingDebouncer<Void> focusOnOpenTask = new SimplePollingDebouncer<>(this::focus, 0);
    private final List<GuiEventListener> children = new ObjectArrayList<>();
    private final List<Renderable> renderables = new ObjectArrayList<>();
    private final List<NarratableEntry> narratables = new ObjectArrayList<>();
    private final List<Consumer<Boolean>> listeners;
    private final T root;
    private final RenderableRect backdrop;
    private final RenderableRect background;
    private final boolean autoClose;
    private final GuiRectangle ignoreAutoCloseArea;
    private final boolean autoLoseFocus;
    private final boolean closeOnEscape;
    private final boolean captureClick;
    private final boolean captureFocus;
    private final boolean focusOnOpen;
    private @Nullable NarratableEntry lastNarratable;
    private GuiRectangle boundingBox;
    private boolean open = false;
    private boolean hovered;

    protected ToggleableDialog(Builder<T, ?> builder) {
        this.screen = builder.screen;
        this.root = builder.root;
        this.boundingBox = builder.boundingBox;
        this.backdrop = builder.backdrop;
        this.background = builder.background;
        this.autoClose = builder.autoClose;
        this.ignoreAutoCloseArea = builder.ignoreAutoCloseArea;
        this.focusOnOpen = builder.focusOnOpen;
        this.autoLoseFocus = builder.autoLoseFocus;
        this.closeOnEscape = builder.closeOnEscape;
        this.captureClick = builder.captureClick;
        this.captureFocus = builder.captureFocus;
        this.listeners = builder.listeners;
        this.open = builder.open;
        if (this.open && this.focusOnOpen) this.focusOnOpenTask.run();
    }

    public T root() {
        return this.root;
    }

    public void toggle() {
        this.setOpen(!this.isOpen());
    }

    public void setOpen(boolean open) {
        boolean previous = this.open;
        this.open = open;

        if (previous != this.open) {
            if (!open) {
                this.focusOnOpenTask.abort();

                if (this.screen.getFocused() == this) {
                    this.screen.setFocused(null);
                }

                ComponentPath path = this.getCurrentFocusPath();
                if (path != null) {
                    path.applyFocus(false);
                }
            } else if (this.focusOnOpen || this.captureFocus) {
                this.focusOnOpenTask.run();
            }

            for (var listener : this.listeners) {
                listener.accept(open);
            }
        }
    }

    public boolean isOpen() {
        return this.open;
    }

    public <U extends GuiEventListener> U prependWidget(U widget) {
        this.children.addFirst(Objects.requireNonNull(widget));
        if (widget instanceof NarratableEntry narratable) this.narratables.addFirst(narratable);
        return widget;
    }

    public <U extends GuiEventListener> U addWidget(U widget) {
        this.children.add(Objects.requireNonNull(widget));
        if (widget instanceof NarratableEntry narratable) this.narratables.add(narratable);
        return widget;
    }

    public <U extends Renderable> U addRenderableOnly(U renderable) {
        this.renderables.add(Objects.requireNonNull(renderable));
        return renderable;
    }

    public <U extends GuiEventListener & Renderable> U addRenderableWidget(U child) {
        this.children.add(Objects.requireNonNull(child));
        this.renderables.add(child);
        if (child instanceof NarratableEntry narratable) this.narratables.add(narratable);
        return child;
    }

    protected void clearWidgets() {
        this.children.clear();
        this.renderables.clear();
        this.narratables.clear();
    }

    @Override
    public @NonNull List<? extends GuiEventListener> children() {
        return this.isOpen() ? this.children : Collections.emptyList();
    }

    @Override
    public @NonNull Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
        for (var child : this.children) {
            if (child.isMouseOver(mouseX, mouseY)) {
                return Optional.of(child);
            }
        }
        return Optional.empty();
    }

    public Consumer<Boolean> addListener(Consumer<Boolean> listener) {
        this.listeners.add(listener);
        return listener;
    }

    public void setBoundingBox(GuiRectangle boundingBox) {
        this.boundingBox = Objects.requireNonNull(boundingBox);
    }

    public void setBoundingBox(LayoutElement boundingBox) {
        this.setBoundingBox(GuiRectangle.viewOf(boundingBox));
    }

    public GuiRectangle getBoundingBox() {
        return this.boundingBox;
    }

    public boolean shouldCloseOnEscape() {
        return this.closeOnEscape;
    }

    protected void renderBackdrop(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, float partialTick) {
        if (this.backdrop != null) {
            this.backdrop.render(guiGraphics, x, y, width, height, partialTick);
        }
    }

    protected void renderBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, float partialTick) {
        if (this.background != null) {
            this.background.render(guiGraphics, x, y, width, height, partialTick);
        }
    }

    protected void renderForeground(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public final void render(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.hovered = this.isMouseOverBounds(mouseX, mouseY);

        if (this.isOpen()) {
            this.focusOnOpenTask.poll();

            int x = this.boundingBox.getX();
            int y = this.boundingBox.getY();
            int width = this.boundingBox.getWidth();
            int height = this.boundingBox.getHeight();

            if (this.isHovered() || this.isCaptureClick() || this.isCaptureFocus()) {
                guiGraphics.requestCursor(CursorType.DEFAULT);
            }

            this.renderBackdrop(guiGraphics, 0, 0, this.screen.width, this.screen.height, mouseX, mouseY, partialTick);
            this.renderBackground(guiGraphics, x, y, width, height, mouseX, mouseY, partialTick);
            for (Renderable renderable : this.renderables) {
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            }
            this.renderForeground(guiGraphics, x, y, width, height, mouseX, mouseY, partialTick);
        }
    }

    protected boolean isValidClickButton(MouseButtonInfo mouseButtonInfo) {
        return mouseButtonInfo.button() == InputConstants.MOUSE_BUTTON_LEFT;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent keyEvent) {
        if (!this.isOpen()) {
            return false;
        }
        if (this.shouldCloseOnEscape() && keyEvent.isEscape()) {
            this.setOpen(false);
            return true;
        }
        GuiEventListener focused = this.getFocused();
        if (focused != null) {
            return focused.keyPressed(keyEvent);
        }
        return false;
    }

    public boolean isMouseOverBounds(double mouseX, double mouseY) {
        if (!this.isOpen()) {
            return false;
        }
        if (this.boundingBox.containsPoint(mouseX, mouseY)) {
            return true;
        }
        return GuiUtil.deepChildHovered(this, mouseX, mouseY);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!this.isOpen()) {
            return false;
        }
        if (this.isCaptureClick() || this.isCaptureFocus()) {
            return true;
        }
        return this.isMouseOverBounds(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
        if (!this.isOpen()) {
            return false;
        }
        Optional<GuiEventListener> hoveredChild = this.getChildAt(mouseButtonEvent.x(), mouseButtonEvent.y());
        if (hoveredChild.isPresent() && hoveredChild.get().mouseClicked(mouseButtonEvent, doubleClicked)) {
            if (hoveredChild.get().shouldTakeFocusAfterInteraction()) {
                this.setFocused(hoveredChild.get());
            }
            if (this.isValidClickButton(mouseButtonEvent.buttonInfo())) {
                this.setDragging(true);
            }
            return true;
        }
        if (this.autoLoseFocus && hoveredChild.isEmpty() && !this.children.isEmpty()) {
            this.setFocused(this.children.getFirst());
            for (var child : this.children) {
                child.setFocused(false);
            }
        }
        if (hoveredChild.isPresent() || this.isMouseOverBounds(mouseButtonEvent.x(), mouseButtonEvent.y())) {
            return true;
        }
        if (this.autoClose && this.isValidClickButton(mouseButtonEvent.buttonInfo()) &&
            (this.ignoreAutoCloseArea == null || !this.ignoreAutoCloseArea.containsPoint(mouseButtonEvent.x(), mouseButtonEvent.y()))) {
            this.setOpen(false);
        }
        return this.captureClick || this.captureFocus;
    }

    public boolean encloses(ScreenRectangle rectangle) {
        return rectangle != null && (this.boundingBox.contains(rectangle) || this.childEncloses(rectangle));
    }

    public boolean encloses(LayoutElement element) {
        return element != null && (this.boundingBox.contains(element) || this.childEncloses(element.getRectangle()));
    }

    public boolean encloses(GuiEventListener guiEventListener) {
        if (guiEventListener == null) return false;

        if (guiEventListener instanceof LayoutElement element) {
            return this.encloses(element);
        }

        ScreenRectangle rectangle = guiEventListener.getRectangle();
        return this.boundingBox.contains(rectangle) || this.childEncloses(rectangle);
    }

    private boolean childEncloses(ScreenRectangle rectangle) {
        for (GuiEventListener child : this.children) {
            switch (child) {
                case ToggleableDialog<?> dialog when dialog.encloses(rectangle) -> {
                    return true;
                }
                case LayoutElement childElement when GuiUtil.contains(childElement, rectangle) -> {
                    return true;
                }
                default -> {
                    if (GuiUtil.contains(child.getRectangle(), rectangle)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public @NonNull ScreenRectangle getRectangle() {
        return this.boundingBox.getScreenRectangle();
    }

    public void focus() {
        if (this.isOpen()) {
            ComponentPath path = this.nextFocusPath(new FocusNavigationEvent.InitialFocus());
            this.screen.clearFocus();
            this.screen.setFocused(this);
            if (path != null) {
                path.applyFocus(true);
            }
        }
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(@NonNull FocusNavigationEvent event) {
        if (!this.isOpen()) return null;

        ComponentPath next = super.nextFocusPath(event);
        if (next != null) return next;

        if (event instanceof FocusNavigationEvent.InitialFocus && !this.children.isEmpty()) {
            GuiEventListener firstFocusable = Collections.min(this.children, Comparator.comparingInt(GuiEventListener::getTabOrderGroup));
            ComponentPath path = firstFocusable.nextFocusPath(event);
            return path == null ? ComponentPath.path(firstFocusable, this) : ComponentPath.path(this, path);
        }

        return this.captureFocus ? this.getCurrentFocusPath() : null;
    }

    @Override
    public void setFocused(boolean isFocused) {
        if (!isFocused) {
            this.setFocused(null);
        }
    }

    public void repositionElements() {
    }

    @Override
    public boolean isFocused() {
        return this.isOpen() && super.isFocused();
    }

    public boolean isCaptureClick() {
        return this.captureClick;
    }

    public boolean isCaptureFocus() {
        return this.captureFocus;
    }

    @Override
    public int getTabOrderGroup() {
        return -1;
    }

    @Override
    public boolean isActive() {
        return this.isOpen();
    }

    public boolean isHovered() {
        return this.isOpen() && this.hovered;
    }

    @Override
    public @NonNull NarrationPriority narrationPriority() {
        return this.isHovered() ? NarrationPriority.HOVERED : NarrationPriority.NONE;
    }

    protected Component getUsageNarration() {
        return Component.translatable("narration.component_list.usage");
    }

    @Override
    public void updateNarration(@NonNull NarrationElementOutput narrationElementOutput) {
        List<NarratableEntry> sortedNarratables = this.narratables
                .stream()
                .filter(NarratableEntry::isActive)
                .sorted(Comparator.comparingInt(TabOrderedElement::getTabOrderGroup))
                .toList();
        Screen.NarratableSearchResult narratableSearchResult = findNarratableWidget(sortedNarratables, this.lastNarratable);
        if (narratableSearchResult != null) {
            if (narratableSearchResult.priority().isTerminal()) {
                this.lastNarratable = narratableSearchResult.entry();
            }
            if (sortedNarratables.size() > 1 && narratableSearchResult.priority() == NarrationPriority.FOCUSED) {
                narrationElementOutput.add(NarratedElementType.USAGE, this.getUsageNarration());
            }
            narratableSearchResult.entry().updateNarration(narrationElementOutput.nest());
        }
    }

    public static <T extends LayoutElement> Builder<T, ?> builder(Screen screen, T root) {
        return new Builder<>(screen, root);
    }

    public static class Builder<T extends LayoutElement, B extends Builder<T, B>> {
        protected final List<Consumer<Boolean>> listeners = new ObjectArrayList<>();
        protected final Screen screen;
        protected final T root;
        protected GuiRectangle boundingBox;
        protected RenderableRect backdrop;
        protected RenderableRect background;
        protected boolean open = false;
        protected boolean autoClose = true;
        protected GuiRectangle ignoreAutoCloseArea;
        protected boolean focusOnOpen = true;
        protected boolean autoLoseFocus = true;
        protected boolean closeOnEscape = true;
        protected boolean captureClick = false;
        protected boolean captureFocus = false;

        protected Builder(Screen screen, T root) {
            this.screen = screen;
            this.root = root;
            this.boundingBox = GuiRectangle.viewOf(this.root);
        }

        @SuppressWarnings("unchecked")
        protected B self() {
            return (B) this;
        }

        public B setBoundingBox(GuiRectangle boundingBox) {
            this.boundingBox = boundingBox;
            return self();
        }

        public B setBoundingBox(LayoutElement elementView) {
            this.boundingBox = GuiRectangle.viewOf(elementView);
            return self();
        }

        public B setBackdrop(RenderableRect backdrop) {
            this.backdrop = backdrop;
            return self();
        }

        public B setBackdrop(int color) {
            this.backdrop = new ColoredRect(color);
            return self();
        }

        public B setBackground(RenderableRect background) {
            this.background = background;
            return self();
        }

        public B setBackground(int color) {
            this.background = new ColoredRect(color);
            return self();
        }

        public B setOpen(boolean open) {
            this.open = open;
            return self();
        }

        public B addListener(Consumer<Boolean> listener) {
            this.listeners.add(listener);
            return self();
        }

        public B setAutoClose(boolean autoClose) {
            this.autoClose = autoClose;
            return self();
        }

        public B setAutoClose(GuiRectangle ignoredArea) {
            this.ignoreAutoCloseArea = ignoredArea;
            this.autoClose = true;
            return self();
        }

        public B setAutoClose(LayoutElement ignoredArea) {
            this.ignoreAutoCloseArea = GuiRectangle.viewOf(ignoredArea);
            this.autoClose = true;
            return self();
        }

        public B setCloseOnEscape(boolean closeOnEscape) {
            this.closeOnEscape = closeOnEscape;
            return self();
        }

        public B setFocusOnOpen(boolean focusOnOpen) {
            this.focusOnOpen = focusOnOpen;
            return self();
        }

        public B setAutoLoseFocus(boolean autoLoseFocus) {
            this.autoLoseFocus = autoLoseFocus;
            return self();
        }

        public B setCaptureClick(boolean captureClick) {
            this.captureClick = captureClick;
            return self();
        }

        public B setCaptureFocus(boolean captureFocus) {
            this.captureFocus = captureFocus;
            return self();
        }

        public ToggleableDialog<T> build() {
            return new ToggleableDialog<>(this);
        }
    }
}
