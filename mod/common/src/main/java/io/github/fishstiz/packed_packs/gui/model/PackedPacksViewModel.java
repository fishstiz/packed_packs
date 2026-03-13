package io.github.fishstiz.packed_packs.gui.model;

import io.github.fishstiz.fidgetz.util.GuiUtil;
import io.github.fishstiz.fidgetz.util.lang.CollectionsUtil;
import io.github.fishstiz.fidgetz.util.lang.FunctionsUtil;
import io.github.fishstiz.fidgetz.util.lang.ObjectsUtil;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.config.*;
import io.github.fishstiz.packed_packs.gui.HistoryManager;
import io.github.fishstiz.packed_packs.gui.Intent;
import io.github.fishstiz.packed_packs.gui.UiEffect;
import io.github.fishstiz.packed_packs.gui.components.pack.Query;
import io.github.fishstiz.packed_packs.gui.intents.PackListIntent;
import io.github.fishstiz.packed_packs.gui.intents.ProfileIntent;
import io.github.fishstiz.packed_packs.gui.intents.Status;
import io.github.fishstiz.packed_packs.gui.metadata.PackSelectionScreenArgs;
import io.github.fishstiz.packed_packs.gui.reducers.PackedPacksReducer;
import io.github.fishstiz.packed_packs.gui.screens.InitMode;
import io.github.fishstiz.packed_packs.gui.states.*;
import io.github.fishstiz.packed_packs.impl.context.PackEntryContext;
import io.github.fishstiz.packed_packs.pack.*;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import io.github.fishstiz.packed_packs.util.AsyncUtil;
import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.packed_packs.util.ToastUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.util.Util;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.github.fishstiz.packed_packs.util.PackUtil.*;

public class PackedPacksViewModel {
    private final Executor mainThreadExecutor;
    private final Consumer<PackRepository> reload;
    private final PackOptionsContext options;
    private final PackConfigs configs;
    private final PackAssetManager assetManager;
    private final PackRepositoryManager repository;
    private final PackFileOperations fileOps;
    private final HistoryManager<PackedPacksState> history;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<UiEffect>> effectListeners = new CopyOnWriteArrayList<>();
    private final PackedPacksReducer reducer;
    private PackedPacksState state = PackedPacksState.empty();
    private CompletableFuture<PackedPacksState> initialStateFuture;
    private @Nullable CompletableFuture<Void> refreshFuture;
    private CompletableFuture<Void> watcherFuture;
    private PackWatcher watcher;
    private List<Path> additionalFolders;

    public PackedPacksViewModel(Minecraft minecraft, PackSelectionScreenArgs args, InitMode initMode) {
        this.mainThreadExecutor = minecraft;
        this.configs = PackConfigs.get(args.packType());
        this.repository = new PackRepositoryManager(args.repository(), args.packDir());
        this.initialStateFuture = CompletableFuture.supplyAsync(() -> buildInitialState(initMode, this.repository, this.configs), Util.backgroundExecutor());
        this.options = new PackOptionsContext(() -> this.state.profiles().selectedProfile(), () -> this.state.profiles().defaultProfile());
        this.fileOps = new PackFileOperations(this.options, this.repository);
        this.history = new HistoryManager<>(this.state);
        this.reducer = new PackedPacksReducer();
        this.assetManager = new PackAssetManager(minecraft, minecraft.getTextureManager());
        this.reload = args.output();
        this.additionalFolders = Collections.emptyList();
    }

