package io.github.fishstiz.packed_packs.compat.vtdownloader;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import io.github.fishstiz.fidgetz.gui.components.Fidgetz;
import io.github.fishstiz.packed_packs.api.Preference;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.gui.components.PreferenceToggle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.BooleanSupplier;

/**
 * Copied from VTDownloader
 * <p>
 * Original work Copyright (c) 2020-2023 IotaBread
 * <p>
 * Licensed under the MIT License.
 *
 * @see <a href="https://github.com/IotaBread/VTDownloader/blob/1.21/src/main/java/me/bymartrixx/vtd/mixin/PackEntryListWidgetMixin.java">Github</a>
 */
public class VTDEditButtonWidget extends AbstractButton implements Fidgetz {
    private static final String VT_DESCRIPTION_MARKER = "vanillatweaks.net";
    private static final Identifier PENCIL_TEXTURE = Identifier.fromNamespaceAndPath("vt_downloader", "textures/pencil.png");
    private static final int PENCIL_TEXTURE_SIZE = 32;
    private static final int PENCIL_SIZE = 16;
    private final Screen previous;
    private final Pack pack;
    private final BooleanSupplier editable;
    private final @Nullable PreferenceToggle toggle;

    private VTDEditButtonWidget(Preference<Boolean> prefKey, Screen previous, Pack pack, BooleanSupplier editable) {
        super(0, 0, PENCIL_SIZE, PENCIL_SIZE, CommonComponents.EMPTY);
        this.toggle = Config.get().isDevMode() ? PreferenceToggle.tryWithInternalName(prefKey) : null;
        this.previous = previous;
        this.pack = pack;
        this.editable = editable;
        this.active = this.editable.getAsBoolean();
    }

    public static @Nullable VTDEditButtonWidget create(Preference<Boolean> prefKey, Screen previous, Pack pack, BooleanSupplier editable) {
        return pack.getDescription().getString().contains(VT_DESCRIPTION_MARKER) ? new VTDEditButtonWidget(prefKey, previous, pack, editable) : null;
    }

    @Override
    protected void renderContents(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.active = this.editable.getAsBoolean();
        this.isHovered = this.isHovered && Fidgetz.super.isHovered(mouseX, mouseY);

        int x = this.getX();
        int y = this.getY();

        float u = 0.0F;
        float v = 0.0F;
        if (!this.active) {
            v = PENCIL_SIZE;
        } else if (this.isHovered()) {
            u = PENCIL_SIZE;
        }

        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                PENCIL_TEXTURE,
                x, y,
                u, v,
                PENCIL_SIZE, PENCIL_SIZE,
                PENCIL_TEXTURE_SIZE, PENCIL_TEXTURE_SIZE
        );

        if (this.toggle != null) {
            this.toggle.render(guiGraphics, x, y, this.getWidth(), this.getHeight(), partialTick);
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
        if (this.active) {
            VTDIntegration.createVTDScreenSetter(this.previous, this.pack).run();
        }
    }

    @Override
    protected void updateWidgetNarration(@NonNull NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}
