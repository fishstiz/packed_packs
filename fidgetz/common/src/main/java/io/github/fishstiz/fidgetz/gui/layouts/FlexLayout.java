package io.github.fishstiz.fidgetz.gui.layouts;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.*;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

public class FlexLayout implements Layout {
    private final List<Child<? extends LayoutElement>> children = new ObjectArrayList<>();
    private final LinearLayout wrappedLayout;
    private final LinearLayout.Orientation orientation;
    private final @Nullable IntSupplier maxSizeAtOrientation;
    private int minWidth;
    private int minHeight;
    private int spacing;

    private FlexLayout(LinearLayout.Orientation orientation, @Nullable IntSupplier maxSizeAtOrientation, int minWidth, int minHeight, int spacing) {
        this.wrappedLayout = new LinearLayout(0, 0, orientation).spacing(spacing);
        this.orientation = orientation;
        this.maxSizeAtOrientation = maxSizeAtOrientation;
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        this.spacing = spacing;
    }

    private FlexLayout(LinearLayout.Orientation orientation, @Nullable IntSupplier maxSizeAtOrientation) {
        this(orientation, maxSizeAtOrientation, 0, 0, 0);
    }

    private void addChild(Child<? extends LayoutElement> child) {
        this.children.add(child);
        switch (this.orientation) {
            case VERTICAL -> this.minWidth = Math.max(this.minWidth, child.getWidth());
            case HORIZONTAL -> this.minHeight = Math.max(this.minHeight, child.getHeight());
        }
    }

    public <T extends LayoutElement> T addChild(T child, LayoutSettings layoutSettings) {
        this.addChild(new Child<>(child, layoutSettings));
        return this.wrappedLayout.addChild(child, layoutSettings);
    }

    public <T extends LayoutElement> T addChild(T child) {
        return this.addChild(child, this.wrappedLayout.newCellSettings());
    }

    public <T extends AbstractWidget> T addFlexChild(T child, boolean crossAxis, LayoutSettings layoutSettings) {
        this.addChild(new FlexWidget(child, crossAxis, layoutSettings));
        return this.wrappedLayout.addChild(child, layoutSettings);
    }

    public <T extends AbstractWidget> T addFlexChild(T child, boolean crossAxis) {
        return this.addFlexChild(child, crossAxis, this.wrappedLayout.newCellSettings());
    }

    public <T extends AbstractWidget> T addFlexChild(T child) {
        return this.addFlexChild(child, false, this.wrappedLayout.newCellSettings());
    }

    public <T extends FlexLayout> T addFlexChild(T child, boolean crossAxis, LayoutSettings layoutSettings) {
        this.addChild(new NestedFlexLayout(child, crossAxis, layoutSettings));
        return this.wrappedLayout.addChild(child, layoutSettings);
    }

    public <T extends FlexLayout> T addFlexChild(T child, boolean crossAxis) {
        return this.addFlexChild(child, crossAxis, this.wrappedLayout.newCellSettings());
    }

    public <T extends FlexLayout> T addFlexChild(T child) {
        return this.addFlexChild(child, false, this.wrappedLayout.newCellSettings());
    }

    public void addSpacer() {
        this.addChild(new Spacer(this.wrappedLayout.addChild(new FlexSpacerElement())));
    }

    private int getMinSizeAtOrientation() {
        return switch (this.orientation) {
            case HORIZONTAL -> this.minWidth;
            case VERTICAL -> this.minHeight;
        };
    }

    private int getPaddingAtOrientation(Child<?> child) {
        LayoutSettings.LayoutSettingsImpl layoutSettings = child.layoutSettings.getExposed();
        return switch (this.orientation) {
            case HORIZONTAL -> layoutSettings.paddingLeft + layoutSettings.paddingRight;
            case VERTICAL -> layoutSettings.paddingTop + layoutSettings.paddingBottom;
        };
    }

    private int getFlexDistribution() {
        int padding = 0;
        int totalSize = 0;
        int flexCount = 0;

        for (int i = 0; i < this.children.size(); i++) {
            Child<?> child = this.children.get(i);

            if (child.isFlexible()) {
                flexCount++;
            } else {
                totalSize += child.getSizeAtOrientation(this.orientation);
            }

            if (i < this.children.size() - 1) {
                totalSize += spacing;
            }

            padding += this.getPaddingAtOrientation(child);
        }

        int max = this.maxSizeAtOrientation != null ? this.maxSizeAtOrientation.getAsInt() : this.getMinSizeAtOrientation();
        return flexCount > 0 ? (max - padding - totalSize) / flexCount : 0;
    }

    @Override
    public void arrangeElements() {
        int distribution = this.getFlexDistribution();

        for (Child<?> child : this.children) {
            child.setSize(this.orientation, distribution, this.minWidth, this.minHeight);
        }

        this.wrappedLayout.arrangeElements();
    }

