package io.github.fishstiz.packed_packs.impl.context;

import io.github.fishstiz.fidgetz.v0.utils.GuiHooks;
import io.github.fishstiz.packed_packs.api.Preference;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.api.gui.ContextMenuSink;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.config.PackConfigs;
import io.github.fishstiz.packed_packs.gui.components.PreferenceHelper;
import io.github.fishstiz.packed_packs.gui.screens.PackSelectionScreenArgs;
import io.github.fishstiz.packed_packs.gui2.Store;
import io.github.fishstiz.packed_packs.gui2.screens.PackedPacksScreen;
import io.github.fishstiz.packed_packs.gui2.actions.intents.Intent;
import io.github.fishstiz.packed_packs.impl.PackedPacksApiImpl;
import io.github.fishstiz.packed_packs.impl.gui.ContextMenuItemSpecImpl;
import net.minecraft.client.Minecraft;
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

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public record Context(
        Minecraft minecraft,
        @Nullable Screen previousScreen,
        PackedPacksScreen screen,
        Store store,
        PackConfigs configs,
        PackRepository packRepository,
        Supplier<PackSelectionScreen> originalSupplier
) implements ScreenContext {
    @Override
    public Minecraft getMinecraft() {
        return minecraft;
    }

    @Override
    public PackSelectionScreen originalScreen() {
        return originalSupplier.get();
    }

    @Override
    public PackType packType() {
        return configs.packType();
    }

    @Override
    public List<Pack> getAvailablePacks() {
        return store.getDisabledPacks();
    }

    @Override
    public List<Pack> getSelectedPacks() {
        return store.getEnabledPacks();
    }

    @Override
    public void reload() {
        this.store.refreshRepository();
    }

    @Override
    public void commit() {
        this.store.commit();
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
    public @Nullable AbstractWidget wrapWidget(Preference<Boolean> key, Component text, @Nullable AbstractWidget widget) {
        return PackedPacksApiImpl.getInstance().preferences()
                .find(key.id(), key.type())
                .map(p -> PreferenceHelper.wrap(widget, p, text))
                .orElse(null);
    }

    @Override
    public <T extends AbstractWidget> AbstractWidget wrapWithContextMenu(T widget, BiConsumer<T, ContextMenuSink> configurator) {
        GuiHooks.supplyContextMenuEntries(widget, collector -> configurator.accept(widget, itemConfigurator -> {
            ContextMenuItemSpecImpl itemSpec = new ContextMenuItemSpecImpl(false);
            itemConfigurator.accept(itemSpec);
            itemSpec.apply(collector);
        }));
        return widget;
    }

    public void dispatch(Intent intent) {
        store.dispatch(intent);
    }
}
