package io.github.fishstiz.packed_packs.gui.components.pack;

import io.github.fishstiz.packed_packs.api.context.PackContext;
import io.github.fishstiz.packed_packs.gui.components.SelectionContext;
import io.github.fishstiz.packed_packs.gui.components.events.*;
import io.github.fishstiz.packed_packs.pack.PackAssetManager;
import io.github.fishstiz.packed_packs.pack.PackFileOperations;
import io.github.fishstiz.packed_packs.pack.PackOptionsContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.NonNull;

public class FolderPackList extends CurrentPackList {
    public FolderPackList(PackOptionsContext options, PackAssetManager assets, PackFileOperations fileOps, PackListEventListener listener) {
        super(options, assets, fileOps, listener);
    }

    @Override
    protected @NonNull Entry createEntry(PackContext context, SelectionContext<Pack> selectionContext, int index) {
        return new SubPackEntry(context, selectionContext, index);
    }

    @Override
    public boolean isTransferable(Pack pack) {
        return false;
    }

    @Override
    public boolean canInteract(PackList source) {
        return source == this;
    }

    @Override
    public boolean canDrop(DragEvent dragEvent, double mouseX, double mouseY) {
        return this.canInteract(dragEvent.target()) && super.canDrop(dragEvent, mouseX, mouseY);
    }

    @Override
    public void renderDroppableZone(GuiGraphics guiGraphics, DragEvent dragEvent, int mouseX, int mouseY, float partialTick) {
        if (dragEvent.target() == this) {
            super.renderDroppableZone(guiGraphics, dragEvent, mouseX, mouseY, partialTick);
        }
    }

    protected class SubPackEntry extends Entry {
        protected SubPackEntry(PackContext context, SelectionContext<Pack> selectionContext, int index) {
            super(context, selectionContext, index);
        }

        @Override
        public boolean isTransferable() {
            return false;
        }
    }
}
