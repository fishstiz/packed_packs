package io.github.fishstiz.packed_packs.gui.states;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import io.github.fishstiz.fidgetz.gui.renderables.ColoredRect;
import io.github.fishstiz.fidgetz.util.DrawUtil;
import io.github.fishstiz.packed_packs.gui.components.pack.PackListContainer;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jspecify.annotations.Nullable;

public final class DragActionRenderer {
    private static final int OFFSET_Y = 4;
    private static final int ICON_SIZE = 48;
    private static final int NUM_SIZE = 16;
    private static final int ICON_OFFSET_X = ICON_SIZE / 2;
    private static final int ICON_OFFSET_Y = ICON_SIZE - OFFSET_Y;
    private static final int NUM_OFFSET_Y = NUM_SIZE - OFFSET_Y + (ICON_SIZE - NUM_SIZE) / 2;
    private final ColoredRect background = new ColoredRect(Theme.GRAY_800.getARGB());
    private final ColoredRect overlay = new ColoredRect(Theme.BLACK.withAlpha(0.5f));
    private final ColoredRect numberBackground = new ColoredRect(Theme.BLUE_500.getARGB());
    private final Font font;

    public DragActionRenderer(Font font) {
        this.font = font;
    }

    public void render(
            PackListContainer available,
            PackListContainer enabled,
            ActiveAction.@Nullable Dragging dragging,
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if (dragging == null) return;

        boolean validDrop = false;

        validDrop |= available.renderDroppableZone(dragging, guiGraphics, mouseX, mouseY, partialTick);
        validDrop |= enabled.renderDroppableZone(dragging, guiGraphics, mouseX, mouseY, partialTick);

        this.renderDragging(dragging, guiGraphics, mouseX, mouseY);

        guiGraphics.requestCursor(validDrop ? CursorTypes.RESIZE_ALL : CursorTypes.NOT_ALLOWED);
    }

    private void renderDragging(ActiveAction.@Nullable Dragging dragging, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String sizeString = Integer.toString(dragging.payload().size());
        int sizeStringWidth = this.font.width(sizeString);
        int iconX = mouseX - ICON_OFFSET_X;
        int iconY = mouseY - ICON_OFFSET_Y;
        int numWidth = NUM_SIZE > sizeStringWidth ? NUM_SIZE : (sizeStringWidth + NUM_SIZE - this.font.lineHeight);
        int numX = mouseX - numWidth / 2;
        int numY = mouseY - NUM_OFFSET_Y;

        this.background.render(guiGraphics, iconX, iconY, ICON_SIZE, ICON_SIZE);
        dragging.ctx().sprite().render(guiGraphics, iconX, iconY, ICON_SIZE, ICON_SIZE);
        this.overlay.render(guiGraphics, iconX, iconY, ICON_SIZE, ICON_SIZE);
        this.numberBackground.render(guiGraphics, numX, numY, numWidth, NUM_SIZE);
        guiGraphics.drawString(this.font, sizeString, numX + numWidth / 2 - sizeStringWidth / 2, numY + NUM_SIZE / 2 - this.font.lineHeight / 2, Theme.WHITE.getARGB());
        DrawUtil.renderOutline(guiGraphics, iconX, iconY, ICON_SIZE, ICON_SIZE, Theme.WHITE.getARGB());
        DrawUtil.renderOutline(guiGraphics, numX, numY, numWidth, NUM_SIZE, Theme.WHITE.getARGB());
    }
}
