package io.github.fishstiz.packed_packs.compat.vtdownloader;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import io.github.fishstiz.fidgetz.gui.components.Fidgetz;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuProvider;
import io.github.fishstiz.packed_packs.api.PreferenceRegistry;
import io.github.fishstiz.packed_packs.compat.ModScreenFactory;
import io.github.fishstiz.packed_packs.compat.PackWrapperDelegatorAbstractionEpicModelEntry;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.config.Preferences;
import io.github.fishstiz.packed_packs.gui.components.ToggleableHelper;
import io.github.fishstiz.packed_packs.impl.PackedPacksApiImpl;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import static io.github.fishstiz.packed_packs.compat.vtdownloader.VTDButtonFactory.VTD_SCREEN_NAME;
import static io.github.fishstiz.packed_packs.compat.vtdownloader.VTDButtonFactory.VTD_SUBTITLE;

/**
 * Copied from VTDownloader
 * <p>
 * Original work Copyright (c) 2020-2023 IotaBread
 * <p>
 * Licensed under the MIT License.
 *
 * @see <a href="https://github.com/IotaBread/VTDownloader/blob/1.21/src/main/java/me/bymartrixx/vtd/mixin/PackEntryListWidgetMixin.java">Github</a>
 */
public class VTDEditButtonWidget extends AbstractButton implements ContextMenuProvider, Fidgetz {
    private static final String VT_DESCRIPTION_MARKER = "vanillatweaks.net";
    private static final Identifier PENCIL_TEXTURE = Identifier.fromNamespaceAndPath("vt_downloader", "textures/pencil.png");
    private static final int PENCIL_TEXTURE_SIZE = 32;
    private static final int PENCIL_SIZE = 16;
    private static final int PENCIL_MARGIN_RIGHT = 1;
    private final LayoutElement container;
    private final Screen previous;
    private final PackSelectionModel.Entry pack;
    private final boolean editable;
    private final @Nullable ToggleableHelper toggleable;

    private VTDEditButtonWidget(PreferenceRegistry.Key<Boolean> prefKey, LayoutElement container, Screen previous, PackSelectionModel.Entry pack, boolean editable) {
        super(0, 0, PENCIL_SIZE, PENCIL_SIZE, CommonComponents.EMPTY);

        this.toggleable = Config.get().isDevMode()
                ? new ToggleableHelper(Preferences.INSTANCE.getOrThrow(PackedPacksApiImpl.getInstance().preferences().getSpec(prefKey)))
                : null;
        this.container = container;
        this.previous = previous;
        this.pack = pack;
        this.editable = editable;
        this.active = this.editable;
    }

    public static @Nullable VTDEditButtonWidget create(PreferenceRegistry.Key<Boolean> prefKey, Screen previous, LayoutElement container, Pack pack, boolean editable) {
        return pack.getDescription().getString().contains(VT_DESCRIPTION_MARKER)
                ? new VTDEditButtonWidget(prefKey, container, previous, new PackWrapperDelegatorAbstractionEpicModelEntry(pack), editable)
                : null;
    }

    @Override
    protected void renderContents(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.isHovered = this.isHovered && Fidgetz.super.isHovered(mouseX, mouseY);

        int pencilX = this.container.getX() + this.container.getWidth() - PENCIL_SIZE - PENCIL_MARGIN_RIGHT;
        int pencilY = this.container.getY() + this.container.getHeight() - PENCIL_SIZE;
        this.setPosition(pencilX, pencilY);

        float u = 0.0F;
        float v = 0.0F;
        if (!this.editable) {
            v = PENCIL_SIZE;
        } else if (this.isHovered()) {
            u = PENCIL_SIZE;
        }

        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                PENCIL_TEXTURE,
                pencilX, pencilY,
                u, v,
                PENCIL_SIZE, PENCIL_SIZE,
                PENCIL_TEXTURE_SIZE, PENCIL_TEXTURE_SIZE
        );

        if (this.toggleable != null) {
            this.toggleable.render(guiGraphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), partialTick);
        }

        if (this.isHovered()) {
            guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.visible && Fidgetz.super.isMouseOver(mouseX, mouseY);
    }

    @Override
    public void onPress(@NonNull InputWithModifiers inputWithModifiers) {
        if (this.editable) {
            ModScreenFactory.createScreenSetter(
                    VTD_SCREEN_NAME,
                    new ModScreenFactory.Arg<>(Screen.class, this.previous),
                    new ModScreenFactory.Arg<>(Component.class, VTD_SUBTITLE),
                    new ModScreenFactory.Arg<>(PackSelectionModel.Entry.class, this.pack)
            ).run();
        }
    }

    @Override
    protected void updateWidgetNarration(@NonNull NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }

    @Override
    public void buildItems(ContextMenuItemBuilder builder, int mouseX, int mouseY) {
        if (this.toggleable != null) {
            this.toggleable.buildContext(builder.separatorIfNonEmpty());
        }
    }
}
