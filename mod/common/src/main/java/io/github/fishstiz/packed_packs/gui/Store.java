package io.github.fishstiz.packed_packs.gui;

import io.github.fishstiz.fidgetz.v0.gui.state.FZRef;
import io.github.fishstiz.fidgetz.v0.utils.FunctionUtils;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.api.events.WatchEvent;
import io.github.fishstiz.packed_packs.config.*;
import io.github.fishstiz.packed_packs.gui.model.PackListKey;
import io.github.fishstiz.packed_packs.gui.model.PackListType;
import io.github.fishstiz.packed_packs.gui.model.Query;
import io.github.fishstiz.packed_packs.gui.states.ActiveAction;
import io.github.fishstiz.packed_packs.gui.states.PackListState;
import io.github.fishstiz.packed_packs.gui.states.ProfilesState;
import io.github.fishstiz.packed_packs.gui.actions.intents.Intent;
import io.github.fishstiz.packed_packs.gui.actions.intents.PackListIntent;
import io.github.fishstiz.packed_packs.gui.actions.intents.ProfileIntent;
import io.github.fishstiz.packed_packs.gui.actions.mutations.Mutation;
import io.github.fishstiz.packed_packs.gui.states.PackedPacksState;
import io.github.fishstiz.packed_packs.gui.actions.mutations.PackListMutation;
import io.github.fishstiz.packed_packs.gui.actions.mutations.ProfileMutation;
import io.github.fishstiz.packed_packs.gui.services.PackIconCache;
import io.github.fishstiz.packed_packs.gui.services.PackRepositoryService;
import io.github.fishstiz.packed_packs.gui.services.PackResourcesService;
import io.github.fishstiz.packed_packs.util.PackEntryResolver;
import io.github.fishstiz.packed_packs.impl.PackedPacksApiImpl;
import io.github.fishstiz.packed_packs.pack.PackEntry;
import io.github.fishstiz.packed_packs.util.PackEntryLists;
import io.github.fishstiz.packed_packs.pack.PackWatcher;
import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.packed_packs.util.ToastUtil;
import io.github.fishstiz.packed_packs.util.Utils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.locale.Language;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.util.Util;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.github.fishstiz.packed_packs.util.PackUtil.mapValidDirectories;

public class Store implements FZRef<PackedPacksState> {
    private final Minecraft minecraft;
    private final PackConfigs configs;
    private final PackRepositoryService repository;
    private final Consumer<PackRepository> onCommit;
    private final PackResourcesService resources;
    private final HistoryManager<PackedPacksState> history;
    private final ConcurrentHashMap<String, Runnable> subscribers = new ConcurrentHashMap<>();
    private Consumer<UiEffect> effectHandler = FunctionUtils.nopConsumer();
    private PackedPacksState state = PackedPacksState.empty();
    private volatile @Nullable List<Path> otherDirectorySources;
    private volatile @Nullable PackWatcher watcher;
    private @Nullable CompletableFuture<Void> refreshFuture;
    private @Nullable CompletableFuture<Void> initialStateFuture;

    public Store(
            Minecraft minecraft,
            PackConfigs configs,
            Path baseDir,
            PackRepository repository,
            Consumer<PackRepository> onCommit
    ) {
        this.minecraft = minecraft;
        this.configs = configs;
        this.history = new HistoryManager<>(state);
        this.repository = new PackRepositoryService(repository, baseDir);
        this.initialStateFuture = CompletableFuture.runAsync(this.repository::refreshSources, Util.backgroundExecutor());
        this.resources = new PackResourcesService(this.repository, new PackIconCache(minecraft, minecraft.getTextureManager()));
        this.onCommit = onCommit;
    }

    @Override
    public PackedPacksState value() {
        return state;
    }

    @Override
    public <R> Runnable subscribe(String key, Function<PackedPacksState, R> selector, Consumer<R> callback) {
        MutableObject<R> last = new MutableObject<>(selector.apply(this.state));
        Runnable listener = () -> {
            R next = selector.apply(this.state);
            if (next != last.get()) {
                callback.accept(next);
                last.setValue(next);
            }
        };
        subscribers.put(key, listener);
        return () -> subscribers.remove(key);
    }

    public void setEffectHandler(Consumer<UiEffect> effectHandler) {
        this.effectHandler = effectHandler;
    }