    private static PackedPacksState buildInitialState(InitMode initMode, PackRepositoryManager repository, PackConfigs configs) {
        Profile defaultProfile = configs.profiles().getDefault();
        ProfilesState profileState = new ProfilesState(configs.profiles().getProfiles(), null, defaultProfile);
        PackListState availableState = PackListState.empty()
                .withQuery(new Query(Config.get().isHideIncompatible(), Config.get().getSort(), null, null), profileState.options());

        return switch (initMode) {
            case InitMode.WithPacks(PackGroup packs) -> {
                profileState = profileState.withSelected(null);
                yield new PackedPacksState(availableState.withPacks(packs.unselected(), profileState.options()), new PackListState(packs.selected()), profileState);
            }
            case InitMode.WithProfile(Profile profile) -> {
                profileState = profileState.withSelected(profile);
                PackGroup packs = repository.validatePacks(availableState.packs(), repository.getPacksByFlattenedIds(profile.getPackIds()), profileState.options());
                yield new PackedPacksState(availableState.withPacks(packs.unselected(), profileState.options()), new PackListState(packs.selected()), profileState);
            }
            default -> {
                PackGroup packs = null;
                if (configs.user().isLastViewedProfileRemembered()) {
                    Profile lastViewed = configs.profiles().getLastViewed();
                    if (lastViewed != null) {
                        profileState = profileState.withSelected(lastViewed);
                        packs = repository.validatePacks(availableState.packs(), repository.getPacksByFlattenedIds(lastViewed.getPackIds()), profileState.options());
                    }
                }
                if (packs == null) {
                    packs = repository.getPacksBySelected(profileState.options());
                }
                PackListState newAvailable = availableState.withPacks(packs.unselected(), profileState.options());
                yield new PackedPacksState(newAvailable, new PackListState(packs.selected()), profileState);
            }
        };
    }

    public PackedPacksState state() {
        return this.state;
    }

    public Runnable subscribe(Runnable listener) {
        this.listeners.add(listener);
        return () -> this.listeners.remove(listener);
    }

    public <T> Runnable subscribe(Function<PackedPacksState, T> selector, Consumer<T> callback) {
        MutableObject<T> last = new MutableObject<>(selector.apply(this.state));
        Runnable listener = () -> {
            T next = selector.apply(this.state);
            if (next != last.get()) {
                callback.accept(next);
                last.setValue(next);
            }
        };
        this.listeners.add(listener);
        return () -> this.listeners.remove(listener);
    }

    public Runnable addEffectListener(Consumer<UiEffect> listener) {
        this.effectListeners.add(listener);
        return () -> this.effectListeners.remove(listener);
    }

    private void emitEffect(UiEffect effect) {
        this.effectListeners.forEach(listener -> listener.accept(effect));
    }

    private void replaceState(PackedPacksState state) {
        PackedPacksState prev = this.state;
        this.state = state;
        if (this.state != prev) {
            this.listeners.forEach(Runnable::run);
        }
    }

