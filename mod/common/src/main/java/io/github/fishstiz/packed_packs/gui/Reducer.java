package io.github.fishstiz.packed_packs.gui;

import com.google.common.primitives.Ints;
import io.github.fishstiz.fidgetz.v0.utils.CollectionUtils;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.gui.actions.mutations.Mutation;
import io.github.fishstiz.packed_packs.gui.model.PackListKey;
import io.github.fishstiz.packed_packs.util.PackListUtils;
import io.github.fishstiz.packed_packs.gui.model.Query;
import io.github.fishstiz.packed_packs.gui.states.ActiveAction;
import io.github.fishstiz.packed_packs.gui.states.PackListState;
import io.github.fishstiz.packed_packs.gui.states.PackedPacksState;
import io.github.fishstiz.packed_packs.gui.states.ProfilesState;
import io.github.fishstiz.packed_packs.gui.actions.mutations.PackListMutation;
import io.github.fishstiz.packed_packs.gui.actions.mutations.ProfileMutation;
import io.github.fishstiz.packed_packs.pack.PackNode;
import io.github.fishstiz.packed_packs.pack.PackSelection;
import io.github.fishstiz.packed_packs.gui.states.ProfileSelection;
import io.github.fishstiz.packed_packs.util.ToIntTriFunction;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jspecify.annotations.Nullable;

import java.util.*;

import static io.github.fishstiz.packed_packs.util.PackListUtils.*;

public final class Reducer {
    public static PackedPacksState reduce(PackedPacksState state, Mutation mutation) {
        return switch (mutation) {
            case Mutation.Reset reset -> {
                PackListState available = state.available();
                PackListState enabled = state.enabled();
                yield new PackedPacksState(
                        available.with(
                                reset.packs().disabled(),
                                Collections.emptyList(),
                                available.query().withSearch(""),
                                state.profiles(),
                                state.devMode()
                        ),
                        enabled.with(
                                reset.packs().enabled(),
                                Collections.emptyList(),
                                enabled.query().withSearch(""),
                                state.profiles(),
                                state.devMode()
                        ),
                        state.profiles(),
                        state.lastTarget().root(),
                        null,
                        state.devMode()
                );
            }
            case Mutation.PackRenaming renaming -> {
                ActiveAction.RenamingPack renamingState = state.renamingPack();
                if (renamingState != null) {
                    yield state.withAction(renamingState.withLoading(renaming.loading()));
                }
                yield state;
            }
            case PackListMutation packListMutation -> switch (packListMutation) {
                case PackListMutation.Local local -> switch (local.srcList().type()) {
                    case AVAILABLE -> {
                        PackListState newAvailable = reduceList(state.available(), 0, local, state.profiles(), state.devMode());
                        yield newAvailable == state.available() ? state : state.withAvailable(newAvailable, local.srcList());
                    }
                    case ENABLED -> {
                        PackListState newEnabled = reduceList(state.enabled(), 0, local, state.profiles(), state.devMode());
                        yield newEnabled == state.available() ? state : state.withEnabled(newEnabled, local.srcList());
                    }
                };
                case PackListMutation.Cross cross -> reduceList(state, cross);
            };
            case ProfileMutation profileMutation -> reduceProfile(state, profileMutation);
        };
    }

    private static PackListState reduceList(
            PackListState state,
            int depth,
            PackListMutation.Local mutation,
            ProfileSelection profiles,
            boolean devMode
    ) {
        if (depth == mutation.srcList().depth()) {
            return reduceList(state, mutation, profiles, devMode);
        }
        if (state.folder() == null || mutation.srcList().depth() < 0) {
            return state;
        }
        PackListState newContents = reduceList(state.folder().contents(), depth + 1, mutation, profiles, devMode);
        return newContents == state.folder().contents() ? state : state.withFolder(state.folder().withContents(newContents));
    }