    private void replaceState(PackedPacksState state) {
        if (this.state != state) {
            this.state = state;
            subscribers.values().forEach(Runnable::run);
        }
    }

    private boolean dispatch(Mutation mutation) {
        PackedPacksState prevState = this.state;
        PackedPacksState newState = Reducer.reduce(prevState, mutation);
        this.state = newState;

        if (prevState != newState) {
            if (mutation.pushState()) {
                history.push(newState);
            } else if (mutation.resetHistory()) {
                history.reset(newState);
            }

            subscribers.values().forEach(Runnable::run);
            onStateChanged(prevState, newState);
            return true;
        }

        return false;
    }

    public void dispatch(Intent intent) {
        PackedPacksState prevState = this.state;
        switch (intent) {
            case Intent.Reset(@Nullable Profile profile) -> {
                if (dispatch(new Mutation.Reset(profile == null ? getCurrentPacks() : getPacks(profile)))) {
                    effectHandler.accept(new UiEffect.ScrollToTop(PackListType.AVAILABLE));
                    effectHandler.accept(new UiEffect.ScrollToTop(PackListType.ENABLED));
                }
            }
            case PackListIntent packListIntent -> {
                switch (packListIntent) {
                    case PackListIntent.Enable enable -> {
                        if (dispatch(new PackListMutation.Enabled(enable.srcList(), enable.srcPack(), enable.packs(), enable.index()))
                            && prevState.enabled() != state.enabled()) {
                            effectHandler.accept(new UiEffect.Focus(PackListType.AVAILABLE));
                            AbstractWidget.playButtonClickSound(minecraft.getSoundManager());
                        }
                    }
                    case PackListIntent.Disable disable -> {
                        if (dispatch(new PackListMutation.Disabled(disable.srcList(), disable.srcPack(), disable.packs()))
                            && prevState.enabled() != state.enabled()) {
                            effectHandler.accept(new UiEffect.Focus(PackListType.AVAILABLE));
                            AbstractWidget.playButtonClickSound(minecraft.getSoundManager());
                        }
                    }
                    case PackListIntent.Drag drag -> {
                        if (dispatch(new PackListMutation.Dragged(drag.srcList(), drag.srcPack(), drag.packs()))
                            && prevState.enabled() != state.enabled()) {
                            effectHandler.accept(new UiEffect.ScrollToTop(PackListType.ENABLED));
                        }
                    }
                    case PackListIntent.Drop drop -> {
                        if (dispatch(new PackListMutation.Dropped(drop.srcList(), drop.targetList(), drop.index()))
                            && drop.targetList() != null) {
                            if (drop.targetList().equals(drop.srcList())) {
                                effectHandler.accept(new UiEffect.Focus(drop.targetList().type()));
                            } else if (prevState.enabled() != state.enabled()) {
                                effectHandler.accept(new UiEffect.Focus(drop.targetList().type(), drop.index() == -1));
                            }
                        }
                    }
                    case PackListIntent.HideIncompatible(PackListKey srcList, boolean hide) ->
                            dispatch(new PackListMutation.IncompatibleHidden(srcList, hide));
                    case PackListIntent.Move move -> {
                        if (dispatch(new PackListMutation.Moved(move.srcList(), move.srcPack(), move.packs(), move.index()))) {
                            effectHandler.accept(new UiEffect.Focus(move.srcList().type()));
                        }
                    }
                    case PackListIntent.MoveOnce moveOnce -> {
                        if (dispatch(new PackListMutation.MovedOnce(
                                moveOnce.srcList(),
                                moveOnce.srcPack(),
                                moveOnce.packs(),
                                moveOnce.upwards()
                        ))) {
                            effectHandler.accept(new UiEffect.Focus(PackListType.ENABLED, moveOnce.srcPack().id(), true));
                            AbstractWidget.playButtonClickSound(minecraft.getSoundManager());
                        }
                    }
                    case PackListIntent.OpenAliasesModal open -> {
                        List<String> aliases = configs.dev().getAliases(open.pack().id());
                        dispatch(new PackListMutation.AliasesModalOpened(open.srcList(), open.pack(), aliases));
                    }
                    case PackListIntent.OpenFolder open -> {
                        List<PackEntry> unsortedChildren = open.pack().children();
                        FolderPackMeta metadata = Objects.requireNonNullElseGet(
                                repository.getFolderMetadata(open.pack().id()),
                                FolderPackMeta::new
                        );
                        List<PackEntry> children = PackEntryResolver.resolveChildren(
                                unsortedChildren,
                                metadata,
                                open.srcList().type(),
                                state.enabled()
                        );
                        if (dispatch(new PackListMutation.FolderOpened(open.srcList(), open.pack(), metadata.module(), children))) {
                            effectHandler.accept(new UiEffect.FocusList(open.srcList().type()));
                        }
                    }
                    case PackListIntent.OpenRenameModal open ->
                            dispatch(new PackListIntent.OpenRenameModal(open.srcList(), open.pack()));
                    case PackListIntent.CloseAliases(List<String> newAliases) -> {
                        ActiveAction.EditingAliases aliases = state.editingAliases();
                        if (aliases == null) return;
                        configs.dev().setAliases(aliases.pack().id(), newAliases);
                        if (dispatch(new PackListMutation.AliasesModalClosed())) {
                            effectHandler.accept(new UiEffect.Focus(aliases.target().type(), aliases.pack().id()));
                        }
                    }
                    case PackListIntent.CloseFolder close -> {
                        PackListState list = state.getList(close.srcList());
                        if (list == null) return;

                        PackListState.Folder folder = list.folder();
                        if (folder == null) return;

                        if (dispatch(new PackListMutation.FolderClosed(close.srcList()))) {
                            PackListState prevListState = prevState.getList(close.srcList());
                            if (prevListState != null && prevListState.folder() != null) {
                                effectHandler.accept(new UiEffect.Focus(close.srcList().type(), prevListState.folder().pack().id()));
                            }
                        }
                    }
                    case PackListIntent.CloseRenameModal close -> {
                        if (dispatch(new PackListMutation.RenameModalClosed(close.srcList(), close.pack()))) {
                            effectHandler.accept(new UiEffect.Focus(close.srcList().type(), close.pack().id()));
                        }
                    }
                    case PackListIntent.OverrideHidden override -> dispatch(new PackListMutation.VisibilityOverridden(
                            override.srcList(),
                            override.srcPack(),
                            override.packs(),
                            override.hidden()
                    ));
                    case PackListIntent.OverridePosition override -> dispatch(new PackListMutation.PositionOverridden(
                            override.srcList(),
                            override.srcPack(),
                            override.packs(),
                            override.position()
                    ));
                    case PackListIntent.OverrideRequirement override ->
                            dispatch(new PackListMutation.RequirementOverridden(
                                    override.srcList(),
                                    override.srcPack(),
                                    override.packs(),
                                    override.required()
                            ));
                    case PackListIntent.RemoveOverrides remove -> dispatch(new PackListMutation.OverridesRemoved(
                            remove.srcList(),
                            remove.srcPack(),
                            remove.packs()
                    ));
                    case PackListIntent.Rename rename -> {
                        dispatch(new Mutation.PackRenaming(true));
                        pauseWatcher();
                        try {
                            if (!resources.renamePack(rename.pack(), state.profiles(), rename.newName())) {
                                if (dispatch(new Mutation.PackRenaming(false))) {
                                    ToastUtil.onRenameFailToast(rename.pack().title(), rename.newName());
                                }
                            } else {
                                consumeWatchedChanges();
                                refreshRepository().thenRunAsync(() -> {
                                    dispatch(new PackListMutation.RenameModalClosed(rename.srcList(), rename.pack()));
                                    effectHandler.accept(new UiEffect.Focus(
                                            rename.srcList().type(),
                                            PackUtil.getNewIdOnRename(rename.newName()),
                                            true
                                    ));
                                }, minecraft);
                            }
                        } finally {
                            resumeWatcher();
                        }
                    }
                    case PackListIntent.Delete delete -> {
                        pauseWatcher();
                        try {
                            if (!resources.deletePack(delete.pack(), state.profiles())) {
                                ToastUtil.onDeleteFailToast(delete.pack().title());
                            } else {
                                consumeWatchedChanges();
                                refreshRepositoryBlocking();
                            }
                        } finally {
                            resumeWatcher();
                        }
                    }
                    case PackListIntent.Search search ->
                            dispatch(new PackListMutation.Searched(search.srcList(), search.query()));
                    case PackListIntent.Select select ->
                            dispatch(new PackListMutation.Selected(select.srcList(), select.pack()));
                    case PackListIntent.SelectAll select ->
                            dispatch(new PackListMutation.SelectedAll(select.srcList(), select.pack()));
                    case PackListIntent.SelectExclusive select ->
                            dispatch(new PackListMutation.SelectedExclusively(select.srcList(), select.pack()));
                    case PackListIntent.SelectRange select ->
                            dispatch(new PackListMutation.SelectedRange(select.srcList(), select.pack()));
                    case PackListIntent.SelectToggle select ->
                            dispatch(new PackListMutation.SelectionToggled(select.srcList(), select.pack()));
                    case PackListIntent.Sort sort -> dispatch(new PackListMutation.Sorted(sort.srcList(), sort.sort()));
                }
            }
            case ProfileIntent profileIntent -> {
                switch (profileIntent) {
                    case ProfileIntent.CopySelected() -> {
                        Profile selectedProfile = state.profiles().selectedProfile();
                        Profile copiedProfile;

                        if (selectedProfile == null) {
                            copiedProfile = configs.profiles().create(
                                    Language.getInstance().getOrDefault("packed_packs.profile.unnamed")
                            );
                        } else {
                            configs.profiles().save(selectedProfile);
                            List<String> enabled = new ObjectArrayList<>(state.enabled().packs().size());
                            state.enabled().packs().forEach(enabledEntry ->
                                    enabledEntry.visitEntries(entry -> enabled.add(entry.id()))
                            );

                            selectedProfile.setPacks(enabled);
                            copiedProfile = configs.profiles().copy(selectedProfile);
                        }

                        if (dispatch(new ProfileMutation.Added(copiedProfile))) {
                            effectHandler.accept(new UiEffect.ScrollToTop(PackListType.AVAILABLE));
                            effectHandler.accept(new UiEffect.ScrollToTop(PackListType.ENABLED));
                        }
                    }
                    case ProfileIntent.Delete(Profile profile) -> {
                        if (configs.profiles().delete(profile)) {
                            if (state.profiles().isSelected(profile)) {
                                if (dispatch(new ProfileMutation.DeletedAndReset(profile, getCurrentPacks()))) {
                                    effectHandler.accept(new UiEffect.ScrollToTop(PackListType.AVAILABLE));
                                    effectHandler.accept(new UiEffect.ScrollToTop(PackListType.ENABLED));
                                }
                            } else {
                                dispatch(new ProfileMutation.Deleted(profile));
                            }
                        }
                    }
                    case ProfileIntent.Rename(Profile profile, String name) -> {
                        configs.profiles().rename(profile, name);
                        dispatch(new ProfileMutation.Renamed(profile, profile.getName()));
                    }
                    case ProfileIntent.Select(@Nullable Profile profile) -> {
                        Profile selectedProfile = state.profiles().selectedProfile();
                        if (selectedProfile != null) {
                            configs.profiles().save(selectedProfile);
                        }

                        PackEntryLists packs = profile == null ? getCurrentPacks() : getPacks(profile);

                        if (dispatch(new ProfileMutation.Selected(profile, packs))) {
                            effectHandler.accept(new UiEffect.ScrollToTop(PackListType.AVAILABLE));
                            effectHandler.accept(new UiEffect.ScrollToTop(PackListType.ENABLED));
                        }
                    }
                    case ProfileIntent.SetDefault(@Nullable Profile profile) -> {
                        configs.profiles().setDefault(profile);

                        if (profile == null) {
                            dispatch(new ProfileMutation.DefaultRemoved());
                        } else {
                            dispatch(new ProfileMutation.DefaultChanged(
                                    profile,
                                    state.profiles().isSelected(profile) ? null : getPacks(profile)
                            ));
                        }
                    }
                    case ProfileIntent.ToggleLock(Profile profile) ->
                            dispatch(new ProfileMutation.LockToggled(profile));
                    case ProfileIntent.ToggleRenaming() -> dispatch(new ProfileMutation.RenamingToggled());
                }
            }
        }
    }

