package io.github.fishstiz.packed_packs.compat;

import io.github.fishstiz.packed_packs.pack.PackAssetManager;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import org.jetbrains.annotations.NotNull;

public record PackWrapperDelegatorAbstractionEpicModelEntry(Pack pack) implements PackSelectionModel.Entry {
    @Override
    public @NotNull ResourceLocation getIconTexture() {
        return PackAssetManager.getDefaultLocation(pack);
    }

    @Override
    public @NotNull PackCompatibility getCompatibility() {
        return pack.getCompatibility();
    }

    @Override
    public @NotNull String getId() {
        return pack.getId();
    }

    @Override
    public @NotNull Component getTitle() {
        return pack.getTitle();
    }

    @Override
    public @NotNull Component getDescription() {
        return pack.getDescription();
    }

    @Override
    public @NotNull PackSource getPackSource() {
        return pack.getPackSource();
    }

    @Override
    public boolean isFixedPosition() {
        return pack.isFixedPosition();
    }

    @Override
    public boolean isRequired() {
        return pack.isRequired();
    }

    @Override
    public void select() {
        // no-op
    }

    @Override
    public void unselect() {
        // no-op
    }

    @Override
    public void moveUp() {
        // no-op
    }

    @Override
    public void moveDown() {
        // no-op
    }

    @Override
    public boolean isSelected() {
        return false;
    }

    @Override
    public boolean canMoveUp() {
        return false;
    }

    @Override
    public boolean canMoveDown() {
        return false;
    }
}