    private Intent applyIntent(Intent intent) {
        switch (intent) {
            case PackListIntent.Require require -> {
                Profile selectedProfile = this.state.profiles().selectedProfile();
                if (selectedProfile != null) {
                    selectedProfile.setRequired(require.required(), require.payload());
                }
            }
            case PackListIntent.Hide hide -> {
                Profile selectedProfile = this.state.profiles().selectedProfile();
                if (selectedProfile != null) {
                    selectedProfile.setHidden(hide.hidden(), hide.payload());
                }
            }
            case PackListIntent.FixPosition reposition -> {
                Profile selectedProfile = this.state.profiles().selectedProfile();
                if (selectedProfile != null) {
                    selectedProfile.setPosition(reposition.position(), reposition.payload());
                }
            }
            case PackListIntent.RemoveOverrides remove -> {
                Profile selectedProfile = this.state.profiles().selectedProfile();
                if (selectedProfile != null) {
                    selectedProfile.setHidden(false, remove.payload());
                    selectedProfile.setRequired(null, remove.payload());
                    selectedProfile.setPosition(null, remove.payload());
                }
            }
            case PackListIntent.Rename rename when rename.status().loading() -> {
                ObjectsUtil.ifPresent(this.watcher, PackWatcher::pause);
                try {
                    if (this.fileOps.renamePack(rename.ctx().pack(), rename.newName())) {
                        ObjectsUtil.ifPresent(this.watcher, PackWatcher::consumeChanges);
                        return rename.withSuccess();
                    } else {
                        ToastUtil.onRenameFailToast(rename.ctx().pack().getTitle(), rename.newName());
                        return rename.withFail();
                    }
                } finally {
                    ObjectsUtil.ifPresent(this.watcher, PackWatcher::resume);
                }
            }
            case PackListIntent.Delete delete when delete.status().loading() -> {
                ObjectsUtil.ifPresent(this.watcher, PackWatcher::pause);
                try {
                    if (this.fileOps.deletePack(delete.ctx().pack())) {
                        ObjectsUtil.ifPresent(this.watcher, PackWatcher::consumeChanges);
                        return delete.withSuccess();
                    } else {
                        ToastUtil.onDeleteFailToast(delete.ctx().pack().getTitle());
                        return delete.withFail();
                    }
                } finally {
                    ObjectsUtil.ifPresent(this.watcher, PackWatcher::resume);
                }
            }
            case ProfileIntent.Select selected when selected.profile() == null -> {
                return new ProfileIntent.SelectNone(this.repository.getPacksBySelected(state.profiles().options()));
            }
            case ProfileIntent.Add add -> {
                Profile selectedProfile = this.state.profiles().selectedProfile();
                if (selectedProfile != null) {
                    selectedProfile.setPacks(this.state.enabled().packs());
                    this.configs.profiles().save(selectedProfile);
                }
                add.profile().setPacks(this.state.enabled().packs());
            }
            case ProfileIntent.Rename rename -> this.configs.profiles().rename(rename.profile(), rename.name());
            case ProfileIntent.Delete delete when delete.status().loading() -> {
                return this.configs.profiles().delete(delete.profile())
                        ? delete.withSuccess(this.repository.getPacksBySelected(this.state.profiles().options()))
                        : delete.withFail();
            }
            case ProfileIntent.SetDefault(Profile profile) -> this.configs.profiles().setDefault(profile);
            case ProfileIntent.ToggleLock(Profile profile) -> profile.setLocked(!profile.isLocked());
            case PackListIntent.CloseAliases aliases ->
                    this.configs.dev().setAliases(aliases.ctx().pack().getId(), aliases.aliases());
            default -> {
            }
        }
        return intent;
    }

    public void dispatch(Intent intent) {
        Intent result = this.applyIntent(intent);
        PackedPacksState prev = this.state;
        PackedPacksState current = this.reducer.reduce(prev, result);
        this.state = current;

        if (prev != current) {
            if (result.resetHistory()) {
                this.history.reset(current);
            } else if (result.pushState() && this.isUnlocked()) {
                this.history.push(current);
            }
            this.listeners.forEach(Runnable::run);
            this.handleEffects(prev, current, result);
        }
    }

    private void handleEffects(PackedPacksState prev, PackedPacksState current, Intent intent) {
        if (prev.profiles() != current.profiles()) {
            current.available().packs().forEach(this.options::validate);
            current.enabled().packs().forEach(this.options::validate);
        }

        if (prev.profiles().selectedProfile() != current.profiles().selectedProfile()) {
            Profile previousProfile = prev.profiles().selectedProfile();
            if (previousProfile != null) {
                previousProfile.setPacks(prev.enabled().packs());
                this.configs.profiles().save(previousProfile);
            }
        }

        if (intent instanceof PackListIntent.Rename(
                PackListKey target, PackEntryContext ctx, String newName, Status status
        ) && status.success()) {
            this.refreshRepository(true);
            this.emitEffect(new UiEffect.Focus(target.type(), PackUtil.getNewIdOnRename(ctx.pack(), newName), true));
            return;
        }

        this.emitUiEffects(prev, current, intent);
    }

    private void emitUiEffects(PackedPacksState prev, PackedPacksState current, Intent intent) {
        if (intent instanceof PackListIntent.Reset || !Objects.equals(prev.profiles().selectedProfile(), current.profiles().selectedProfile())) {
            this.emitEffect(new UiEffect.ScrollToTop(PackListType.AVAILABLE));
            this.emitEffect(new UiEffect.ScrollToTop(PackListType.ENABLED));
            return;
        }

        if (intent instanceof PackListIntent packListIntent) {
            if (this.emitNavigationEffects(prev, current, packListIntent)) return;
            if (intent instanceof PackListIntent.Entry entryIntent && this.emitMoveEffects(prev, current, entryIntent)) return;
            this.emitTransferEffects(prev, current, packListIntent);
        }
    }

