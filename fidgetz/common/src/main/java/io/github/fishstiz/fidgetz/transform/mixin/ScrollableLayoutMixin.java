package io.github.fishstiz.fidgetz.transform.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.fishstiz.fidgetz.transform.interfaces.UnpaddedScrollableLayout;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.layouts.Layout;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Mixin(ScrollableLayout.class)
public abstract class ScrollableLayoutMixin implements UnpaddedScrollableLayout {
    @Shadow
    @Final
    Layout content;

    @Shadow
    private int maxHeight;

    @Shadow
    private int minWidth;

    @Unique
    private boolean fidgetz$unpadded;

    @Override
    public void fidgetz$setUnpadded(boolean unpadded) {
        this.fidgetz$unpadded = unpadded;
    }

    @Override
    public boolean fidgetz$unpadded() {
        return this.fidgetz$unpadded;
    }

    @WrapOperation(method = "arrangeElements", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/ScrollableLayout$Container;setWidth(I)V"
    ))
    private void removePaddingOnArrange(@Coerce AbstractContainerWidget instance, int width, Operation<Void> original) {
        if (this.fidgetz$unpadded) {
            boolean scrollbarVisible = this.content.getHeight() > this.maxHeight;
            original.call(instance, Math.max(this.content.getWidth() + (scrollbarVisible ? AbstractScrollArea.SCROLLBAR_WIDTH : 0), this.minWidth));
        } else {
            original.call(instance, width);
        }
    }
}