    @Override
    public void visitChildren(Consumer<LayoutElement> visitor) {
        this.wrappedLayout.visitChildren(visitor);
    }

    @Override
    public void setX(int x) {
        this.wrappedLayout.setX(x);
    }

    @Override
    public void setY(int y) {
        this.wrappedLayout.setY(y);
    }

    @Override
    public int getX() {
        return this.wrappedLayout.getX();
    }

    @Override
    public int getY() {
        return this.wrappedLayout.getY();
    }

    @Override
    public int getWidth() {
        return Math.max(this.minWidth, this.wrappedLayout.getWidth());
    }

    @Override
    public int getHeight() {
        return Math.max(this.minHeight, this.wrappedLayout.getHeight());
    }

    public FlexLayout spacing(int spacing) {
        this.wrappedLayout.spacing(spacing);
        this.spacing = spacing;
        return this;
    }

    public FlexLayout copyLayout() {
        return new FlexLayout(this.orientation, this.maxSizeAtOrientation, this.minWidth, this.minHeight, this.spacing);
    }

    public static FlexLayout horizontal(IntSupplier maxWidth) {
        return new FlexLayout(LinearLayout.Orientation.HORIZONTAL, maxWidth);
    }

    public static FlexLayout horizontal() {
        return new FlexLayout(LinearLayout.Orientation.HORIZONTAL, null);
    }

    public static FlexLayout vertical(IntSupplier maxHeight) {
        return new FlexLayout(LinearLayout.Orientation.VERTICAL, maxHeight);
    }

    public static FlexLayout vertical() {
        return new FlexLayout(LinearLayout.Orientation.VERTICAL, null);
    }

    private static class Child<T extends LayoutElement> {
        protected final T element;
        protected final LayoutSettings layoutSettings;

        protected Child(T element, LayoutSettings layoutSettings) {
            this.element = element;
            this.layoutSettings = layoutSettings;
        }

        protected int getSizeAtOrientation(LinearLayout.Orientation orientation) {
            return orientation == LinearLayout.Orientation.HORIZONTAL ? this.getWidth() : this.getHeight();
        }

        protected int getWidth() {
            return this.element.getWidth();
        }

        protected int getHeight() {
            return this.element.getHeight();
        }

        protected void setSize(LinearLayout.Orientation orientation, int distribution, int width, int height) {
        }

        protected boolean isFlexible() {
            return false;
        }
    }

    private abstract static class FlexChild<T extends LayoutElement> extends Child<T> {
        protected final boolean crossAxis;

        protected FlexChild(T element, boolean crossAxis, LayoutSettings layoutSettings) {
            super(element, layoutSettings);

            this.crossAxis = crossAxis;
        }

        protected abstract void setWidth(int width);

        protected abstract void setHeight(int height);

        @Override
        protected boolean isFlexible() {
            return true;
        }

        @Override
        protected void setSize(LinearLayout.Orientation orientation, int distribution, int width, int height) {
            switch (orientation) {
                case HORIZONTAL -> {
                    this.setWidth(distribution);
                    if (this.crossAxis && height > 0) {
                        this.setHeight(height);
                    }
                }
                case VERTICAL -> {
                    this.setHeight(distribution);
                    if (this.crossAxis && width > 0) {
                        this.setWidth(width);
                    }
                }
            }
        }
    }

    private static class Spacer extends FlexChild<FlexSpacerElement> {
        Spacer(FlexSpacerElement element) {
            super(element, false, LayoutSettings.defaults());
        }

        @Override
        protected int getSizeAtOrientation(LinearLayout.Orientation orientation) {
            return 0;
        }

        @Override
        protected int getWidth() {
            return 0;
        }

        @Override
        protected int getHeight() {
            return 0;
        }

        @Override
        protected void setWidth(int width) {
            this.element.setWidth(width);
        }

        @Override
        protected void setHeight(int height) {
            this.element.setHeight(height);
        }
    }

    private static class FlexWidget extends FlexChild<AbstractWidget> {
        private FlexWidget(AbstractWidget element, boolean crossAxis, LayoutSettings layoutSettings) {
            super(element, crossAxis, layoutSettings);
        }

        @Override
        protected void setWidth(int width) {
            this.element.setWidth(width);
        }

        @Override
        protected void setHeight(int height) {
            this.element.setHeight(height);
        }
    }

    private static class NestedFlexLayout extends FlexChild<FlexLayout> {
        private NestedFlexLayout(FlexLayout element, boolean crossAxis, LayoutSettings layoutSettings) {
            super(element, crossAxis, layoutSettings);
        }

        @Override
        protected void setWidth(int width) {
            this.element.minWidth = width;
        }

        @Override
        protected void setHeight(int height) {
            this.element.minHeight = height;
        }
    }
}