    private static PackListState reduceList(
            PackListState state,
            PackListMutation.Local mutation,
            ProfileSelection profiles,
            boolean devMode
    ) {
        return switch (mutation) {
            case PackListMutation.Searched searched ->
                    state.withQuery(state.query().withSearch(searched.search()), profiles, devMode);
            case PackListMutation.Sorted sorted ->
                    state.withQuery(state.query().withSort(sorted.sort()), profiles, devMode);
            case PackListMutation.IncompatibleHidden incompatibleHidden ->
                    state.withQuery(state.query().withHideIncompatible(incompatibleHidden.hidden()), profiles, devMode);
            case PackListMutation.Selected selected -> {
                PackNode pack = selected.pack();
                if (!state.visiblePacks().contains(pack)) yield state;
                List<PackNode> newSelection = new ObjectArrayList<>(state.selectedPacks());
                newSelection.remove(pack);
                newSelection.add(pack);
                yield state.withSelection(newSelection);
            }
            case PackListMutation.SelectedExclusively selected -> {
                PackNode pack = selected.pack();
                if (!state.visiblePacks().contains(pack)) yield state;
                yield state.withSelection(List.of(pack));
            }
            case PackListMutation.SelectionToggled selected -> {
                PackNode pack = selected.pack();
                if (!state.visiblePacks().contains(pack)) yield state;
                List<PackNode> newSelection = new ObjectArrayList<>(state.selectedPacks());
                if (!newSelection.remove(pack)) newSelection.add(pack);
                yield state.withSelection(newSelection);
            }
            case PackListMutation.SelectedRange selected -> {
                if (state.selectedPacks().isEmpty()) {
                    yield state.withSelection(List.of(selected.pack()));
                }

                PackNode anchor = state.selectedPacks().getLast();
                int anchorIndex = state.visiblePacks().indexOf(anchor);
                int targetIndex = state.visiblePacks().indexOf(selected.pack());
                int[] indices = indicesOf(state.visiblePacks(), state.selectedPacks());
                Arrays.sort(indices);

                if (!(Ints.contains(indices, -1) || hasGap(indices, true)) && indices.length > 0) {
                    if (indices[0] == anchorIndex) {
                        anchor = state.visiblePacks().get(indices[indices.length - 1]);
                    } else if (indices[indices.length - 1] == anchorIndex) {
                        anchor = state.visiblePacks().get(indices[0]);
                    }
                }
                List<PackNode> newSelection = new ObjectArrayList<>(state.selectedPacks());
                int start = state.visiblePacks().indexOf(anchor);
                if (targetIndex != -1 && start != -1) {
                    newSelection.clear();
                    for (int i = Math.min(targetIndex, start); i <= Math.max(targetIndex, start); i++) {
                        PackNode entry = state.visiblePacks().get(i);
                        if (entry != null && entry != selected.pack() && state.visiblePacks().contains(entry)) {
                            newSelection.remove(entry);
                            newSelection.addLast(entry);
                        }
                    }
                }

                newSelection.add(selected.pack());
                yield state.withSelection(newSelection);
            }
            case PackListMutation.SelectedAll selected -> {
                ObjectLinkedOpenHashSet<PackNode> newSelection = new ObjectLinkedOpenHashSet<>(state.visiblePacks());
                if (selected.pack() != null) newSelection.addAndMoveToLast(selected.pack());
                yield state.withSelection(newSelection);
            }
            case PackListMutation.Moved moved -> {
                List<PackNode> ordered = sortByOrderOf(state.visiblePacks(), moved.packs());
                List<PackNode> newPacks = new ObjectArrayList<>(state.packs());
                int to = moved.index();
                int insertOffset = 0;
                for (PackNode pack : ordered) {
                    int previous = newPacks.indexOf(pack);
                    boolean isBeforeTo = previous != -1 && previous < to;
                    int target = isBeforeTo ? to - 1 : to + insertOffset;
                    newPacks.remove(pack);
                    newPacks.add(Math.clamp(target, 0, newPacks.size()), pack);
                    if (!isBeforeTo) insertOffset++;
                }
                yield newPacks.equals(state.packs()) ? state : state.withPacks(newPacks, profiles, devMode);
            }
            case PackListMutation.MovedOnce moved -> {
                List<PackNode> newPacks = moved.packs().isEmpty()
                        ? state.packs()
                        : movePacks(state, moved.packs(), moved.upwards(), profiles);

                yield state.withPacksAndSelectedLast(newPacks, moved.srcPack(), profiles, devMode);
            }
            case PackListMutation.FolderOpened opened -> state.withFolder(new PackListState.Folder(
                    opened.pack(),
                    opened.locked(),
                    new PackListState(opened.children()).withQuery(
                            state.query().withSort(opened.locked() ? null : state.query().sort()),
                            profiles,
                            devMode
                    )
            ));
            case PackListMutation.FolderClosed ignored -> state.withFolder(null);
        };
    }