    private void onStateChanged(PackedPacksState prev, PackedPacksState current) {
        if (prev.profiles() != current.profiles()) {
            current.available().packs().forEach(current.profiles()::validate);
            current.enabled().packs().forEach(current.profiles()::validate);
        }

        if (prev.profiles().selectedProfile() != current.profiles().selectedProfile()) {
            Profile previousProfile = prev.profiles().selectedProfile();
            saveFolderState(prev.available().folder());
            saveFolderState(prev.enabled().folder());
            if (previousProfile != null && current.profiles().profiles().contains(previousProfile)) {
                previousProfile.setPacks(prev.enabled().packs().stream().map(PackEntry::id).toList());
                configs.profiles().save(previousProfile);
            }
        }
    }

    private void onPopHistory(PackedPacksState popped) {
        replaceState(popped);
        effectHandler.accept(new UiEffect.Focus(popped.lastTarget().type(), true));
        effectHandler.accept(new UiEffect.ScrollToLastSelected(popped.lastTarget().type().other()));
    }

    public void undo() {
        if (!state.profiles().isLocked()) {
            history.undo().ifPresent(this::onPopHistory);
        }
    }

    public void redo() {
        if (!state.profiles().isLocked()) {
            history.redo().ifPresent(this::onPopHistory);
        }
    }

