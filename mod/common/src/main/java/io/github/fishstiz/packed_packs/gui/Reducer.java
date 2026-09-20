package io.github.fishstiz.packed_packs.gui;

import com.google.common.primitives.Ints;
import io.github.fishstiz.fidgetz.v0.utils.CollectionUtils;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.gui.actions.mutations.Mutation;
import io.github.fishstiz.packed_packs.gui.states.PackListKey;
import io.github.fishstiz.packed_packs.gui.states.PackListType;
import io.github.fishstiz.packed_packs.util.PackListComputedUtils;
import io.github.fishstiz.packed_packs.gui.states.Query;
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

import static io.github.fishstiz.packed_packs.util.PackListComputedUtils.*;

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
                        state.lastTarget().head(),
                        null,
                        state.devMode()
                );
            }
            case Mutation.DevModeToggled() -> {
                PackedPacksState newState = state.withDevMode(!state.devMode());

                yield newState.withPackLists(
                        refreshList(newState.available(), newState.profiles(), newState.devMode()),
                        refreshList(newState.enabled(), newState.profiles(), newState.devMode())
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

    private static PackListState refreshList(PackListState state, ProfileSelection profiles, boolean devMode) {
        state = state.withQuery(state.query(), profiles, devMode);

        PackListState currentParent = state;
        PackListState currentFolder = state.folder();
        for (boolean head = true; currentFolder != null; head = false) {
            currentParent = currentParent.withFolder(
                    currentFolder.withQuery(currentFolder.query(), profiles, devMode),
                    profiles,
                    devMode
            );

            if (head) state = currentParent;
            currentParent = currentParent.folder();
            currentFolder = currentParent == null ? null : currentParent.folder();
        }

        return state;
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
        PackListState newFolder = reduceList(state.folder(), depth + 1, mutation, profiles, devMode);
        return newFolder == state.folder() ? state : state.withFolder(newFolder, profiles, devMode);
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
            case PackListMutation.SelectedMultiple selected -> state.withSelection(selected.packs());
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
            case PackListMutation.FolderOpened opened -> state.withFolder(
                    PackListState.folder(opened.pack(), opened.locked(), state.query())
                            .withPacks(opened.children(), profiles, devMode),
                    profiles,
                    devMode
            );
            case PackListMutation.FolderClosed ignored -> state.withFolder(null, profiles, devMode);
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
            indexFn = PackListComputedUtils::getMoveUpIndex;
        } else {
            sorted = sortByOrderOf(state.visiblePacks(), payload).reversed();
            indexFn = PackListComputedUtils::getMoveDownIndex;
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

    private static PackListState updateHeadList(
            PackListState headList,
            int depth,
            PackListState state,
            ProfileSelection profiles,
            boolean devMode
    ) {
        if (depth == 0) {
            return state;
        }
        if (headList.folder() == null) {
            return headList;
        }
        return headList.withFolder(
                updateHeadList(headList.folder(), depth - 1, state, profiles, devMode),
                profiles,
                devMode
        );
    }

    private static PackedPacksState updateHeadList(
            PackedPacksState state,
            PackListState head,
            PackListType type,
            PackListKey actionSrc
    ) {
        return switch (type) {
            case AVAILABLE -> state.withAvailable(head, actionSrc);
            case ENABLED -> state.withEnabled(head, actionSrc);
        };
    }

    private static PackedPacksState transfer(PackedPacksState state, PackListMutation.Transfer transfer) {
        PackListKey destKey = transfer.srcList().type().available()
                ? PackListKey.enabled()
                : state.getTailKey(PackListType.AVAILABLE);

        PackListState destState = Objects.requireNonNullElse(state.getList(destKey), PackListState.empty());
        List<PackNode> newDestPacks = new ObjectArrayList<>(destState.packs());
        List<PackNode> newDestSelection = new ObjectArrayList<>(transfer.packs().size());

        PackNode srcPack = transfer.srcPack();
        PackListState srcState = Objects.requireNonNullElse(state.getList(transfer.srcList()), PackListState.empty());
        List<PackNode> newSrcPacks = new ObjectArrayList<>(srcState.packs());

        for (PackNode pack : transfer.packs()) {
            if (newSrcPacks.remove(pack)) {
                newDestPacks.add(transfer.index(), pack);
                if (srcPack == null || !pack.id().equals(srcPack.id())) {
                    newDestSelection.add(pack);
                }
            }
        }

        if (srcPack != null) {
            newDestSelection.add(srcPack);
        }

        SequencedCollection<PackNode> newSrcSelection = srcPack != null && srcState.selectedPacks().contains(srcPack)
                ? srcState.selectedPacks()
                : Collections.emptyList();

        PackListState srcHeadList = state.getHeadList(transfer.srcList().type());
        PackListState newSrcState = srcState == PackListState.empty() ? srcHeadList : updateHeadList(
                srcHeadList,
                transfer.srcList().depth(),
                srcState.with(newSrcPacks, newSrcSelection, state.profiles(), state.devMode()),
                state.profiles(),
                state.devMode()
        );

        PackListState destHeadList = state.getHeadList(destKey.type());
        PackListState newDestState = destState == PackListState.empty() ? destHeadList : updateHeadList(
                destHeadList,
                destKey.depth(),
                destState.with(newDestPacks, newDestSelection, state.profiles(), state.devMode()),
                state.profiles(),
                state.devMode()
        );

        PackListState disabled;
        PackListState enabled;

        if (transfer.srcList().type().available()) {
            disabled = newSrcState;
            enabled = newDestState;
        } else {
            disabled = newDestState;
            enabled = newSrcState;
        }

        return state.withPackLists(disabled, enabled, transfer.srcList().head());
    }

    private static PackedPacksState reduceList(PackedPacksState state, PackListMutation.Cross mutation) {
        return switch (mutation) {
            case PackListMutation.Enabled enabled -> {
                if (enabled.srcList().type().enabled()) {
                    yield state;
                }

                PackedPacksState newState = transfer(state, enabled);
                if (newState.enabled().folder() == null) {
                    yield newState;
                }

                yield newState.withEnabled(
                        newState.enabled().withFolder(null, state.profiles(), state.devMode()),
                        newState.lastTarget()
                );
            }
            case PackListMutation.Disabled disabled -> transfer(
                    state.enabled().folder() == null ? state : state.withEnabled(
                            state.enabled().withFolder(null, state.profiles(), state.devMode()),
                            state.lastTarget()
                    ),
                    disabled.srcList().type().enabled()
                            ? disabled
                            : new PackListMutation.Disabled(PackListKey.enabled(), null, disabled.packs())
            );
            case PackListMutation.Dragged dragged -> {
                PackListState srcListState = state.getList(dragged.srcList());

                ActiveAction.Dragging draggingAction = new ActiveAction.Dragging(
                        dragged.srcList(),
                        srcListState != null && srcListState.module(),
                        dragged.srcPack(),
                        dragged.packs()
                );

                PackListKey enabledTailKey = state.getTailKey(PackListType.ENABLED);
                PackListState enabledListState = Objects.requireNonNull(state.getList(enabledTailKey));

                if (!enabledListState.query().hasQuery()) {
                    yield state.withAction(draggingAction);
                }

                PackListState newEnabledState = updateHeadList(
                        state.enabled(),
                        enabledTailKey.depth(),
                        enabledListState.withQuery(Query.empty(), state.profiles(), state.devMode()),
                        state.profiles(),
                        state.devMode()
                );

                yield state.withEnabled(newEnabledState, draggingAction.src()).withAction(draggingAction);
            }
            case PackListMutation.Dropped(PackListKey srcList, PackListKey dest, int index) -> {
                ActiveAction.Dragging dragging = state.dragging();
                if (dragging == null) yield state;

                PackedPacksState newState = state.withAction(null);
                if (dest == null) yield newState;

                PackListState destListState = newState.getList(dest);
                if (destListState == null) yield newState;

                PackListState srcListState = newState.getList(srcList);
                if (srcListState == null) yield newState;

                if (!canDrop(dragging, dest, destListState, index, state.profiles())) {
                    yield newState;
                }

                int position = clampIndex(destListState, getAbsoluteIndex(destListState, index), state.profiles());
                if (srcList.equals(dest)) {
                    List<PackNode> packs = new ObjectArrayList<>(dragging.packs().size());
                    CollectionUtils.addIf(packs, dragging.packs(), p -> canDrag(srcList, srcListState.module(), p, state.profiles()));

                    if (!packs.isEmpty()) {
                        yield reduce(newState, new PackListMutation.Moved(dest, dragging.srcPack(), packs, position));
                    }
                } else if (canTransfer(srcList, srcListState.module(), dragging.srcPack(), state.profiles())) {
                    List<PackNode> packs = new ObjectArrayList<>(dragging.packs().size());
                    CollectionUtils.addIf(
                            packs,
                            dragging.packs(),
                            p -> canTransfer(srcList, false, p, state.profiles())
                    );

                    if (!packs.isEmpty()) {
                        yield reduceList(newState, switch (dest.type()) {
                            case AVAILABLE ->
                                    new PackListMutation.Disabled(srcList, dragging.srcPack(), packs.reversed());
                            case ENABLED ->
                                    new PackListMutation.Enabled(srcList, dragging.srcPack(), packs.reversed(), position);
                        });
                    }
                }
                yield newState;
            }
            case PackListMutation.ModuleUpdated updated -> {
                PackListState srcState = state.getList(updated.srcList());
                if (srcState == null) {
                    yield state;
                }

                if (srcState.module() == updated.module()) {
                    yield state;
                }

                PackListState newSrcState = srcState.withModule(updated.module(), state.profiles(), state.devMode());
                PackedPacksState newState = updateHeadList(
                        state,
                        updateHeadList(
                                state.getHeadList(updated.srcList().type()),
                                updated.srcList().depth(),
                                newSrcState,
                                state.profiles(),
                                state.devMode()
                        ),
                        updated.srcList().type(),
                        updated.srcList()
                );

                if (!newSrcState.module() || newSrcState.parent() == null || updated.srcList().type().enabled()) {
                    yield newState;
                }

                SequencedSet<PackNode> newEnabledPacks = new ObjectLinkedOpenHashSet<>(state.enabled().packs());
                List<PackNode> removedPacks = new ObjectArrayList<>();

                newSrcState.parent().visitNodes(node -> {
                    if (newEnabledPacks.remove(node)) {
                        removedPacks.add(node);
                    }
                });

                if (removedPacks.isEmpty()) {
                    yield newState;
                }

                SequencedSet<PackNode> newChildren = new ObjectLinkedOpenHashSet<>(newSrcState.packs());
                newChildren.addAll(removedPacks);

                yield updateHeadList(
                        updateHeadList(
                                newState,
                                updateHeadList(
                                        newState.getHeadList(updated.srcList().type()),
                                        updated.srcList().depth(),
                                        newSrcState.withPacks(List.copyOf(newChildren), state.profiles(), state.devMode()),
                                        newState.profiles(),
                                        newState.devMode()
                                ),
                                updated.srcList().type(),
                                updated.srcList()
                        ),
                        newState.enabled().withPacks(List.copyOf(newEnabledPacks), state.profiles(), state.devMode()),
                        PackListType.ENABLED,
                        updated.srcList()
                );
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
                PackedPacksState newState = state.withProfiles(profiles.withSelected(selected.profile()));

                yield newState.withPackLists(
                        state.available().withPacks(packs.disabled(), profiles, state.devMode())
                                .withFolder(null, newState.profiles(), newState.devMode()),
                        state.enabled().withPacks(packs.enabled(), profiles, state.devMode())
                                .withFolder(null, newState.profiles(), newState.devMode())
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
                        state.available().withPacks(delete.packs().disabled(), newProfiles, state.devMode())
                                .withFolder(null, newProfiles, state.devMode()),
                        state.enabled().withPacks(delete.packs().enabled(), newProfiles, state.devMode())
                                .withFolder(null, newProfiles, state.devMode())
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
            case ProfileMutation.DefaultChanged(Profile profile, @Nullable PackSelection packs) -> {
                PackedPacksState newState = state.withProfiles(
                        updateProfileState(state.profiles().withDefault(profile), profile)
                );

                if (Objects.equals(profile, state.profiles().selectedProfile()) || packs == null) {
                    yield newState;
                }

                yield reduceProfile(newState, new ProfileMutation.Selected(profile, packs));
            }
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
        if (selectedProfile != null && (selectedProfile == newProfile || newProfile.getId().equals(selectedProfile.getId()))) {
            selectedProfile = newProfile;
        }

        Profile defaultProfile = state.defaultProfile();
        if (defaultProfile != null && (defaultProfile == newProfile || newProfile.getId().equals(defaultProfile.getId()))) {
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
