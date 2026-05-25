package io.github.fishstiz.packed_packs.gui.model;

import io.github.fishstiz.fidgetz.v0.gui.state.FZRef;
import io.github.fishstiz.fidgetz.v0.utils.FunctionUtils;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.config.*;
import io.github.fishstiz.packed_packs.gui.HistoryManager;
import io.github.fishstiz.packed_packs.gui.Intent;
import io.github.fishstiz.packed_packs.gui.UiEffect;
import io.github.fishstiz.packed_packs.gui.intents.PackListIntent;
import io.github.fishstiz.packed_packs.gui.intents.ProfileIntent;
import io.github.fishstiz.packed_packs.gui.intents.Status;
import io.github.fishstiz.packed_packs.gui.screens.PackSelectionScreenArgs;
import io.github.fishstiz.packed_packs.gui.states.PackedPacksReducer;
import io.github.fishstiz.packed_packs.gui.states.InitMode;
import io.github.fishstiz.packed_packs.gui.states.PackListState;
import io.github.fishstiz.packed_packs.gui.states.PackedPacksState;
import io.github.fishstiz.packed_packs.gui.states.ProfilesState;
import io.github.fishstiz.packed_packs.api.context.PackContext;
import io.github.fishstiz.packed_packs.pack.*;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import io.github.fishstiz.packed_packs.util.Utils;
import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.packed_packs.util.ToastUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.sounds.SoundEvents;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.github.fishstiz.packed_packs.util.PackUtil.mapValidDirectories;

public class PackedPacksStore implements FZRef<PackedPacksState> {
    private final Minecraft minecraft;
    private final Consumer<PackRepository> reload;
    private final PackOptionsContext options;
    private final PackConfigs configs;
    private final PackIconManager iconManager;
    private final PackRepositoryManager repository;
    private final PackFileOperations fileOps;
    private final HistoryManager<PackedPacksState> history;
    private final Map<String, Runnable> subscribers = new ConcurrentHashMap<>();
    private final PackedPacksReducer reducer;
    private volatile PackedPacksState state = PackedPacksState.empty();
    private volatile CompletableFuture<PackedPacksState> initialStateFuture;
    private Consumer<UiEffect> effectListener = FunctionUtils.nopConsumer();
    private @Nullable CompletableFuture<Void> refreshFuture;
    private CompletableFuture<Void> watcherFuture;
    private PackWatcher watcher;
    private List<Path> additionalFolders = Collections.emptyList();

    public PackedPacksStore(Minecraft minecraft, PackSelectionScreenArgs args) {
        this.minecraft = minecraft;
        this.configs = PackConfigs.get(args.packType());
        this.repository = new PackRepositoryManager(args.repository(), args.packDir());
        this.initialStateFuture = CompletableFuture.supplyAsync(() -> buildInitialState(new InitMode.Default(), repository, configs), Util.backgroundExecutor());
        this.options = new PackOptionsContext(() -> this.state.profiles().selectedProfile(), () -> this.state.profiles().defaultProfile());
        this.fileOps = new PackFileOperations(options, repository);
        this.history = new HistoryManager<>(this.state);
        this.reducer = new PackedPacksReducer();
        this.iconManager = new PackIconManager(minecraft, minecraft.getTextureManager());
        this.reload = args.output();
        this.additionalFolders = resolveAdditionalFolders();
    }