    public PackResourcesService getPackResourcesService() {
        return resources;
    }

    public List<Pack> getDisabledPacks() {
        List<Pack> disabled = new ObjectArrayList<>(state.available().packs().size());
        state.available().packs().forEach(pack -> pack.visitPacks(disabled::add));
        return disabled;
    }

    public List<Pack> getEnabledPacks() {
        List<Pack> enabled = new ObjectArrayList<>(state.enabled().packs().size());
        state.enabled().packs().forEach(pack -> pack.visitPacks(enabled::add));
        return enabled;
    }

    private PackEntryLists getCurrentPacks() {
        // todo cache this maybe
        return PackEntryResolver.syncPackLists(
                repository,
                state.profiles(),
                Collections.emptyList(),
                repository.getEnabledPacks()
        );
    }

    private PackEntryLists getPacks(Profile profile) {
        return PackEntryResolver.syncPackLists(
                repository,
                state.profiles(),
                Collections.emptyList(),
                repository.getPacksById(profile.getPackIds())
        );
    }

    public Path getBaseDirectorySource() {
        return repository.baseDirectorySource();
    }

    public List<Path> getOtherDirectorySources() {
        if (this.otherDirectorySources == null) {
            Set<Path> additionalFolders = new LinkedHashSet<>(mapValidDirectories(configs.user().getAdditionalFolders()));
            additionalFolders.addAll(repository.otherDirectorySources());
            this.otherDirectorySources = List.copyOf(additionalFolders);
        }
        return this.otherDirectorySources;
    }

