package io.github.fishstiz.packed_packs.transform.mixin.gui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.fishstiz.fidgetz.v0.gui.components.FZIconButton;
import io.github.fishstiz.fidgetz.v0.gui.components.WidgetElements;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.gui.screens.PackSelectionScreenArgs;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksScreen;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksScreenPreloader;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionScreenAccessor;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
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
    private FZIconButton packed_packs$button;

    @Unique
    private LinearLayout packed_packs$footerLayout;

    @Unique
    private int packed_packs$footerSpacing;

    @Unique
    private Screen packed_packs$previous;

    @Unique
    @Nullable
    private Screen packed_packs$actualScreen;

    @Override
    public void packed_packs$setPrevious(@Nullable Screen previous) {
        this.packed_packs$previous = previous;
    }

    @Override
    public void packed_packs$setActualScreen(Screen screen) {
        this.packed_packs$actualScreen = screen;
    }

    @Override
    public @Nullable Screen packed_packs$getActualScreen() {
        return this.packed_packs$actualScreen;
    }

    @Override
    public @Nullable Screen packed_packs$getPrevious() {
        return this.packed_packs$previous;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void setRepository(PackRepository repository, Consumer<PackRepository> output, Path packDir, Component title, CallbackInfo ci) {
        this.packed_packs$original = new PackSelectionScreenArgs(repository, output, packDir, title);
    }

    @WrapOperation(
            method = "init",
            slice = @Slice(from = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;addToContents(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;"
            )),
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/LinearLayout;spacing(I)Lnet/minecraft/client/gui/layouts/LinearLayout;",
                    ordinal = 0
            )
    )
    public LinearLayout addPackedPacksButton(LinearLayout instance, int spacing, Operation<LinearLayout> original) {
        Screen previous = this.packed_packs$previous != null ? this.packed_packs$previous : this;

        this.packed_packs$button = FZIconButton.builder()
                .square()
                .tooltip(Component.literal(PackedPacks.MOD_NAME))
                .icon(new WidgetElements(PackedPacks.id("icon/packed_packs"), 16, 16))
                .onPress(() -> minecraft.setScreen(new PackedPacksScreen(previous, packed_packs$original)))
                .build();

        PackedPacksScreenPreloader.attach(this, this.packed_packs$button);

        this.addRenderableWidget(this.packed_packs$button);

        this.packed_packs$footerLayout = original.call(instance, spacing);
        this.packed_packs$footerSpacing = spacing;

        return packed_packs$footerLayout;
    }

    @Inject(method = "repositionElements", at = @At("TAIL"))
    public void repositionPackedPacksButton(CallbackInfo ci) {
        if (this.packed_packs$button != null && this.packed_packs$footerLayout != null) {
            int x = packed_packs$footerLayout.getX() - this.packed_packs$button.getWidth() - this.packed_packs$footerSpacing;
            this.packed_packs$button.setPosition(x, this.packed_packs$footerLayout.getY());
        }
    }
}
