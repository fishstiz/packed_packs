package io.github.fishstiz.fidgetz.gui.layouts;

import net.minecraft.client.gui.layouts.LayoutElement;

public interface FlexLayoutElement extends LayoutElement {
    void setWidth(int width);

    void setHeight(int height);

    default void setSize(int size) {
        this.setWidth(size);
        this.setHeight(size);
    }
}