    public void startWatcher(ScreenContext context) {
        if (this.watcher == null) {
            List<Path> otherSources = getOtherDirectorySources();
            List<Path> directories = new ArrayList<>(otherSources.size() + 1);
            directories.add(repository.baseDirectorySource());
            directories.addAll(otherSources);
            PackWatcher watcher = new PackWatcher(directories, path -> {
                if (!PackedPacksApiImpl.getInstance().eventBus().post(new WatchEvent(context, path)).isCanceled()) {
                    refreshRepository();
                }
            });
            watcher.consumeChanges();
            this.watcher = watcher;
        }
    }

    public void stopWatcher() {
        this.watcher = null;
        cancelRefresh();
    }

    public void pollWatcher() {
        PackWatcher watcher = this.watcher;
        if (watcher != null) {
            watcher.poll();
        }
    }

    private void pauseWatcher() {
        PackWatcher watcher = this.watcher;
        if (watcher != null) {
            watcher.pause();
        }
    }

    private void resumeWatcher() {
        PackWatcher watcher = this.watcher;
        if (watcher != null) {
            watcher.resume();
        }
    }

    private void consumeWatchedChanges() {
        PackWatcher watcher = this.watcher;
        if (watcher != null) {
            watcher.consumeChanges();
        }
    }

    public boolean canRefresh() {
        CompletableFuture<Void> future = this.refreshFuture;
        return future == null || future.isDone();
    }

    public void cancelRefresh() {
        CompletableFuture<Void> future = this.refreshFuture;
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
    }

