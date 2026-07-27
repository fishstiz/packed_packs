package io.github.fishstiz.fidgetz.gui.components;

import io.github.fishstiz.fidgetz.gui.Metadata;
import io.github.fishstiz.fidgetz.gui.renderables.RenderableRect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;

import java.util.Objects;

public class RenderableRectWidget<E> extends AbstractWidget implements Metadata<E>, Fidgetz {
    protected RenderableRect renderableRect;
    protected E metadata;

    protected RenderableRectWidget(Builder<E> builder) {
        super(builder.x, builder.y, builder.width, builder.height, CommonComponents.EMPTY);
        this.renderableRect = builder.renderableRect;
        this.metadata = builder.metadata;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.isHovered = this.isHovered && Fidgetz.super.isHovered(mouseX, mouseY);
        this.renderableRect.render(guiGraphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), partialTick);
    }

    public void setRenderableRect(RenderableRect renderableRect) {
        this.renderableRect = Objects.requireNonNull(renderableRect, "RenderableRect");
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.active && Fidgetz.super.isMouseOver(mouseX, mouseY);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    @Override
    public void playDownSound(SoundManager handler) {
    }

    @Override
    public E getMetadata() {
        return this.metadata;
    }

    @Override
    public void setMetadata(E metadata) {
        this.metadata = metadata;
    }

    public static <E> Builder<E> builder(RenderableRect renderableRect) {
        return new Builder<>(renderableRect);
    }

    public static class Builder<E> extends AbstractWidgetBuilder<Builder<E>> {
        protected final RenderableRect renderableRect;
        protected E metadata;

        protected Builder(RenderableRect renderableRect) {
            this.renderableRect = renderableRect;
        }

        public Builder<E> setMetadata(E metadata) {
            this.metadata = metadata;
            return this.self();
        }

        public RenderableRectWidget<E> build() {
            return new RenderableRectWidget<>(this);
        }
    }
}
