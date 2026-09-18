package io.github.fishstiz.packed_packs.gui.states;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import io.github.fishstiz.fidgetz.v0.utils.GuiGraphicsUtils;
import io.github.fishstiz.packed_packs.gui.components.PackListContainer;
import io.github.fishstiz.packed_packs.pack.PackResourcesService;
import io.github.fishstiz.packed_packs.util.Colors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jspecify.annotations.Nullable;

public final class DragActionRenderer {
    private static final int OFFSET_Y = 4;
    private static final int ICON_SIZE = 48;
    private static final int NUM_SIZE = 16;
    private static final int ICON_OFFSET_X = ICON_SIZE / 2;
    private static final int ICON_OFFSET_Y = ICON_SIZE - OFFSET_Y;
    private static final int NUM_OFFSET_Y = NUM_SIZE - OFFSET_Y + (ICON_SIZE - NUM_SIZE) / 2;
    private final PackResourcesService resources;
    private final Font font;
    private final PackListContainer[] lists;

    public DragActionRenderer(PackResourcesService resources, Font font, PackListContainer... lists) {
        this.resources = resources;
        this.font = font;
        this.lists = lists;
    }

    public void render(ActiveAction.@Nullable Dragging dragging, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (dragging == null) return;

        boolean validDrop = false;

        for (PackListContainer list : lists) {
            validDrop |= list.canInteractWithDragged(dragging, mouseX, mouseY);
        }

        renderDragging(dragging, graphics, mouseX, mouseY);
        graphics.requestCursor(validDrop ? CursorTypes.RESIZE_ALL : CursorTypes.NOT_ALLOWED);
    }

    private void renderDragging(ActiveAction.Dragging dragging, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        String sizeString = Integer.toString(dragging.packs().size());
        int sizeStringWidth = this.font.width(sizeString);
        int iconX = mouseX - ICON_OFFSET_X;
        int iconY = mouseY - ICON_OFFSET_Y;
        int numWidth = NUM_SIZE > sizeStringWidth ? NUM_SIZE : (sizeStringWidth + NUM_SIZE - font.lineHeight);
        int numX = mouseX - numWidth / 2;
        int numY = mouseY - NUM_OFFSET_Y;

        int iconRight = iconX + ICON_SIZE;
        int iconBottom = iconY + ICON_SIZE;

        graphics.fill(iconX, iconY, iconRight, iconBottom, Colors.GRAY_800);
        GuiGraphicsUtils.texture(graphics, resources.getIcon(dragging.srcPack()), iconX, iconY, ICON_SIZE, ICON_SIZE);
        graphics.fill(iconX, iconY, iconRight, iconBottom, Colors.alpha(Colors.BLACK, 0.5f));

        int numRight = numX + numWidth;
        int numBottom = numY + NUM_SIZE;

        graphics.fill(numX, numY, numRight, numBottom, Colors.BLUE_500);
        graphics.text(this.font, sizeString, numX + numWidth / 2 - sizeStringWidth / 2, numY + NUM_SIZE / 2 - font.lineHeight / 2, Colors.WHITE);

        graphics.outline(iconX, iconY, ICON_SIZE, ICON_SIZE, Colors.WHITE);
        graphics.outline(numX, numY, numWidth, NUM_SIZE, Colors.WHITE);
    }
}