    private static List<PackNode> movePacks(
            PackListState state,
            SequencedCollection<PackNode> payload,
            boolean up,
            ProfileSelection profiles
    ) {
        Set<PackNode> validPacks = new ObjectOpenHashSet<>(state.packs());
        List<PackNode> newPacks = new ObjectArrayList<>(state.packs());
        List<PackNode> sorted;
        ToIntTriFunction<List<PackNode>, PackNode, ProfileSelection> indexFn;

        if (up) {
            sorted = sortByOrderOf(state.visiblePacks(), payload);
            indexFn = PackListUtils::getMoveUpIndex;
        } else {
            sorted = sortByOrderOf(state.visiblePacks(), payload).reversed();
            indexFn = PackListUtils::getMoveDownIndex;
        }

        for (int i = 0; i < sorted.size(); i++) {
            PackNode pack = sorted.get(i);
            if (!validPacks.contains(pack)) continue;
            int targetIndex = indexFn.applyAsInt(newPacks, pack, profiles);
            if (targetIndex > -1 && targetIndex < newPacks.size() && !profiles.isPackFixed(pack)) {
                newPacks.remove(pack);
                newPacks.add(targetIndex, pack);
            } else if (i == 0) {
                return state.packs();
            }
        }

        return newPacks;
    }

