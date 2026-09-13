package io.github.fishstiz.packed_packs.gui2;

import io.github.fishstiz.fidgetz.v0.gui.state.FZRef;
import io.github.fishstiz.packed_packs.config.FolderPackMeta;
import io.github.fishstiz.packed_packs.config.PackConfigs;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.gui.HistoryManager;
import io.github.fishstiz.packed_packs.gui.model.PackListKey;
import io.github.fishstiz.packed_packs.gui.states.ActiveAction;
import io.github.fishstiz.packed_packs.gui.states.PackListState;
import io.github.fishstiz.packed_packs.gui2.actions.intents.Intent;
import io.github.fishstiz.packed_packs.gui2.actions.intents.PackListIntent;
import io.github.fishstiz.packed_packs.gui2.actions.intents.ProfileIntent;
import io.github.fishstiz.packed_packs.gui2.actions.mutations.Mutation;
import io.github.fishstiz.packed_packs.gui.states.PackedPacksState;
import io.github.fishstiz.packed_packs.gui2.actions.mutations.PackListMutation;
import io.github.fishstiz.packed_packs.gui2.actions.mutations.ProfileMutation;
import io.github.fishstiz.packed_packs.gui2.services.PackIconCache;
import io.github.fishstiz.packed_packs.gui2.services.PackRepositoryService;
import io.github.fishstiz.packed_packs.gui2.services.PackResourcesService;
import io.github.fishstiz.packed_packs.gui2.states.PackEntryResolver;
import io.github.fishstiz.packed_packs.models.PackEntry;
import io.github.fishstiz.packed_packs.models.PackEntryLists;
import io.github.fishstiz.packed_packs.util.ToastUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class Store implements FZRef<PackedPacksState> {
    private final Minecraft minecraft;
    private final PackConfigs configs;
    private final HistoryManager<PackedPacksState> history;
    private final PackRepositoryService repository;
    private final PackResourcesService resources;
    private final ConcurrentHashMap<String, Runnable> subscribers = new ConcurrentHashMap<>();
    private PackedPacksState state = PackedPacksState.empty();

    public Store(Minecraft minecraft, PackRepository repository, PackType packType, Path baseDir) {
        this.minecraft = minecraft;
        this.configs = PackConfigs.get(packType);
        this.history = new HistoryManager<>(state);
        this.repository = new PackRepositoryService(repository, baseDir);
        this.resources = new PackResourcesService(this.repository, new PackIconCache(minecraft, minecraft.getTextureManager()));
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

    private boolean dispatch(Mutation mutation) {
        PackedPacksState prevState = this.state;
        PackedPacksState newState = Reducer.reduce(prevState, mutation);

        if (prevState != newState) {
            if (mutation.pushState()) {
                history.push(newState);
            } else if (mutation.resetHistory()) {
                history.reset(newState);
            }

            subscribers.values().forEach(Runnable::run);
            return true;
        }
        return false;
    }

    public void dispatch(Intent intent) {
        switch (intent) { // todo effects/map to mutation
            case Intent.Reset() -> dispatch(new Mutation.Reset(getCurrentPacks()));
            case PackListIntent packListIntent -> {
                switch (packListIntent) {
                    case PackListIntent.CloseAliases() -> {
                        ActiveAction.EditingAliases aliases = state.editingAliases();
                        if (aliases == null) return;
                        configs.dev().setAliases(aliases.pack().id(), aliases.aliases());
                        dispatch(new PackListMutation.AliasesModalClosed());
                        // todo focus effect
                    }
                    case PackListIntent.CloseFolder close -> {
                        PackListState list = state.targetList(close.srcList());
                        if (list == null) return;

                        PackListState.Folder folder = list.folder();
                        if (folder == null) return;

                        FolderPackMeta folderPackMeta = repository.getFolderMetadata(folder.pack().id());
                        if (folderPackMeta != null) {
                            // todo consume watched changes
                            resources.saveFolderMetadata(folder.pack().path(), folderPackMeta);
                        }

                        // todo focus effect
                        dispatch(new PackListMutation.FolderClosed(close.srcList()));
                    }
                    case PackListIntent.CloseRenameModal close -> {
                        dispatch(new PackListMutation.RenameModalClosed(close.srcList(), close.pack()));
                        // todo focus effect
                    }
                    case PackListIntent.Delete delete -> {
                        if (resources.deletePack(delete.pack(), state.profiles())) {
                            // todo refresh repo instantly
                        }
                    }
                    case PackListIntent.Disable disable -> {
                        dispatch(new PackListMutation.Disabled(disable.srcList(), disable.srcPack(), disable.packs()));
                        // todo focus effect
                    }
                    case PackListIntent.Drag drag ->
                            dispatch(new PackListMutation.Dragged(drag.srcList(), drag.srcPack(), drag.packs()));
                    case PackListIntent.Drop drop -> {
                        dispatch(new PackListMutation.Dropped(drop.srcList(), drop.targetList(), drop.index()));
                        // todo focus effect
                    }
                    case PackListIntent.Enable enable -> {
                        dispatch(new PackListMutation.Enabled(enable.srcList(), enable.srcPack(), enable.packs(), enable.index()));
                        // todo focus effect
                    }
                    case PackListIntent.HideIncompatible(PackListKey srcList, boolean hide) ->
                            dispatch(new PackListMutation.IncompatibleHidden(srcList, hide));
                    case PackListIntent.Move move -> {
                        dispatch(new PackListMutation.Moved(move.srcList(), move.srcPack(), move.packs(), move.index()));
                        // todo focus effect
                    }
                    case PackListIntent.MoveOnce moveOnce -> {
                        dispatch(new PackListMutation.MovedOnce(
                                moveOnce.srcList(),
                                moveOnce.srcPack(),
                                moveOnce.packs(),
                                moveOnce.upwards()
                        ));
                        // todo focus effect
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

                        List<PackEntry> children;
                        if (metadata.module()) {
                            Map<String, PackEntry> contentById = new Object2ObjectOpenHashMap<>(
                                    unsortedChildren.size(),
                                    0.99f
                            );

                            for (PackEntry pack : unsortedChildren) {
                                contentById.put(pack.id(), pack);
                            }

                            List<String> orderedIds = metadata.packIds();
                            Set<PackEntry> seen = new ObjectOpenHashSet<>();
                            List<PackEntry> sorted = new ObjectArrayList<>(unsortedChildren.size());

                            for (String id : orderedIds) {
                                PackEntry pack = contentById.get(id);
                                if (pack != null && seen.add(pack)) sorted.add(pack);
                            }

                            for (PackEntry pack : unsortedChildren) {
                                if (seen.add(pack)) sorted.add(pack);
                            }

                            children = sorted;
                        } else {
                            children = unsortedChildren;
                        }

                        dispatch(new PackListMutation.FolderOpened(open.srcList(), open.pack(), metadata.module(), children));
                    }
                    case PackListIntent.OpenRenameModal open ->
                            dispatch(new PackListIntent.OpenRenameModal(open.srcList(), open.pack()));
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
                        if (!resources.renamePack(rename.pack(), state.profiles(), rename.newName())) {
                            ToastUtil.onRenameFailToast(rename.pack().title(), rename.newName());
                            dispatch(new Mutation.PackRenaming(false));
                        } else {
                            dispatch(new PackListMutation.RenameModalClosed(rename.srcList(), rename.pack()));
                            // todo consume changes and focus effects
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

                        dispatch(new ProfileMutation.Added(copiedProfile));
                    }
                    case ProfileIntent.Delete(Profile profile) -> {
                        if (configs.profiles().delete(profile)) {
                            if (isSelected(profile)) {
                                dispatch(new ProfileMutation.DeletedAndReset(profile, getCurrentPacks()));
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

                        PackEntryLists packs = profile == null
                                ? getCurrentPacks()
                                : getPacks(profile);

                        dispatch(new ProfileMutation.Selected(profile, packs));
                    }
                    case ProfileIntent.SetDefault(@Nullable Profile profile) -> {
                        configs.profiles().setDefault(profile);

                        if (profile == null) {
                            dispatch(new ProfileMutation.DefaultRemoved());
                        } else {
                            dispatch(new ProfileMutation.DefaultChanged(profile, isSelected(profile)
                                    ? null
                                    : getPacks(profile)
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

    private boolean isSelected(Profile profile) {
        Profile selectedProfile = state.profiles().selectedProfile();
        return selectedProfile != null && Objects.equals(selectedProfile.getId(), profile.getId());
    }
}
