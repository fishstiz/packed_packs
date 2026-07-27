package io.github.fishstiz.fidgetz.transform.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.fishstiz.fidgetz.transform.interfaces.IStringWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(StringWidget.class)
public class StringWidgetMixin implements IStringWidget {
    @Shadow
    private float alignX;

    @Unique
    private Boolean fidgetz$shadow;

    @Unique
    private int fidgetz$offsetY = 0;

    @Override
    public void fidgetz$setShadow(boolean shadow) {
        this.fidgetz$shadow = shadow;
    }

    @Override
    public boolean fidgetz$hasShadow() {
        return this.fidgetz$shadow != null && this.fidgetz$shadow;
    }

    @Override
    public void fidgetz$setAlignX(float alignX) {
        this.alignX = alignX;
    }

    @Override
    public void fidgetz$setOffsetY(int offsetY) {
        this.fidgetz$offsetY = offsetY;
    }

    @WrapOperation(method = "renderWidget", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)V"
    ))
    public void drawShadow(GuiGraphics guiGraphics, Font font, FormattedCharSequence text, int x, int y, int color, Operation<Integer> original) {
        if (this.fidgetz$shadow != null) {
            guiGraphics.drawString(font, text, x, y + this.fidgetz$offsetY, color, this.fidgetz$hasShadow());
        } else {
            original.call(guiGraphics, font, text, x, y, color);
        }
    }
}