    private static PackedPacksState reduceList(PackedPacksState state, PackListMutation.Cross mutation) {
        return switch (mutation) {
            // todo handle unlocked folders
            case PackListMutation.Enabled enabled -> {
                List<PackNode> newAvailable = new ObjectArrayList<>(state.available().packs());
                List<PackNode> newEnabled = new ObjectArrayList<>(state.enabled().packs());
                List<PackNode> newEnabledSelection = new ObjectArrayList<>(enabled.packs().size());

                for (PackNode pack : enabled.packs()) {
                    if (newAvailable.remove(pack)) {
                        newEnabled.add(enabled.index(), pack);
                        if (!pack.equals(enabled.srcPack())) {
                            newEnabledSelection.add(pack);
                        }
                    }
                }

                newEnabledSelection.add(enabled.srcPack());

                SequencedCollection<PackNode> newAvailableSelection =
                        state.available().selectedPacks().contains(enabled.srcPack())
                                ? state.available().selectedPacks()
                                : Collections.emptyList();

                yield state.withPackLists(
                        state.available().with(newAvailable, newAvailableSelection, state.profiles(), state.devMode()),
                        state.enabled().with(newEnabled, newEnabledSelection, state.profiles(), state.devMode()),
                        PackListKey.enabled()
                );
            }
            // todo handle unlocked folders
            case PackListMutation.Disabled disabled -> {
                List<PackNode> newAvailable = new ObjectArrayList<>(state.available().packs());
                List<PackNode> newEnabled = new ObjectArrayList<>(state.enabled().packs());
                List<PackNode> newAvailableSelection = new ObjectArrayList<>(disabled.packs().size());
                for (PackNode pack : disabled.packs()) {
                    if (newEnabled.remove(pack)) {
                        newAvailable.add(pack);
                        if (!disabled.srcPack().equals(pack)) {
                            newAvailableSelection.add(pack);
                        }
                    }
                }

                newAvailableSelection.add(disabled.srcPack());

                SequencedCollection<PackNode> newEnabledSelection =
                        state.enabled().selectedPacks().contains(disabled.srcPack())
                                ? state.enabled().selectedPacks()
                                : Collections.emptyList();

                yield state.withPackLists(
                        state.available().with(newAvailable, newAvailableSelection, state.profiles(), state.devMode()),
                        state.enabled().with(newEnabled, newEnabledSelection, state.profiles(), state.devMode()),
                        PackListKey.available()
                );
            }
            case PackListMutation.Dragged dragged -> {
                ActiveAction.Dragging draggingAction = new ActiveAction.Dragging(
                        dragged.srcList(),
                        dragged.srcPack(), dragged.packs()
                );

                // todo need to do more than check depth
                if (dragged.srcList().type().available() || dragged.srcList().depth() > 0) {
                    yield state.withAction(draggingAction);
                }

                PackListState targetList = state.getList(dragged.srcList());
                if (targetList == null || !targetList.query().hasQuery()) {
                    yield state.withAction(draggingAction);
                }

                yield state.withEnabled(
                        targetList.withQuery(Query.empty(), state.profiles(), state.devMode()),
                        draggingAction.target()
                ).withAction(draggingAction);
            }
            case PackListMutation.Dropped(PackListKey srcList, PackListKey destination, int index) -> {
                ActiveAction.Dragging dragging = state.dragging();
                if (dragging == null) yield state;

                PackedPacksState newState = state.withAction(null);
                if (destination == null) yield newState;

                PackListState targetList = newState.getList(destination);
                if (targetList == null) yield newState;

                if (!canDrop(srcList, dragging.srcPack(), dragging.packs(), destination, targetList, index, state.profiles())) {
                    yield newState;
                }

                int position = clampIndex(targetList, getAbsoluteIndex(targetList, index), state.profiles());
                if (srcList.equals(destination)) {
                    List<PackNode> packs = new ObjectArrayList<>(dragging.packs().size());
                    CollectionUtils.addIf(packs, dragging.packs(), p -> canDrag(srcList, p, state.profiles()));

                    if (!packs.isEmpty()) {
                        yield reduce(newState, new PackListMutation.Moved(destination, dragging.srcPack(), packs, position));
                    }
                } else if (canTransfer(srcList, dragging.srcPack(), state.profiles())) {
                    List<PackNode> packs = new ObjectArrayList<>(dragging.packs().size());
                    CollectionUtils.addIf(packs, dragging.packs(), p -> canTransfer(srcList, p, state.profiles()));

                    if (!packs.isEmpty()) {
                        yield reduceList(newState, switch (destination.type()) {
                            case AVAILABLE ->
                                    new PackListMutation.Disabled(destination, dragging.srcPack(), packs.reversed());
                            case ENABLED ->
                                    new PackListMutation.Enabled(destination, dragging.srcPack(), packs.reversed(), position);
                        });
                    }
                }
                yield newState;
            }
            case PackListMutation.RenameModalOpened opened ->
                    state.withAction(new ActiveAction.RenamingPack(opened.srcList(), opened.pack(), false));
            case PackListMutation.RenameModalClosed ignored -> state.withAction(null);
            case PackListMutation.AliasesModalOpened opened ->
                    state.withAction(new ActiveAction.EditingAliases(opened.srcList(), opened.srcPack(), opened.aliases()));
            case PackListMutation.AliasesModalClosed ignored -> state.withAction(null);
            case PackListMutation.RequirementOverridden overridden -> {
                Profile selectedProfile = state.profiles().selectedProfile();
                if (selectedProfile == null) yield state;

                for (PackNode entry : overridden.packs()) {
                    // just pretend its immutable
                    //noinspection DataFlowIssue
                    selectedProfile = selectedProfile.withRequiredOverride(overridden.required(), entry.id());
                }

                PackedPacksState newState = state.withProfiles(state.profiles().withSelected(selectedProfile));

                if (Boolean.TRUE.equals(overridden.required()) && overridden.srcList().type().available()) {
                    yield reduceList(newState, new PackListMutation.Enabled(
                            overridden.srcList(),
                            overridden.srcPack(),
                            overridden.packs(),
                            0
                    ));
                }

                yield newState;
            }
            case PackListMutation.PositionOverridden overridden -> {
                Profile selectedProfile = state.profiles().selectedProfile();
                if (selectedProfile == null) yield state;

                for (PackNode entry : overridden.packs()) {
                    // just pretend its immutable
                    //noinspection DataFlowIssue
                    selectedProfile = selectedProfile.withPositionOverride(overridden.position(), entry.id());
                }

                yield state.withProfiles(state.profiles().withSelected(selectedProfile));
            }
            case PackListMutation.VisibilityOverridden overridden -> {
                Profile selectedProfile = state.profiles().selectedProfile();
                if (selectedProfile == null) yield state;

                for (PackNode entry : overridden.packs()) {
                    // just pretend its immutable
                    //noinspection DataFlowIssue
                    selectedProfile = selectedProfile.withHiddenOverride(overridden.hidden(), entry.id());
                }

                yield state.withProfiles(state.profiles().withSelected(selectedProfile));
            }
            case PackListMutation.OverridesRemoved overridesRemoved -> {
                Profile selectedProfile = state.profiles().selectedProfile();
                if (selectedProfile == null) yield state;

                for (PackNode entry : overridesRemoved.packs()) {
                    // just pretend its immutable
                    //noinspection DataFlowIssue
                    selectedProfile = selectedProfile
                            .withRequiredOverride(null, entry.id())
                            .withPositionOverride(null, entry.id())
                            .withHiddenOverride(false, entry.id());
                }

                yield state.withProfiles(state.profiles().withSelected(selectedProfile));
            }
        };
    }