    private static PackedPacksState buildInitialState(InitMode initMode, PackRepositoryManager repository, PackConfigs configs) {
        Profile defaultProfile = configs.profiles().getDefault();
        ProfilesState profileState = new ProfilesState(configs.profiles().getProfiles(), null, defaultProfile);
        PackListState availableState = PackListState.empty()
                .withQuery(new Query(Config.get().isHideIncompatible(), Config.get().getSort(), null, null), profileState.options());

        return switch (initMode) {
            case InitMode.WithPacks(PackGroup packs) -> {
                profileState = profileState.withSelected(null);
                yield new PackedPacksState(
                        availableState.withPacks(packs.unselected(), profileState.options()),
                        PackListState.empty().withPacks(packs.selected(), profileState.options()),
                        profileState
                );
            }
            case InitMode.WithProfile(Profile profile) -> {
                profileState = profileState.withSelected(profile);

                PackGroup packs = repository.validatePacks(
                        availableState.packs(),
                        repository.getPacksByFlattenedIds(profile.getPackIds()),
                        profileState.options()
                );

                yield new PackedPacksState(
                        availableState.withPacks(packs.unselected(), profileState.options()),
                        PackListState.empty().withPacks(packs.selected(), profileState.options()),
                        profileState
                );
            }
            default -> {
                PackGroup packs = null;
                if (configs.user().isLastViewedProfileRemembered()) {
                    Profile lastViewed = configs.profiles().getLastViewed();
                    if (lastViewed != null) {
                        profileState = profileState.withSelected(lastViewed);
                        packs = repository.validatePacks(
                                availableState.packs(),
                                repository.getPacksByFlattenedIds(lastViewed.getPackIds()),
                                profileState.options()
                        );
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

    @Override
    public PackedPacksState value() {
        return state;
    }

    @Override
    public <R extends @Nullable Object> Runnable subscribe(String key, Function<PackedPacksState, R> selector, Consumer<R> callback) {
        MutableObject<R> last = new MutableObject<>(selector.apply(this.state));
        Runnable listener = () -> {
            R next = selector.apply(this.state);
            if (next != last.getValue()) {
                callback.accept(next);
                last.setValue(next);
            }
        };
        subscribers.put(key, listener);
        return () -> subscribers.remove(key);
    }

    public void setEffectListener(Consumer<UiEffect> listener) {
        this.effectListener = listener;
    }

    private void emitEffect(UiEffect effect) {
        effectListener.accept(effect);
    }

    private void replaceState(PackedPacksState state) {
        PackedPacksState prev = this.state;
        this.state = state;
        if (this.state != prev) {
            subscribers.values().forEach(Runnable::run);
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
                pauseWatcher();
                try {
                    if (fileOps.renamePack(rename.ctx().pack(), rename.newName())) {
                        consumeWatchedChanges();
                        return rename.withSuccess();
                    } else {
                        ToastUtil.onRenameFailToast(rename.ctx().pack().getTitle(), rename.newName());
                        return rename.withFail();
                    }
                } finally {
                    resumeWatcher();
                }
            }
            case PackListIntent.Delete delete when delete.status().loading() -> {
                pauseWatcher();
                try {
                    if (fileOps.deletePack(delete.ctx().pack())) {
                        consumeWatchedChanges();
                        return delete.withSuccess();
                    } else {
                        ToastUtil.onDeleteFailToast(delete.ctx().pack().getTitle());
                        return delete.withFail();
                    }
                } finally {
                    resumeWatcher();
                }
            }
            case ProfileIntent.Select selected when selected.profile() == null -> {
                return new ProfileIntent.SelectNone(repository.getPacksBySelected(state.profiles().options()));
            }
            case ProfileIntent.Add add -> {
                Profile selectedProfile = this.state.profiles().selectedProfile();
                if (selectedProfile != null) {
                    saveFolderState(this.state.enabled().folder());
                    selectedProfile.setPacks(this.state.enabled().packs());
                    configs.profiles().save(selectedProfile);
                }
                add.profile().setPacks(this.state.enabled().packs());
            }
            case ProfileIntent.Rename rename -> configs.profiles().rename(rename.profile(), rename.name());
            case ProfileIntent.Delete delete when delete.status().loading() -> {
                return configs.profiles().delete(delete.profile())
                        ? delete.withSuccess(repository.getPacksBySelected(this.state.profiles().options()))
                        : delete.withFail();
            }
            case ProfileIntent.SetDefault(Profile profile) -> configs.profiles().setDefault(profile);
            case ProfileIntent.ToggleLock(Profile profile) -> profile.setLocked(!profile.isLocked());
            case PackListIntent.CloseAliases aliases ->
                    configs.dev().setAliases(aliases.ctx().pack().getId(), aliases.aliases());
            default -> {
            }
        }
        return intent;
    }

    public void dispatch(Intent intent) {
        Intent result = applyIntent(intent);
        PackedPacksState prev = this.state;
        PackedPacksState current = reducer.reduce(prev, result);
        this.state = current;

        if (prev != current) {
            if (result.resetHistory()) {
                history.reset(current);
            } else if (result.pushState() && isUnlocked()) {
                history.push(current);
            }
            subscribers.values().forEach(Runnable::run);
            handleEffects(prev, current, result);
        }
    }

    private void handleEffects(PackedPacksState prev, PackedPacksState current, Intent intent) {
        if (prev.profiles() != current.profiles()) {
            current.available().packs().forEach(options::validate);
            current.enabled().packs().forEach(options::validate);
        }

        if (prev.profiles().selectedProfile() != current.profiles().selectedProfile()) {
            Profile previousProfile = prev.profiles().selectedProfile();
            saveFolderState(prev.available().folder());
            saveFolderState(prev.enabled().folder());
            if (previousProfile != null && current.profiles().profiles().contains(previousProfile)) {
                previousProfile.setPacks(prev.enabled().packs());
                configs.profiles().save(previousProfile);
            }
        }

        if (intent instanceof PackListIntent.Rename(
                PackListKey target, PackContext ctx, String newName, Status status
        ) && status.success()) {
            refreshRepositoryBlocking();
            emitEffect(new UiEffect.Focus(target.type(), PackUtil.getNewIdOnRename(ctx.pack(), newName), true));
            return;
        }

        emitUiEffects(prev, current, intent);
    }

    private void emitUiEffects(PackedPacksState prev, PackedPacksState current, Intent intent) {
        if (intent instanceof PackListIntent.Reset || !Objects.equals(prev.profiles().selectedProfile(), current.profiles().selectedProfile())) {
            emitEffect(new UiEffect.ScrollToTop(PackListType.AVAILABLE));
            emitEffect(new UiEffect.ScrollToTop(PackListType.ENABLED));
            return;
        }

        if (intent instanceof PackListIntent packListIntent) {
            if (emitNavigationEffects(prev, packListIntent)) return;
            if (intent instanceof PackListIntent.Entry entryIntent && emitMoveEffects(prev, current, entryIntent)) {
                return;
            }
            emitTransferEffects(prev, current, packListIntent);
        }
    }

    private boolean emitNavigationEffects(PackedPacksState prev, PackListIntent intent) {
        return switch (intent) {
            case PackListIntent.OpenFolder ignored -> {
                emitEffect(new UiEffect.FocusList(intent.target().type()));
                yield true;
            }
            case PackListIntent.CloseFolder(PackListKey target) -> {
                PackListState listState = prev.targetList(target);
                if (listState == null || listState.folder() == null) yield false;
                emitEffect(new UiEffect.Focus(target.type(), listState.folder().pack().getId()));
                yield true;
            }
            case PackListIntent.Entry entry when entry instanceof PackListIntent.CloseAliases || entry instanceof PackListIntent.CloseRename -> {
                emitEffect(new UiEffect.Focus(entry.target().type(), entry.ctx().pack().getId()));
                yield true;
            }
            default -> false;
        };
    }

    private boolean emitMoveEffects(PackedPacksState prev, PackedPacksState current, PackListIntent.Entry intent) {
        PackListState prevList = prev.targetList(intent.target());
        PackListState currentList = current.targetList(intent.target());
        if (prevList != null && currentList != null && prevList.visiblePacks() != currentList.visiblePacks()) {
            if (intent instanceof PackListIntent.Drop drop && Objects.equals(drop.destination(), drop.target()) && drop.destination() != null) {
                emitEffect(new UiEffect.Focus(drop.destination().type()));
                return true;
            }
            if (intent instanceof PackListIntent.MoveUp || intent instanceof PackListIntent.MoveDown) {
                emitEffect(new UiEffect.Focus(PackListType.ENABLED, intent.ctx().pack().getId(), true));
                playClickSound();
                return true;
            }
        }
        return false;
    }

    private void emitTransferEffects(PackedPacksState prev, PackedPacksState current, PackListIntent intent) {
        if (prev.enabled().visiblePacks() == current.enabled().visiblePacks()) return;
        switch (intent) {
            case PackListIntent.Drag ignored -> emitEffect(new UiEffect.ScrollToLastSelected(PackListType.ENABLED));
            case PackListIntent.Drop drop when drop.destination() != null ->
                    emitEffect(new UiEffect.Focus(drop.destination().type(), drop.index() == -1));
            case PackListIntent.Enable ignored -> {
                emitEffect(new UiEffect.Focus(PackListType.ENABLED, true));
                playClickSound();
            }
            case PackListIntent.Disable ignored -> {
                emitEffect(new UiEffect.Focus(PackListType.AVAILABLE));
                playClickSound();
            }
            default -> {
                if (intent.target().type().available()) {
                    emitEffect(new UiEffect.ScrollToLastSelected(PackListType.ENABLED));
                }
            }
        }
    }

    private void playClickSound() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
    }

    public ProfilesViewModel createProfilesSlice() {
        ProfilesViewModel profilesSlice = new ProfilesViewModel(configs.profiles(), this::dispatch, this::value);
        subscribe("ProfilesViewModel", profilesSlice::onStateChanged);
        return profilesSlice;
    }

    public PackListViewModel createPackListSlice(PackListType type) {
        PackListContext ctx = new PackListContext(configs, options, this::saveFolder, fileOps::isOperable, iconManager::get);
        PackListViewModel model = switch (type) {
            case AVAILABLE -> PackListViewModel.available(ctx, () -> this.state.available(), this::dispatch);
            case ENABLED -> PackListViewModel.enabled(ctx, () -> this.state.enabled(), this::dispatch);
        };
        subscribe("PackListViewModel$" + type, model::onStateChanged);
        return model;
    }

    private void saveFolder(FolderPack folder, List<Pack> contents) {
        Pack valid = repository.getPackById(folder.getId());
        if (!(valid instanceof FolderPack folderPack)) return;

        pauseWatcher();
        folderPack.setContents(contents);
        consumeWatchedChanges();
        resumeWatcher();
    }

    private PackListState.@Nullable Folder revalidateFolder(PackListState.@Nullable Folder folder) {
        if (folder == null) return null;
        Pack pack = repository.getPackById(folder.pack().getId());
        if (!(pack instanceof FolderPack folderPack)) return null;

        return new PackListState.Folder(folderPack, folder.contents()
                .withPacks(folderPack.contents(), state.profiles().options())
                .withFolder(revalidateFolder(folder.contents().folder())));
    }

    public void cancelRefresh() {
        var future = this.refreshFuture;
        if (future != null && !future.isDone()) {
            this.refreshFuture.cancel(true);
        }
    }

    public void refreshRepository() {
        cancelRefresh();
        this.refreshFuture = CompletableFuture.runAsync(repository::refresh, Util.backgroundExecutor())
                .thenRunAsync(this::syncStateWithRepository, minecraft);
    }

    public void refreshRepositoryBlocking() {
        cancelRefresh();
        repository.refresh();
        syncStateWithRepository();
    }

    private void syncStateWithRepository() {
        PackGroup validated = repository.validatePacks(
                state.available().packs(),
                state.enabled().packs(),
                state.profiles().options()
        );
        PackListState newAvailable = state.available()
                .withPacks(validated.unselected(), state.profiles().options())
                .withFolder(revalidateFolder(state.available().folder()));
        PackListState newEnabled = state.enabled()
                .withPacks(validated.selected(), state.profiles().options())
                .withFolder(revalidateFolder(state.enabled().folder()));

        iconManager.clear();
        replaceState(this.state.withPackLists(newAvailable, newEnabled));
        history.reset(this.state);
    }

    public boolean isRefreshing() {
        return this.refreshFuture != null && !this.refreshFuture.isDone();
    }

    public boolean canRefresh() {
        return !isRefreshing();
    }

    private void onPopHistory(PackedPacksState popped) {
        replaceState(popped);
        emitEffect(new UiEffect.Focus(popped.lastTarget().type(), true));
        emitEffect(new UiEffect.ScrollToLastSelected(popped.lastTarget().type().other()));
    }

    public void undo() {
        if (isUnlocked()) {
            history.undo().ifPresent(this::onPopHistory);
        }
    }

    public void redo() {
        if (isUnlocked()) {
            history.redo().ifPresent(this::onPopHistory);
        }
    }

    public boolean isUnlocked() {
        Profile selectedProfile = this.state.profiles().selectedProfile();
        return selectedProfile == null || !selectedProfile.isLocked();
    }

    public void resetChanges() {
        dispatch(new PackListIntent.Reset(this.state.lastTarget(), repository.getPacksBySelected(this.state.profiles().options())));
    }

    public void switchDefaultProfile() {
        Profile selectedProfile = this.state.profiles().selectedProfile();
        Profile defaultProfile = this.state.profiles().defaultProfile();
        if (defaultProfile != null) {
            dispatch(new ProfileIntent.Select(Objects.equals(defaultProfile, selectedProfile) ? null : defaultProfile));
        }
    }

    public void saveSelectedProfile() {
        Profile selectedProfile = this.state.profiles().selectedProfile();
        if (selectedProfile != null) {
            saveFolderState(this.state.enabled().folder());
            selectedProfile.setPacks(this.state.enabled().packs());
            configs.profiles().save(selectedProfile);
        }
    }

    private void syncSelectedProfile() {
        Profile selectedProfile = this.state.profiles().selectedProfile();
        if (selectedProfile != null) {
            selectedProfile.syncPacks(repository.getPacks(), this.state.enabled().packs());
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

    private List<Path> resolveAdditionalFolders() {
        Set<Path> additionalFolders = new LinkedHashSet<>(mapValidDirectories(configs.user().getAdditionalFolders()));
        additionalFolders.addAll(repository.getAdditionalDirs());
        return List.copyOf(additionalFolders);
    }

    public List<Path> getAdditionalFolders() {
        return this.additionalFolders;
    }

    public Path getBaseDir() {
        return repository.getBaseDir();
    }

    public void openBaseDir() {
        Util.getPlatform().openPath(repository.getBaseDir());
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
                    paths.add(repository.getBaseDir());
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
                    stopWatcher();
                }
            }, minecraft);
        }
    }

    public void pollWatcher() {
        if (this.watcher != null) {
            this.watcher.poll();
        }
    }

    private void pauseWatcher() {
        if (this.watcher != null) {
            this.watcher.pause();
        }
    }

    private void resumeWatcher() {
        if (this.watcher != null) {
            this.watcher.resume();
        }
    }

    private void consumeWatchedChanges() {
        if (this.watcher != null) {
            this.watcher.consumeChanges();
        }
    }

    public void commit() {
        saveFolderState(state.enabled().folder());
        syncSelectedProfile();
        repository.selectPacks(state.enabled().packs());
        reload.accept(repository.getRepository());
        replaceState(state.withPackLists(state.available(), state.enabled().withQuery(Query.empty(), state.profiles().options())));
    }

    public void prepareInitialState(InitMode initMode) {
        if (this.initialStateFuture != null) {
            this.initialStateFuture.cancel(true);
        }
        this.initialStateFuture = CompletableFuture.completedFuture(buildInitialState(initMode, repository, configs));
    }

    public void initializeState() {
        if (this.initialStateFuture != null) {
            replaceState(this.initialStateFuture.join());
            this.initialStateFuture = null;
        }

        history.reset(this.state);
        this.additionalFolders = resolveAdditionalFolders();
    }

    private void saveFolderState(PackListState.@Nullable Folder folderState) {
        if (folderState == null) return;
        saveFolderState(folderState.contents().folder());
        saveFolder(folderState.pack(), folderState.contents().packs());
    }

    public void saveState() {
        if (this.initialStateFuture != null) {
            this.state = this.initialStateFuture.join();
            this.initialStateFuture = null;
        }

        saveFolderState(this.state.available().folder());
        saveFolderState(this.state.enabled().folder());

        Query query = this.state.available().query();
        Config.get().setSort(query.sort() == null ? Query.SortOption.VANILLA : query.sort());
        Config.get().setHideIncompatible(query.hideIncompatible());

        syncSelectedProfile();
        configs.profiles().setLastViewed(this.state.profiles().selectedProfile());
        configs.profiles().setOrder(this.state.profiles().profiles());

        Profile selectedProfile = this.state.profiles().selectedProfile();
        Runnable profileSaver = selectedProfile != null
                ? () -> configs.profiles().save(selectedProfile)
                : FunctionUtils.nop();

        Utils.runInParallel(profileSaver, Config.get()::save, DevConfig.get()::save, Preferences::save);
    }

    public boolean shouldCommitOnClose() {
        return !(configs.user() instanceof Config.ResourcePacks r) || r.isApplyOnClose();
    }

    public boolean closeFolder(PackListType type) {
        PackListKey deepest = this.state.deepestTarget(type);
        PackListKey parent = deepest.unnest();
        if (parent.depth() < 0) return false;

        dispatch(new PackListIntent.CloseFolder(parent));
        return true;
    }

    public boolean closeFolder() {
        PackListType targetType = this.state.lastTarget().type();
        return closeFolder(targetType) || closeFolder(targetType.other());
    }
}
