package io.github.fishstiz.packed_packs.impl.context;

import io.github.fishstiz.packed_packs.api.PreferenceRegistry;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.gui.components.PreferenceToggle;
import io.github.fishstiz.packed_packs.gui.metadata.PackSelectionScreenArgs;
import io.github.fishstiz.packed_packs.gui.model.PackedPacksViewModel;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record ScreenContextImpl(
        Screen previousScreen,
        Screen screen,
        PackedPacksViewModel viewModel,
        PackSelectionScreenArgs originalArgs,
        PackType packType,
        boolean devMode
) implements ScreenContext {
    public ScreenContextImpl(
            Screen previousScreen,
            Screen screen,
            PackedPacksViewModel viewModel,
            PackSelectionScreenArgs originalArgs,
            boolean devMode
    ) {
        this(previousScreen, screen, viewModel, originalArgs, originalArgs.packType(), devMode);
    }

    @Override
    public PackSelectionScreen originalScreen() {
        return this.previousScreen instanceof PackSelectionScreen packSelectionScreen
                ? packSelectionScreen
                : this.originalArgs.createDummy();
    }

    @Override
    public PackRepository packRepository() {
        return this.originalArgs.repository();
    }

    @Override
    public List<Pack> getAvailablePacks() {
        return this.viewModel.getAvailablePacks();
    }

    @Override
    public List<Pack> getSelectedPacks() {
        return this.viewModel.getEnabledPacks();
    }

    @Override
    public void reload() {
        this.viewModel.refreshRepository();
    }

    @Override
    public void commit() {
        this.viewModel.commit();
    }

    @Override
    public @Nullable AbstractWidget bindPreference(PreferenceRegistry.Key<Boolean> key, @Nullable AbstractWidget widget) {
        return PreferenceToggle.wrap(key, widget);
    }
}