    private static PackedPacksState reduceProfile(PackedPacksState state, ProfileMutation mutation) {
        return switch (mutation) {
            case ProfileMutation.Selected selected -> {
                ProfilesState profiles = state.profiles();
                PackSelection packs = selected.packs();
                yield state.withProfiles(profiles.withSelected(selected.profile()))
                        .withPackLists(
                                state.available().withPacks(packs.disabled(), profiles, state.devMode()),
                                state.enabled().withPacks(packs.enabled(), profiles, state.devMode())
                        );
            }
            case ProfileMutation.Deleted delete -> {
                List<Profile> newProfileList = new ObjectArrayList<>(state.profiles().profiles());
                newProfileList.remove(delete.profile());
                yield state.withProfiles(state.profiles().withProfiles(newProfileList));
            }
            case ProfileMutation.DeletedAndReset delete -> {
                List<Profile> newProfileList = new ObjectArrayList<>(state.profiles().profiles());
                newProfileList.remove(delete.profile());

                ProfilesState newProfiles = state.profiles().withProfiles(newProfileList).withSelected(null);
                yield state.withProfiles(newProfiles).withPackLists(
                        state.available().withPacks(delete.packs().disabled(), newProfiles, state.devMode()),
                        state.enabled().withPacks(delete.packs().enabled(), newProfiles, state.devMode())
                );
            }
            case ProfileMutation.Added added -> {
                List<Profile> newProfileList = new ObjectArrayList<>(state.profiles().profiles());
                Profile newProfile = added.profile();
                newProfileList.add(newProfile);
                yield reduceProfile(
                        state.withProfiles(state.profiles().withProfiles(newProfileList)),
                        new ProfileMutation.Selected(
                                newProfile,
                                new PackSelection(state.available().packs(), state.enabled().packs())
                        )
                );
            }
            case ProfileMutation.DefaultRemoved() -> state.withProfiles(state.profiles().withDefault(null));
            case ProfileMutation.DefaultChanged(Profile profile, @Nullable PackSelection packs) ->
                    Objects.equals(profile, state.profiles().selectedProfile()) || packs == null
                            ? state.withProfiles(state.profiles().withDefault(profile))
                            : reduceProfile(
                            state.withProfiles(state.profiles().withDefault(profile)),
                            new ProfileMutation.Selected(profile, packs)
                    );
            case ProfileMutation.LockToggled(Profile profile) ->
                    state.withProfiles(updateProfileState(state.profiles(), profile.withLocked(!profile.isLocked())));
            case ProfileMutation.Renamed(Profile profile, String name) ->
                    state.withProfiles(updateProfileState(state.profiles(), profile.withName(name))
                            .withRenaming(state.profiles().renaming()));
            case ProfileMutation.RenamingToggled() ->
                    state.withProfiles(state.profiles().withRenaming(!state.profiles().renaming()));
        };
    }

    // just pretend profile is immutable, at some point it will be (maybe)
    private static ProfilesState updateProfileState(ProfilesState state, Profile newProfile) {
        Profile selectedProfile = state.selectedProfile();
        if (selectedProfile != null && newProfile.getId().equals(selectedProfile.getId())) {
            selectedProfile = newProfile;
        }

        Profile defaultProfile = state.selectedProfile();
        if (defaultProfile != null && newProfile.getId().equals(defaultProfile.getId())) {
            defaultProfile = newProfile;
        }

        List<Profile> newProfileList = new ObjectArrayList<>(state.profiles().size());
        for (Profile profile : state.profiles()) {
            if (newProfile.getId().equals(profile.getId())) {
                newProfileList.add(newProfile);
            } else {
                newProfileList.add(profile);
            }
        }

        return state
                .withProfiles(newProfileList)
                .withSelected(selectedProfile)
                .withDefault(defaultProfile);
    }

    private Reducer() {
    }
}
