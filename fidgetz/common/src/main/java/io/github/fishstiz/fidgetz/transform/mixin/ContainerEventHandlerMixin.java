package io.github.fishstiz.fidgetz.transform.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.fishstiz.fidgetz.gui.components.ToggleableDialogContainer;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mixin(ContainerEventHandler.class)
public interface ContainerEventHandlerMixin {
    @WrapOperation(method = "handleTabNavigation", at = @At(
            value = "NEW",
            target = "(Ljava/util/Collection;)Ljava/util/ArrayList;"
    ))
    private ArrayList<GuiEventListener> filterCoveredFromPath(
            Collection<GuiEventListener> children,
            Operation<ArrayList<GuiEventListener>> original
    ) {
        if (!(this instanceof ToggleableDialogContainer dialogContainer)) {
            return original.call(children);
        }
        return this.fidgetz$filterCovered(dialogContainer, children);
    }

    @WrapOperation(method = {"nextFocusPathInDirection", "nextFocusPathVaguelyInDirection"}, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/events/ContainerEventHandler;children()Ljava/util/List;"
    ))
    private List<? extends GuiEventListener> filterCoveredFromPath(
            ContainerEventHandler instance,
            Operation<List<? extends GuiEventListener>> original
    ) {
        List<? extends GuiEventListener> children = original.call(instance);

        if (!(this instanceof ToggleableDialogContainer dialogContainer)) {
            return children;
        }

        return this.fidgetz$filterCovered(dialogContainer, children);
    }

    @Unique
    private ArrayList<GuiEventListener> fidgetz$filterCovered(
            ToggleableDialogContainer self,
            Collection<? extends GuiEventListener> children
    ) {
        ArrayList<GuiEventListener> focusable = new ArrayList<>(children.size());
        for (GuiEventListener child : children) {
            if (!self.isChildCovered(child)) {
                focusable.add(child);
            }
        }
        return focusable;
    }
}
