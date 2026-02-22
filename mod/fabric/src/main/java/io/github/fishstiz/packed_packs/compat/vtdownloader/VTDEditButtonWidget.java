package io.github.fishstiz.packed_packs.compat.vtdownloader;

import io.github.fishstiz.fidgetz.gui.components.Fidgetz;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuProvider;
import io.github.fishstiz.packed_packs.api.PreferenceRegistry;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.gui.components.ToggleableHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    private static final ResourceLocation PENCIL_TEXTURE = ResourceLocation.fromNamespaceAndPath("vt_downloader", "textures/pencil.png");
    private static final int PENCIL_TEXTURE_SIZE = 32;
    private static final int PENCIL_SIZE = 16;
    private final Screen previous;
    private final Pack pack;
    private final boolean editable;
    private final @NotNull ToggleableHelper toggleable;

    private VTDEditButtonWidget(PreferenceRegistry.Key<Boolean> prefKey, Screen previous, Pack pack, boolean editable) {
        super(0, 0, PENCIL_SIZE, PENCIL_SIZE, CommonComponents.EMPTY);

        this.toggleable = Config.get().isDevMode() ? new ToggleableHelper(prefKey) : null;
        this.previous = previous;
        this.pack = pack;
        this.editable = editable;
        this.active = this.editable;
    }

    public static @Nullable VTDEditButtonWidget create(PreferenceRegistry.Key<Boolean> prefKey, Screen previous, Pack pack, boolean editable) {
        return pack.getDescription().getString().contains(VT_DESCRIPTION_MARKER) ? new VTDEditButtonWidget(prefKey, previous, pack, editable) : null;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.isHovered = this.isHovered && Fidgetz.super.isHovered(mouseX, mouseY);

        int x = this.getX();
        int y = this.getY();

        float u = 0.0F;
        float v = 0.0F;
        if (!this.editable) {
            v = PENCIL_SIZE;
        } else if (this.isHovered()) {
            u = PENCIL_SIZE;
        }

        guiGraphics.blit(
                RenderType::guiTextured,
                PENCIL_TEXTURE,
                x, y,
                u, v,
                PENCIL_SIZE, PENCIL_SIZE,
                PENCIL_TEXTURE_SIZE, PENCIL_TEXTURE_SIZE
        );

        if (this.toggleable != null) {
            this.toggleable.render(guiGraphics, x, y, this.getWidth(), this.getHeight(), partialTick);
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.visible && Fidgetz.super.isMouseOver(mouseX, mouseY);
    }

    @Override
    public void onPress() {
        if (this.editable) {
            VTDIntegration.createVTDScreenSetter(this.previous, this.pack).run();
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }

    @Override
    public void buildItems(ContextMenuItemBuilder builder, int mouseX, int mouseY) {
        if (this.toggleable != null) {
            this.toggleable.buildContext(builder.separatorIfNonEmpty());
        }
    }
}