    public CompletableFuture<Void> refreshRepository() {
        cancelRefresh();
        CompletableFuture<Void> future = CompletableFuture.runAsync(repository::refreshSources, Util.backgroundExecutor())
                .thenRunAsync(() -> syncStateWithRepository(state, repository), minecraft);

//                .thenApplyAsync(ignored -> Pair.of(this.state, this.repository), minecraft)
//                .thenApplyAsync(state -> syncStateWithRepository(state.getFirst(), state.getSecond()), Util.backgroundExecutor())
//                .thenAcceptAsync(state -> {
//                    resources.clearIcons();
//                    replaceState(state);
//                }, minecraft);

        this.refreshFuture = future;
        return future;
    }

    public void refreshRepositoryBlocking() {
        cancelRefresh();
        repository.refreshSources();
        resources.clearIcons();
        replaceState(syncStateWithRepository(state, repository));
    }

    private void saveFolderMeta(PackEntry.Parent parent, FolderPackMeta metadata) {
        if (repository.setFolderMetadata(parent.id(), metadata)) {
            resources.saveFolderMetadata(parent.path(), metadata);
        }
    }

    private void saveFolderState(PackListState.@Nullable Folder folder) {
        if (folder == null) return;
        saveFolderState(folder.contents().folder());
        saveFolderMeta(
                folder.pack(),
                new FolderPackMeta(folder.locked(), folder.contents().packs().stream().map(PackEntry::id).toList())
        );
    }

    private static PackListState.@Nullable Folder syncFolderStateWithRepository(
            PackRepositoryService repository,
            Set<String> enabledIds,
            PackListState.@Nullable Folder folder,
            ProfilesState profiles,
            boolean devMode
    ) {
        if (folder == null) {
            return null;
        }

        if (!(repository.getPackById(folder.pack().id()) instanceof PackEntry.Parent canonicalParent)) {
            return null;
        }

        Set<String> oldChildIds = folder.pack().children().stream()
                .map(PackEntry::id)
                .collect(Collectors.toCollection(ObjectOpenHashSet::new));

        Set<String> canonicalChildIds = canonicalParent.children().stream()
                .map(PackEntry::id)
                .collect(Collectors.toCollection(ObjectOpenHashSet::new));

        List<PackEntry> newContents = new ObjectArrayList<>(folder.contents().packs().size());
        Set<String> currentContentIds = new ObjectOpenHashSet<>(folder.contents().packs().size());

        for (PackEntry entry : folder.contents().packs()) {
            if (canonicalChildIds.contains(entry.id()) && !enabledIds.contains(entry.id())) {
                newContents.add(entry);
                currentContentIds.add(entry.id());
            }
        }

        for (PackEntry child : canonicalParent.children()) {
            if (enabledIds.contains(child.id())) continue;
            if (!oldChildIds.contains(child.id()) && currentContentIds.add(child.id())) {
                newContents.add(child);
            }
        }

        PackListState.Folder nestedFolder = folder.contents().folder();
        PackListState.Folder newNestedFolder = nestedFolder != null && currentContentIds.contains(nestedFolder.pack().id())
                ? syncFolderStateWithRepository(repository, enabledIds, nestedFolder, profiles, devMode)
                : null;

        return new PackListState.Folder(canonicalParent, folder.locked(), folder.contents()
                .withPacks(newContents, profiles, devMode)
                .withFolder(newNestedFolder));
    }

    private static PackedPacksState syncStateWithRepository(PackedPacksState state, PackRepositoryService repository) {
        long start = 0;

        if (PackedPacks.DEBUG) {
            start = System.nanoTime();
            PackedPacks.LOGGER.info("[packed_packs] ======== Syncing State ========");
        }

        PackEntryLists validated = PackEntryResolver.syncPackLists(
                repository,
                state.profiles(),
                state.available().packs(),
                state.enabled().packs()
        );

        ProfilesState profiles = state.profiles();
        boolean devMode = state.devMode();

        Set<String> enabledIds = state.available().folder() == null && state.enabled().folder() == null
                ? Collections.emptySet()
                : validated.enabled().stream()
                .map(PackEntry::id)
                .collect(Collectors.toCollection(ObjectOpenHashSet::new));

        PackListState newAvailable = state.available()
                .withPacks(validated.disabled(), profiles, devMode)
                .withFolder(syncFolderStateWithRepository(
                        repository,
                        enabledIds,
                        state.available().folder(),
                        profiles,
                        devMode
                ));

        PackListState newEnabled = state.enabled()
                .withPacks(validated.enabled(), profiles, devMode)
                .withFolder(syncFolderStateWithRepository(
                        repository,
                        enabledIds,
                        state.enabled().folder(),
                        profiles,
                        devMode
                ));

        PackedPacksState newState = state.withPackLists(newAvailable, newEnabled);

        if (PackedPacks.DEBUG) {
            long duration = (System.nanoTime() - start) / 1_000_000;
            PackedPacks.LOGGER.info("[packed_packs] ======== State Synced in {}ms ========", duration);
        }

        return newState;
    }