    private boolean emitNavigationEffects(PackedPacksState prev, PackedPacksState current, PackListIntent intent) {
        return switch (intent) {
            case PackListIntent.OpenFolder ignored -> {
                this.emitEffect(new UiEffect.FocusList(intent.target().type()));
                yield true;
            }
            case PackListIntent.CloseFolder(PackListKey target) -> {
                PackListState listState = prev.targetList(target);
                if (listState == null || listState.folder() == null) yield false;
                this.emitEffect(new UiEffect.Focus(target.type(), listState.folder().pack().getId()));
                yield true;
            }
            case PackListIntent.Entry entry when entry instanceof PackListIntent.CloseAliases || entry instanceof PackListIntent.CloseRename -> {
                this.emitEffect(new UiEffect.Focus(entry.target().type(), entry.ctx().pack().getId()));
                yield true;
            }
            default -> false;
        };
    }

    private boolean emitMoveEffects(PackedPacksState prev, PackedPacksState current, PackListIntent.Entry intent) {
        PackListState prevList = prev.targetList(intent.target());
        PackListState currentList = current.targetList(intent.target());
        if (prevList != null && currentList != null && prevList.visiblePacks() != currentList.visiblePacks()) {
            if (intent instanceof PackListIntent.Drop drop && drop.destination() != null) {
                this.emitEffect(new UiEffect.Focus(drop.destination().type()));
                return true;
            }
            if (intent instanceof PackListIntent.MoveUp || intent instanceof PackListIntent.MoveDown) {
                this.emitEffect(new UiEffect.Focus(PackListType.ENABLED, intent.ctx().pack().getId(), true));
                GuiUtil.playClickSound();
                return true;
            }
        }
        return false;
    }

    private void emitTransferEffects(PackedPacksState prev, PackedPacksState current, PackListIntent intent) {
        if (prev.enabled().visiblePacks() == current.enabled().visiblePacks()) return;
        switch (intent) {
            case PackListIntent.Drag ignored ->
                    this.emitEffect(new UiEffect.ScrollToLastSelected(PackListType.ENABLED));
            case PackListIntent.Drop drop -> this.emitEffect(new UiEffect.Focus(drop.target().type()));
            case PackListIntent.Enable ignored -> {
                this.emitEffect(new UiEffect.Focus(PackListType.ENABLED, true));
                GuiUtil.playClickSound();
            }
            case PackListIntent.Disable ignored -> {
                this.emitEffect(new UiEffect.Focus(PackListType.AVAILABLE));
                GuiUtil.playClickSound();
            }
            default -> {
                if (intent.target().type().available()) {
                    this.emitEffect(new UiEffect.ScrollToLastSelected(PackListType.ENABLED));
                }
            }
        }
    }

    public ProfilesViewModel createProfilesSlice() {
        ProfilesViewModel profilesSlice = new ProfilesViewModel(this.configs.profiles(), this::dispatch, this::state);
        this.subscribe(profilesSlice::onStateChanged);
        return profilesSlice;
    }

    public PackListViewModel createPackListSlice(PackListType type) {
        PackListContext ctx = new PackListContext(this.configs, this.options, this::saveFolder, this.fileOps::isOperable, this.assetManager::getIcon);
        PackListViewModel slice = switch (type) {
            case AVAILABLE -> PackListViewModel.available(ctx, () -> this.state.available(), this::dispatch);
            case ENABLED -> PackListViewModel.enabled(ctx, () -> this.state.enabled(), this::dispatch);
        };
        this.subscribe(slice::onStateChanged);
        return slice;
    }

    private void saveFolder(FolderPack folder, List<Pack> contents) {
        FolderPackMeta meta = this.repository.getFolderConfig(folder);
        if (meta.trySetPacks(this.repository.validateAndOrderNestedPacks(folder, contents))) {
            ObjectsUtil.ifPresent(this.watcher, PackWatcher::pause);
            folder.saveConfig(meta);
            ObjectsUtil.ifPresent(this.watcher, PackWatcher::consumeChanges);
            ObjectsUtil.ifPresent(this.watcher, PackWatcher::resume);
        }
    }

