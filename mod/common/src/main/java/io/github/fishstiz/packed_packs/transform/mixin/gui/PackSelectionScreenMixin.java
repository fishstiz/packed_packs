package io.github.fishstiz.packed_packs.transform.mixin.gui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.fishstiz.fidgetz.gui.components.FidgetzButton;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.gui.shapes.Size;
import io.github.fishstiz.packed_packs.gui.metadata.PackSelectionScreenArgs;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksScreen;
import io.github.fishstiz.packed_packs.gui.metadata.GridWrapper;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionScreenAccessor;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.function.Consumer;

@Mixin(PackSelectionScreen.class)
public abstract class PackSelectionScreenMixin extends Screen implements PackSelectionScreenAccessor {
    protected PackSelectionScreenMixin(Component title) {
        super(title);
    }

    @Unique
    private PackSelectionScreenArgs packed_packs$original;

    @Unique
    private FidgetzButton<GridWrapper<LinearLayout>> packed_packs$button;

    @Unique
    private Screen packed_packs$previous;

    @Override
    public void packed_packs$setPrevious(Screen previous) {
        this.packed_packs$previous = previous;
    }

    @Override
    public @Nullable Screen packed_packs$getPrevious() {
        return this.packed_packs$previous;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void setRepository(PackRepository repository, Consumer<PackRepository> output, Path packDir, Component title, CallbackInfo ci) {
        this.packed_packs$original = new PackSelectionScreenArgs(repository, output, packDir, title);
    }

    @WrapOperation(method = "init", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/layouts/LinearLayout;spacing(I)Lnet/minecraft/client/gui/layouts/LinearLayout;",
            ordinal = 1
    ))
    public LinearLayout addPackedPacksButton(LinearLayout instance, int spacing, Operation<LinearLayout> original) {
        if (this.minecraft == null) {
            return original.call(instance, spacing);
        }

        Screen previous = this.packed_packs$previous != null ? this.packed_packs$previous : this;
        this.packed_packs$button = FidgetzButton.<GridWrapper<LinearLayout>>builder()
                .makeSquare()
                .setTooltip(Tooltip.create(ResourceUtil.getModName()))
                .setSprite(new Sprite(ResourceUtil.getIcon("packed_packs"), Size.of16()))
                .setOnPress(() -> this.minecraft.setScreen(new PackedPacksScreen(this.minecraft, previous, this.packed_packs$original)))
                .setMetadata(new GridWrapper<>(original.call(instance, spacing), spacing))
                .build();

        this.addRenderableWidget(this.packed_packs$button);
        return this.packed_packs$button.getMetadata().layout();
    }

    @Inject(method = "repositionElements", at = @At("TAIL"))
    public void repositionPackedPacksButton(CallbackInfo ci) {
        if (this.packed_packs$button != null) {
            GridWrapper<LinearLayout> layoutData = this.packed_packs$button.getMetadata();
            int x = layoutData.layout().getX() - this.packed_packs$button.getWidth() - layoutData.spacing();
            this.packed_packs$button.setPosition(x, layoutData.layout().getY());
        }
    }
}
