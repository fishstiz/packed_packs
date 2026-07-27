package io.github.fishstiz.fidgetz.transform.mixin;

import io.github.fishstiz.fidgetz.transform.interfaces.FidgetzEditBox;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EditBox.class)
public interface EditBoxAccess extends FidgetzEditBox {
    @Accessor("textColor")
    int getTextColor();

    @Accessor("textColorUneditable")
    int getTextColorUneditable();

    @Invoker("isEditable")
    boolean invokeIsEditable();
}
