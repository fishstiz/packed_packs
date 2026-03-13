package io.github.fishstiz.packed_packs.impl.context;

import io.github.fishstiz.packed_packs.api.Preference;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.api.gui.ContextMenuSink;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.gui.components.PreferenceToggle;
import io.github.fishstiz.packed_packs.gui.metadata.PackSelectionScreenArgs;
import io.github.fishstiz.packed_packs.gui.model.PackedPacksViewModel;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksScreen;
import io.github.fishstiz.packed_packs.impl.gui.ContextMenuWrappedWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

public record ScreenContextImpl(
        Screen previousScreen,
        PackedPacksScreen screen,
        PackedPacksViewModel viewModel,
        PackSelectionScreenArgs originalArgs,
        PackType packType
) implements ScreenContext {
    public ScreenContextImpl(
            Screen previousScreen,
            PackedPacksScreen screen,
            PackedPacksViewModel viewModel,
            PackSelectionScreenArgs originalArgs
    ) {
        this(previousScreen, screen, viewModel, originalArgs, originalArgs.packType());
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
        return Collections.unmodifiableList(this.viewModel.getAvailablePacks());
    }

    @Override
    public List<Pack> getSelectedPacks() {
        return Collections.unmodifiableList(this.viewModel.getEnabledPacks());
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
    public boolean devMode() {
        return Config.get().isDevMode();
    }

    @Override
    public void rebuild() {
        this.screen.rebuildWidgets();
    }

    @Override
    public <T extends GuiEventListener & NarratableEntry> T addWidget(T widget) {
        return this.screen.addWidget(widget);
    }

    @Override
    public <T extends Renderable> T addRenderableOnly(T renderable) {
        return this.screen.addRenderableOnly(renderable);
    }

    @Override
    public <T extends GuiEventListener & NarratableEntry & Renderable> T addRenderableWidget(T widget) {
        return this.screen.addRenderableWidget(widget);
    }

    @Override
    public void removeWidget(GuiEventListener widget) {
        this.screen.removeWidget(widget);
    }

    @Override
    public @Nullable AbstractWidget wrapWidget(Preference<Boolean> key, @Nullable Component text, @Nullable AbstractWidget widget) {
        return PreferenceToggle.wrap(key, text, widget);
    }

    @Override
    public <T extends AbstractWidget> AbstractWidget wrapWithContextMenu(T widget, BiConsumer<T, ContextMenuSink> configurator) {
        return new ContextMenuWrappedWidget<>(widget, configurator);
    }
}