    private PackListState.@Nullable Folder revalidateFolder(PackListState.@Nullable Folder folder) {
        if (folder == null || this.repository.getFolderConfig(folder.pack()) == null) return null;
        return new PackListState.Folder(folder.pack(), folder.contents()
                .withPacks(this.repository.getNestedPacks(folder.pack()), state.profiles().options())
                .withFolder(this.revalidateFolder(folder.contents().folder())));
    }

    public void cancelRefresh() {
        var future = this.refreshFuture;
        if (future != null && !future.isDone()) {
            this.refreshFuture.cancel(true);
        }
    }

    public void refreshRepository() {
        this.cancelRefresh();
        this.refreshFuture = CompletableFuture.runAsync(this.repository::refresh, Util.backgroundExecutor())
                .thenRunAsync(this::syncRepository, this.mainThreadExecutor);
    }

    public void refreshRepository(boolean blocking) {
        if (!blocking) {
            this.refreshRepository();
            return;
        }
        this.cancelRefresh();
        this.repository.refresh();
        this.syncRepository();
    }

    public void syncRepository() {
        PackGroup validated = this.repository.validatePacks(
                state.available().packs(),
                state.enabled().packs(),
                state.profiles().options()
        );
        PackListState newAvailable = state.available()
                .withPacks(validated.unselected(), state.profiles().options())
                .withFolder(this.revalidateFolder(state.available().folder()));
        PackListState newEnabled = state.enabled()
                .withPacks(validated.selected(), state.profiles().options())
                .withFolder(this.revalidateFolder(state.enabled().folder()));

        this.assetManager.clearIconCache();
        this.replaceState(this.state.withPackLists(newAvailable, newEnabled));
        this.history.reset(this.state);
    }

    public boolean isRefreshing() {
        return this.refreshFuture != null && !this.refreshFuture.isDone();
    }

    public boolean canRefresh() {
        return !this.isRefreshing();
    }

    private void onPopHistory(PackedPacksState popped) {
        this.replaceState(popped);
        this.emitEffect(new UiEffect.Focus(popped.lastTarget().type(), true));
        this.emitEffect(new UiEffect.ScrollToLastSelected(popped.lastTarget().type().other()));
    }

    public void undo() {
        if (this.isUnlocked()) {
            this.history.undo().ifPresent(this::onPopHistory);
        }
    }

    public void redo() {
        if (this.isUnlocked()) {
            this.history.redo().ifPresent(this::onPopHistory);
        }
    }

    public boolean isUnlocked() {
        Profile selectedProfile = this.state.profiles().selectedProfile();
        return selectedProfile == null || !selectedProfile.isLocked();
    }

    public void resetChanges() {
        this.dispatch(new PackListIntent.Reset(this.state.lastTarget(), this.repository.getPacksBySelected(this.state.profiles().options())));
    }

    public void switchDefaultProfile() {
        Profile selectedProfile = this.state.profiles().selectedProfile();
        Profile defaultProfile = this.state.profiles().defaultProfile();
        if (defaultProfile != null) {
            this.dispatch(new ProfileIntent.Select(Objects.equals(defaultProfile, selectedProfile) ? null : defaultProfile));
        }
    }

    public void saveSelectedProfile() {
        Profile selectedProfile = this.state.profiles().selectedProfile();
        if (selectedProfile != null) {
            selectedProfile.setPacks(this.state.enabled().packs());
            this.configs.profiles().save(selectedProfile);
        }
    }

    private void syncSelectedProfile() {
        Profile selectedProfile = this.state.profiles().selectedProfile();
        if (selectedProfile != null) {
            selectedProfile.syncPacks(this.repository.getPacks(), this.state.enabled().packs());
        }
    }

    public @Nullable Profile getSelectedProfile() {
        return this.state.profiles().selectedProfile();
    }

    public List<Pack> getAvailablePacks() {
        return this.state.available().packs();
    }

    public List<Pack> getEnabledPacks() {
        return this.state.enabled().packs();
    }

    public boolean isDragging() {
        return this.state.dragging() != null;
    }