    private void syncSelectedProfile() {
        Profile selectedProfile = this.state.profiles().selectedProfile();
        if (selectedProfile != null) {
            selectedProfile.syncPacks(
                    repository.getPacks().stream()
                            .map(PackEntry::id)
                            .collect(Collectors.toSet()),
                    this.state.enabled().packs().stream()
                            .map(PackEntry::id)
                            .collect(Collectors.toCollection(LinkedHashSet::new))
            );
        }
    }

    public void saveSelectedProfile() {
        Profile selectedProfile = this.state.profiles().selectedProfile();
        if (selectedProfile != null) {
            saveFolderState(this.state.enabled().folder());
            selectedProfile.setPacks(this.state.enabled().packs().stream().map(PackEntry::id).toList());
            configs.profiles().save(selectedProfile);
        }
    }

    public void commit() {
        saveFolderState(state.available().folder());
        saveFolderState(state.enabled().folder());
        syncSelectedProfile();
        repository.setEnabledPacks(state.enabled().packs());
        onCommit.accept(repository.getRepository());
        replaceState(state.withPackLists(
                state.available(),
                state.enabled().withQuery(Query.empty(), state.profiles(), state.devMode())
        ));
    }

    public void saveState() {
        PackedPacksState state = this.state;

        Query query = state.available().query();
        Config.get().setSort(query.sort() == null ? Query.SortOption.VANILLA : query.sort());
        Config.get().setHideIncompatible(query.hideIncompatible());

        syncSelectedProfile();
        configs.profiles().setLastViewed(state.profiles().selectedProfile());
        configs.profiles().setDefault(state.profiles().defaultProfile());
        configs.profiles().setOrder(state.profiles().profiles());
        configs.user().setHideIncompatibleWarnings(state.hideWarnings());

        Profile selectedProfile = state.profiles().selectedProfile();
        Runnable profileSaver = selectedProfile != null
                ? () -> configs.profiles().save(selectedProfile)
                : FunctionUtils.nop();

        Utils.runInParallel(profileSaver, Config.get()::save, DevConfig.get()::save, Preferences::save);
    }

    public void initializeState() {
        PackedPacksState state = this.state;
        CompletableFuture<Void> initialStateFuture = this.initialStateFuture;

        if (state == PackedPacksState.empty() || initialStateFuture != null) {
            if (initialStateFuture != null) {
                initialStateFuture.join();
                this.initialStateFuture = null;
            }

            boolean devMode = Config.get().isDevMode();
            Profile defaultProfile = configs.profiles().getDefault();
            ProfilesState profileState = new ProfilesState(configs.profiles().getProfiles(), null, defaultProfile);
            PackListState availableState = PackListState.empty().withQuery(
                    new Query(Config.get().isHideIncompatible(), Config.get().getSort(), null, null),
                    profileState,
                    devMode
            );

            this.state = new PackedPacksState(
                    availableState,
                    PackListState.empty(),
                    profileState,
                    PackListKey.available(),
                    null,
                    devMode,
                    configs.user().isIncompatibleWarningsHidden()
            );

            dispatch(new Intent.Reset(
                    configs.user().isLastViewedProfileRemembered() ? configs.profiles().getLastViewed() : null
            ));
        }

        history.reset(state);
        this.otherDirectorySources = null;
    }

    public boolean closeFolder(PackListType type) {
        PackListKey leaf = this.state.getLeafKey(type);
        PackListKey parent = leaf.unnest();
        if (parent.depth() < 0) return false;

        dispatch(new PackListIntent.CloseFolder(parent));
        return true;
    }

    public boolean closeFolder() {
        PackListType targetType = this.state.lastTarget().type();
        return closeFolder(targetType) || closeFolder(targetType.other());
    }
}
