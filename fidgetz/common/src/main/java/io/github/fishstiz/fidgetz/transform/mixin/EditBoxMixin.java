package io.github.fishstiz.fidgetz.transform.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import io.github.fishstiz.fidgetz.gui.components.ToggleableEditBox;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.regex.Pattern;

import static io.github.fishstiz.fidgetz.util.DrawUtil.renderScrollingStringLeftAlign;

@Mixin(EditBox.class)
public abstract class EditBoxMixin extends AbstractWidget implements EditBoxAccess {
    @Unique
    private static final String fidgetz$SECTION_PLACEHOLDER = Pattern.quote("fidgetz¶¶¶section¶¶¶placeholder" + Math.random());

    @Unique
    private boolean fidgetz$allowPastingSectionSign = false;

    protected EditBoxMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Shadow
    protected abstract boolean isEditable();

    @Override
    public void fidgetz$allowPastingSectionSign(boolean allow) {
        this.fidgetz$allowPastingSectionSign = allow;
    }

    @WrapOperation(method = "renderWidget", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",
            ordinal = 0
    ))
    public void drawScrollingString(GuiGraphics guiGraphics, Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow, Operation<Integer> original) {
        if ((EditBox) (Object) this instanceof ToggleableEditBox<?> toggleableEditBox && !this.isEditable()) {
            renderScrollingStringLeftAlign(
                    guiGraphics,
                    font,
                    toggleableEditBox.getInactiveText(),
                    this.getX(),
                    this.getY(),
                    this.getRight(),
                    this.getBottom(),
                    color,
                    shadow
            );
            return;
        }

        original.call(guiGraphics, font, text, x, y, color, shadow);
    }

    @WrapWithCondition(method = "renderWidget", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",
            ordinal = 1
    ))
    public boolean isToggled(GuiGraphics instance, Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        return !((EditBox) (Object) this instanceof ToggleableEditBox) || this.isEditable();
    }

    @ModifyArg(method = "insertText", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/StringUtil;filterText(Ljava/lang/String;)Ljava/lang/String;"
    ))
    private String placeholderSectionSigns(String text, @Share("isReplacedRef") LocalBooleanRef isReplacedRef) {
        if (!this.fidgetz$allowPastingSectionSign || !text.contains("§")) {
            return text;
        }

        isReplacedRef.set(true);
        return text.replaceAll("§", fidgetz$SECTION_PLACEHOLDER);
    }

    @ModifyExpressionValue(method = "insertText", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/StringUtil;filterText(Ljava/lang/String;)Ljava/lang/String;"
    ))
    private String replacePlaceholders(String original, @Share("isReplacedRef") LocalBooleanRef isReplacedRef) {
        if (!isReplacedRef.get()) {
            return original;
        }

        return original.replaceAll(fidgetz$SECTION_PLACEHOLDER, "§");
    }
}