    public void toggleDevMode() {
        Profile selectedProfile = this.state.profiles().selectedProfile();
        if (selectedProfile != null) selectedProfile.setPacks(this.state.enabled().packs());
        Config.get().setDevMode(!Config.get().isDevMode());
        ToastUtil.onDevModeToggleToast(Config.get().isDevMode());
    }

    private List<Path> resolveAdditionalFolders() {
        return CollectionsUtil.deduplicate(CollectionsUtil.addAll(
                mapValidDirectories(this.configs.user().getAdditionalFolders()),
                this.repository.getAdditionalDirs()
        ));
    }

    public List<Path> getAdditionalFolders() {
        return this.additionalFolders;
    }

    public Path getBaseDir() {
        return this.repository.getBaseDir();
    }

    public void openBaseDir() {
        Util.getPlatform().openPath(this.repository.getBaseDir());
    }

    public void stopWatcher() {
        if (this.watcherFuture != null) {
            this.watcherFuture.cancel(true);
        }
        if (this.watcher != null) {
            this.watcher.close();
            this.watcher = null;
        }
    }

    public void startWatcher(ScreenContext screenContext) {
        if (this.watcher == null) {
            this.watcherFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    List<Path> paths = new ObjectArrayList<>(this.additionalFolders.size() + 1);
                    paths.add(this.repository.getBaseDir());
                    paths.addAll(this.additionalFolders);
                    return new PackWatcher(screenContext, paths, this::refreshRepository);
                } catch (Exception e) {
                    PackedPacks.LOGGER.error("[packed_packs] Failed to initialize pack directory watcher.", e);
                    return null;
                }
            }, Util.backgroundExecutor()).thenAcceptAsync(watcher -> {
                if (watcher != null) {
                    this.watcher = watcher;
                } else {
                    this.stopWatcher();
                }
            }, this.mainThreadExecutor);
        }
    }

    public void pollWatcher() {
        if (this.watcher != null) {
            this.watcher.poll();
        }
    }

    public void commit() {
        this.syncSelectedProfile();
        this.repository.selectPacks(this.state.enabled().packs());
        this.reload.accept(this.repository.getRepository());
        this.replaceState(state.withPackLists(state.available(), state.enabled().withQuery(Query.empty(), state.profiles().options())));
    }

    public void onMounted() {
        if (this.initialStateFuture != null) {
            this.replaceState(this.initialStateFuture.join());
            this.initialStateFuture = null;
        }

        this.history.reset(this.state);
        this.additionalFolders = this.resolveAdditionalFolders();
    }

    public void onUnmounted() {
        if (this.initialStateFuture != null) {
            this.state = this.initialStateFuture.join();
            this.initialStateFuture = null;
        }

        Config.get().setHideIncompatible(this.state.available().query().hideIncompatible());
        Config.get().setSort(this.state.available().query().sort());

        this.syncSelectedProfile();
        this.configs.profiles().setLastViewed(this.state.profiles().selectedProfile());
        this.configs.profiles().setOrder(this.state.profiles().profiles());

        Profile selectedProfile = this.state.profiles().selectedProfile();
        Runnable profileSaver = selectedProfile != null
                ? () -> this.configs.profiles().save(selectedProfile)
                : FunctionsUtil.nop();

        AsyncUtil.submitAndWait(
                Util.backgroundExecutor(),
                profileSaver,
                Config.get()::save,
                DevConfig.get()::save,
                Preferences::save
        );
    }

    public boolean shouldCommitOnClose() {
        return !(this.configs.user() instanceof Config.ResourcePacks r) || r.isApplyOnClose();
    }

    public boolean closeFolder(PackListType type) {
        PackListKey deepest = this.state.deepestTarget(type);
        PackListKey parent = deepest.unnest();
        if (parent.depth() < 0) return false;

        this.dispatch(new PackListIntent.CloseFolder(parent));
        return true;
    }

    public boolean closeFolder() {
        PackListType targetType = this.state.lastTarget().type();
        return this.closeFolder(targetType) || this.closeFolder(targetType.other());
    }
}
