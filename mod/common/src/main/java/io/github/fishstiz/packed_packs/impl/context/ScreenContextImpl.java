package io.github.fishstiz.packed_packs.impl.context;

import io.github.fishstiz.fidgetz.gui.components.OverlayedWidget;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuContainer;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.renderables.RenderableRect;
import io.github.fishstiz.packed_packs.api.PreferenceRegistry;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.gui.components.ToggleableHelper;
import io.github.fishstiz.packed_packs.gui.metadata.PackSelectionScreenArgs;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksScreen;
import io.github.fishstiz.packed_packs.impl.PackedPacksApiImpl;
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
        PackedPacksScreen screen,
        PackSelectionScreenArgs originalArgs,
        PackType packType,
        boolean devMode
) implements ScreenContext {
    public ScreenContextImpl(Screen previousScreen, PackedPacksScreen screen, PackSelectionScreenArgs originalArgs, boolean devMode) {
        this(previousScreen, screen, originalArgs, originalArgs.packType(), devMode);
    }

    @Override
    public PackSelectionScreen originalScreen() {
        return previousScreen instanceof PackSelectionScreen packSelectionScreen
                ? packSelectionScreen
                : originalArgs.createDummy();
    }

    @Override
    public PackRepository packRepository() {
        return this.originalArgs.repository();
    }

    @Override
    public List<Pack> getAvailablePacks() {
        return this.screen.getAvailablePacks();
    }

    @Override
    public List<Pack> getSelectedPacks() {
        return this.screen.getCurrentPacks();
    }

    @Override
    public void reload() {
        this.screen.refreshPacks();
    }

    @Override
    public void commit() {
        this.screen.commit();
    }

    @Override
    public @Nullable AbstractWidget bindPreference(PreferenceRegistry prefs, PreferenceRegistry.Key<Boolean> key, AbstractWidget widget) {
        if (this.devMode()) {
            return new PreferenceOverlayedWidget(new ToggleableHelper(key), widget);
        }
        if (Boolean.TRUE.equals(prefs.get(key))) {
            return widget;
        }
        return null;
    }

    private static final class PreferenceOverlayedWidget extends OverlayedWidget implements ContextMenuContainer {
        public PreferenceOverlayedWidget(ToggleableHelper toggleableHelper, AbstractWidget widget) {
            super(toggleableHelper, widget);
        }

        @Override
        public void buildItems(ContextMenuItemBuilder builder, int mouseX, int mouseY) {
            ((ToggleableHelper) this.overlay).buildContext(builder.separatorIfNonEmpty());
            ContextMenuContainer.super.buildItems(builder, mouseX, mouseY);
        }
    }
}
