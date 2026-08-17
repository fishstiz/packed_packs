package io.github.fishstiz.packed_packs.gui.screens;

import io.github.fishstiz.fidgetz.v0.gui.state.FZRef;
import io.github.fishstiz.fidgetz.v0.utils.GuiHooks;
import io.github.fishstiz.packed_packs.api.Preference;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.api.gui.ContextMenuSink;
import io.github.fishstiz.packed_packs.config.PackConfigs;
import io.github.fishstiz.packed_packs.gui.UiEffect;
import io.github.fishstiz.packed_packs.gui.components.PreferenceHelper;
import io.github.fishstiz.packed_packs.gui.Store;
import io.github.fishstiz.packed_packs.gui.actions.intents.Intent;
import io.github.fishstiz.packed_packs.gui.states.PackedPacksState;
import io.github.fishstiz.packed_packs.impl.PackedPacksApiImpl;
import io.github.fishstiz.packed_packs.impl.gui.ContextMenuItemSpecImpl;
import io.github.fishstiz.packed_packs.pack.PackIconCache;
import io.github.fishstiz.packed_packs.pack.PackNodeRepository;
import io.github.fishstiz.packed_packs.pack.PackResourcesService;
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
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static io.github.fishstiz.packed_packs.util.PackUtil.mapValidDirectories;

public final class PackedPacksContext implements ScreenContext {
    private final Minecraft minecraft;
    private final @Nullable Screen previousScreen;
    private final Screen screen;
    private final PackSelectionScreenArgs args;
    private final Path packDir;
    private final PackType packType;
    private final PackConfigs configs;
    private final PackRepository packRepository;
    private final PackNodeRepository nodeRepository;
    private final PackResourcesService resources;
    private final PackIconCache iconCache;
    private final Store store;
    private @Nullable List<Path> otherDirectories = null;

    public PackedPacksContext(
            Minecraft minecraft,
            @Nullable Screen previousScreen,
            PackedPacksScreen screen,
            PackSelectionScreenArgs args
    ) {
        this.minecraft = minecraft;
        this.previousScreen = previousScreen;
        this.screen = screen;
        this.args = args;
        this.packDir = args.packDir();
        this.packType = args.packType();
        this.packRepository = args.repository();
        this.configs = PackConfigs.get(packType);
        this.nodeRepository = new PackNodeRepository(packRepository, configs.user(), packDir);
        this.resources = new PackResourcesService(nodeRepository);
        this.iconCache = new PackIconCache(minecraft, minecraft.getTextureManager());
        this.store = new Store(minecraft, configs, iconCache, resources(), nodeRepository);
    }

    public FZRef<PackedPacksState> state() {
        return store;
    }

    public void dispatch(Intent intent) {
        store.dispatch(intent);
    }

    void initializeState() {
        store.initializeState();
        this.otherDirectories = null;
    }

    Path packDir() {
        return packDir;
    }

    List<Path> otherDirectories() {
        if (this.otherDirectories == null) {
            Set<Path> additionalFolders = new LinkedHashSet<>(mapValidDirectories(configs.user().getAdditionalFolders()));
            additionalFolders.addAll(nodeRepository.otherDirectorySources());
            this.otherDirectories = List.copyOf(additionalFolders);
        }
        return this.otherDirectories;
    }

    void setEffectHandler(Consumer<UiEffect> effect) {
        store.setEffectHandler(effect);
    }

    @Override
    public Minecraft getMinecraft() {
        return minecraft;
    }

    @Override
    public Screen screen() {
        return screen;
    }

    public PackConfigs configs() {
        return configs;
    }

    @Override
    public PackRepository packRepository() {
        return packRepository;
    }

    @Override
    public PackSelectionScreen originalScreen() {
        return previousScreen instanceof PackSelectionScreen packScreen ? packScreen : args.createDummy();
    }

    @Override
    public PackType packType() {
        return packType;
    }

    @Override
    public List<Pack> getAvailablePacks() {
        return store.getDisabledPacks();
    }

    @Override
    public List<Pack> getSelectedPacks() {
        return store.getEnabledPacks();
    }

    void startWatcher() {
        List<Path> otherSources = otherDirectories();
        List<Path> directories = new ArrayList<>(otherSources.size() + 1);
        directories.add(packDir);
        directories.addAll(otherSources);
        store.startWatcher(this, directories);
    }

    void stopWatcher() {
        store.stopWatcher();
    }

    void pollWatcher() {
        store.pollWatcher();
    }

    @Override
    public void reload() {
        store.refreshRepository();
    }

    public boolean canReload() {
        return store.canRefresh();
    }

    void cancelReload() {
        store.cancelRefresh();
    }

    void undo() {
        store.undo();
    }

    void redo() {
        store.redo();
    }

    @Override
    public void commit() {
        store.savePacksToRepository();
        args.output().accept(packRepository);
    }

    public void saveSelectedProfile() {
        store.saveSelectedProfile();
    }

    void saveState() {
        store.saveState();
    }

    public PackIconCache iconCache() {
        return iconCache;
    }

    public PackResourcesService resources() {
        return resources;
    }

    @Override
    public boolean devMode() {
        return store.value().devMode();
    }

    @Override
    public void rebuild() {
        GuiHooks.rebuildWidgets(screen);
    }

    @Override
    public <T extends GuiEventListener & NarratableEntry> T addWidget(T widget) {
        return GuiHooks.addWidget(screen, widget);
    }

    @Override
    public <T extends Renderable> T addRenderableOnly(T renderable) {
        return GuiHooks.addRenderableOnly(screen, renderable);
    }

    @Override
    public <T extends GuiEventListener & NarratableEntry & Renderable> T addRenderableWidget(T widget) {
        return GuiHooks.addRenderableWidget(screen, widget);
    }

    @Override
    public void removeWidget(GuiEventListener widget) {
        GuiHooks.removeWidget(screen, widget);
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
}
