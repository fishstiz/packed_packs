package io.github.fishstiz.packed_packs.gui.reducers;

import com.google.common.primitives.Ints;
import io.github.fishstiz.fidgetz.util.lang.CollectionsUtil;
import io.github.fishstiz.fidgetz.util.lang.ToIntTriFunction;
import io.github.fishstiz.packed_packs.config.PackOptions;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.gui.Intent;
import io.github.fishstiz.packed_packs.gui.components.pack.Query;
import io.github.fishstiz.packed_packs.gui.intents.PackListIntent;
import io.github.fishstiz.packed_packs.gui.intents.ProfileIntent;
import io.github.fishstiz.packed_packs.gui.model.PackListKey;
import io.github.fishstiz.packed_packs.gui.model.PackListUtils;
import io.github.fishstiz.packed_packs.gui.states.*;
import io.github.fishstiz.packed_packs.impl.context.PackEntryContext;
import io.github.fishstiz.packed_packs.pack.PackGroup;
import io.github.fishstiz.packed_packs.util.PackUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.Nullable;

import java.util.*;

import static io.github.fishstiz.packed_packs.gui.model.PackListUtils.*;

public class PackedPacksReducer {
    public PackedPacksState reduce(PackedPacksState state, Intent intent) {
        return switch (intent) {
            case ProfileIntent profileIntent -> this.reduceProfile(state, profileIntent);
            case PackListIntent packListIntent -> switch (packListIntent) {
                case PackListIntent.ScreenScoped screen -> this.reduce(state, screen);
                case PackListIntent.ListScoped list -> switch (list.target().type()) {
                    case AVAILABLE -> {
                        PackListState newAvailable = this.reduceList(state.available(), 0, list, state.profiles().options());
                        yield newAvailable != state.available() ? state.withAvailable(newAvailable, packListIntent.target()) : state;
                    }
                    case ENABLED -> {
                        PackListState newEnabled = this.reduceList(state.enabled(), 0, list, state.profiles().options());
                        yield newEnabled != state.enabled() ? state.withEnabled(newEnabled, packListIntent.target()) : state;
                    }
                };
            };
            default -> state;
        };
    }

    private PackedPacksState reduce(PackedPacksState state, PackListIntent.ScreenScoped intent) {
        return switch (intent) {
            case PackListIntent.Reset reset -> {
                PackListState available = state.available();
                PackListState enabled = state.enabled();
                PackOptions options = state.profiles().options();
                yield new PackedPacksState(
                        available.with(reset.packs().unselected(), Collections.emptyList(), available.query().withSearch(""), options),
                        enabled.with(reset.packs().selected(), Collections.emptyList(), enabled.query().withSearch(""), options),
                        state.profiles(),
                        reset.target().root(),
                        null
                );
            }
            case PackListIntent.Enable enable -> {
                List<Pack> newAvailable = new ObjectArrayList<>(state.available().packs());
                List<Pack> newEnabled = new ObjectArrayList<>(state.enabled().packs());
                List<Pack> newEnabledSelection = new ObjectArrayList<>(enable.payload().size());

                for (Pack pack : enable.payload()) {
                    if (newAvailable.remove(pack)) {
                        newEnabled.add(enable.index(), pack);
                        if (!pack.equals(enable.ctx().pack())) {
                            newEnabledSelection.add(pack);
                        }
                    }
                }
                newEnabledSelection.add(enable.ctx().pack());
                yield state.withPackLists(
                        state.available().withPacks(newAvailable, state.profiles().options()),
                        state.enabled().with(newEnabled, newEnabledSelection, state.profiles().options()),
                        PackListKey.enabled()
                );
            }
            case PackListIntent.Disable disable -> {
                List<Pack> newAvailable = new ObjectArrayList<>(state.available().packs());
                List<Pack> newEnabled = new ObjectArrayList<>(state.enabled().packs());
                List<Pack> newAvailableSelection = new ObjectArrayList<>(disable.payload().size());
                for (Pack pack : disable.payload()) {
                    if (newEnabled.remove(pack)) {
                        newAvailable.add(pack);
                        if (!disable.ctx().pack().equals(pack)) newAvailableSelection.add(pack);
                    }
                }
                newAvailableSelection.add(disable.ctx().pack());
                yield state.withPackLists(
                        state.available().with(newAvailable, newAvailableSelection, state.profiles().options()),
                        state.enabled().withPacks(newEnabled, state.profiles().options()),
                        PackListKey.available()
                );
            }
            case PackListIntent.Require require ->
                    Boolean.TRUE.equals(require.required()) && require.target().type().available()
                            ? this.reduce(state, new PackListIntent.Enable(require.target(), require.ctx(), require.payload()))
                            : state;
            case PackListIntent.Rename rename -> rename.status().success() ? state.withAction(null) : state;
            case PackListIntent.Drag drag -> {
                ActiveAction.Dragging draggingAction = new ActiveAction.Dragging(drag.target(), drag.ctx(), drag.payload());
                if (drag.target().type().available() || drag.target().depth() > 0) {
                    yield state.withAction(draggingAction);
                }

                PackListState targetList = state.targetList(drag.target());
                if (targetList == null || !targetList.query().hasQuery()) {
                    yield state.withAction(draggingAction);
                }

                yield state.withEnabled(targetList.withQuery(Query.empty(), state.profiles().options()), draggingAction.target())
                        .withAction(draggingAction);
            }
            case PackListIntent.OpenRename openRename ->
                    state.withAction(new ActiveAction.RenamingPack(openRename.target(), openRename.ctx()));
            case PackListIntent.CloseRename ignored -> state.withAction(null);
            case PackListIntent.EditAliases openAliases ->
                    state.withAction(new ActiveAction.EditingAliases(openAliases.target(), openAliases.ctx(), openAliases.aliases()));
            case PackListIntent.CloseAliases ignored -> state.withAction(null);
            case PackListIntent.Drop(
                    PackListKey target,
                    PackEntryContext ctx,
                    SequencedCollection<Pack> payload,
                    @Nullable PackListKey destination,
                    int index
            ) -> {
                PackedPacksState newState = state.withAction(null);
                if (destination == null) yield newState;

                PackListState targetList = newState.targetList(destination);
                if (targetList == null) yield newState;

                PackOptions options = newState.profiles().options();
                if (!canDrop(target, ctx.pack(), payload, destination, targetList, index, options)) {
                    yield newState;
                }

                int position = clampIndex(targetList, getAbsoluteIndex(targetList, index), options);
                if (target.equals(destination)) {
                    List<Pack> packs = new ObjectArrayList<>(payload.size());
                    CollectionsUtil.addIf(packs, payload, p -> canDrag(target, p, options));
                    if (!packs.isEmpty())
                        yield this.reduce(newState, new PackListIntent.Move(destination, ctx, packs, position));
                } else if (canTransfer(target, ctx.pack(), options)) {
                    List<Pack> packs = new ObjectArrayList<>(payload.size());
                    CollectionsUtil.addIf(packs, payload, p -> canTransfer(target, p, options));
                    if (!packs.isEmpty()) {
                        yield this.reduce(newState, switch (destination.type()) {
                            case AVAILABLE -> new PackListIntent.Disable(destination, ctx, packs);
                            case ENABLED -> new PackListIntent.Enable(destination, ctx, packs, position);
                        });
                    }
                }
                yield newState;
            }
        };
    }

    private PackListState reduceList(PackListState state, int depth, PackListIntent.ListScoped intent, PackOptions options) {
        if (depth == intent.target().depth()) {
            return this.reduceList(state, intent, options);
        }
        if (state.folder() == null || intent.target().depth() < 0) {
            return state;
        }
        PackListState newContents = this.reduceList(state.folder().contents(), depth + 1, intent, options);
        return newContents == state.folder().contents() ? state : state.withFolder(state.folder().withContents(newContents));
    }

    private PackListState reduceList(PackListState state, PackListIntent.ListScoped intent, PackOptions options) {
        return switch (intent) {
            case PackListIntent.Search search -> state.withQuery(state.query().withSearch(search.query()), options);
            case PackListIntent.Sort sort -> state.withQuery(state.query().withSort(sort.sort()), options);
            case PackListIntent.HideIncompatible hideIncompatible ->
                    state.withQuery(state.query().withHideIncompatible(hideIncompatible.hide()), options);
            case PackListIntent.Select select -> {
                Pack pack = select.ctx().pack();
                if (!state.visiblePacks().contains(pack)) yield state;
                List<Pack> newSelection = new ObjectArrayList<>(state.selectedPacks());
                newSelection.remove(pack);
                newSelection.add(pack);
                yield state.withSelection(newSelection);
            }
            case PackListIntent.SelectExclusive selectExclusive -> {
                Pack pack = selectExclusive.ctx().pack();
                if (!state.visiblePacks().contains(pack)) yield state;
                yield state.withSelection(List.of(pack));
            }
            case PackListIntent.SelectToggle selectToggle -> {
                Pack pack = selectToggle.ctx().pack();
                if (!state.visiblePacks().contains(pack)) yield state;
                List<Pack> newSelection = new ObjectArrayList<>(state.selectedPacks());
                if (!newSelection.remove(pack)) newSelection.add(pack);
                yield state.withSelection(newSelection);
            }
            case PackListIntent.SelectRange selectRange -> {
                if (state.selectedPacks().isEmpty()) {
                    yield state.withSelection(List.of(selectRange.ctx().pack()));
                }

                Pack anchor = state.selectedPacks().getLast();
                int anchorIndex = state.visiblePacks().indexOf(anchor);
                int targetIndex = state.visiblePacks().indexOf(selectRange.ctx().pack());
                int[] indices = indicesOf(state.visiblePacks(), state.selectedPacks());
                Arrays.sort(indices);

                if (!(Ints.contains(indices, -1) || hasGap(indices, true)) && indices.length > 0) {
                    if (indices[0] == anchorIndex) {
                        anchor = state.visiblePacks().get(indices[indices.length - 1]);
                    } else if (indices[indices.length - 1] == anchorIndex) {
                        anchor = state.visiblePacks().get(indices[0]);
                    }
                }
                List<Pack> newSelection = new ObjectArrayList<>(state.selectedPacks());
                int start = state.visiblePacks().indexOf(anchor);
                if (targetIndex != -1 && start != -1) {
                    newSelection.clear();
                    for (int i = Math.min(targetIndex, start); i <= Math.max(targetIndex, start); i++) {
                        Pack selected = state.visiblePacks().get(i);
                        if (selected != null && selected != selectRange.ctx().pack() && state.visiblePacks().contains(selected)) {
                            newSelection.remove(selected);
                            newSelection.addLast(selected);
                        }
                    }
                }

                newSelection.add(selectRange.ctx().pack());
                yield state.withSelection(newSelection);
            }
            case PackListIntent.SelectAll selectAll -> {
                ObjectLinkedOpenHashSet<Pack> newSelection = new ObjectLinkedOpenHashSet<>(state.visiblePacks());
                if (selectAll.ctx() != null) newSelection.addAndMoveToLast(selectAll.ctx().pack());
                yield state.withSelection(newSelection);
            }
            case PackListIntent.Move move -> {
                List<Pack> ordered = sortByOrderOf(state.visiblePacks(), move.payload());
                List<Pack> newPacks = new ObjectArrayList<>(state.packs());
                int to = move.index();
                for (Pack pack : ordered) {
                    int previous = newPacks.indexOf(pack);
                    int target = previous != -1 && previous < to ? to - 1 : to;
                    newPacks.remove(pack);
                    newPacks.add(Math.clamp(target, 0, newPacks.size()), pack);
                }
                yield newPacks.equals(state.packs()) ? state : state.withPacks(newPacks, options);
            }
            case PackListIntent.MoveUp moveUp -> {
                List<Pack> newPacks = moveUp.payload().isEmpty() ? state.packs() : this.movePacks(state, moveUp.payload(), true, options);
                yield state.withPacksAndSelectedLast(newPacks, moveUp.ctx().pack(), options);
            }
            case PackListIntent.MoveDown moveDown -> {
                List<Pack> newPacks = moveDown.payload().isEmpty() ? state.packs() : this.movePacks(state, moveDown.payload(), false, options);
                yield state.withPacksAndSelectedLast(newPacks, moveDown.ctx().pack(), options);
            }
            case PackListIntent.OpenFolder open ->
                    state.withFolder(new PackListState.Folder(open.folderPack(), new PackListState(open.contents())));
            case PackListIntent.CloseFolder ignored -> state.withFolder(null);
            case PackListIntent.Delete delete -> {
                if (!delete.status().success()) yield state;
                List<Pack> newPacks = new ObjectArrayList<>(state.packs());
                yield newPacks.remove(delete.ctx().pack()) ? state.withPacks(newPacks, options) : state;
            }
            case PackListIntent.Hide ignored -> state;
            case PackListIntent.FixPosition ignored -> state;
            case PackListIntent.RemoveOverrides ignored -> state;
        };
    }

    private List<Pack> movePacks(PackListState state, SequencedCollection<Pack> payload, boolean up, PackOptions options) {
        Set<Pack> validPacks = new ObjectOpenHashSet<>(state.packs());
        List<Pack> newPacks = new ObjectArrayList<>(state.packs());
        List<Pack> sorted;
        ToIntTriFunction<List<Pack>, Pack, PackOptions> indexFn;

        if (up) {
            sorted = sortByOrderOf(state.visiblePacks(), payload);
            indexFn = PackListUtils::getMoveUpIndex;
        } else {
            sorted = sortByOrderOf(state.visiblePacks(), payload).reversed();
            indexFn = PackListUtils::getMoveDownIndex;
        }

        for (int i = 0; i < sorted.size(); i++) {
            Pack pack = sorted.get(i);
            if (!validPacks.contains(pack)) continue;
            int targetIndex = indexFn.applyAsInt(newPacks, pack, options);
            if (targetIndex > -1 && targetIndex < newPacks.size() && !options.isFixed(pack)) {
                newPacks.remove(pack);
                newPacks.add(targetIndex, pack);
            } else if (i == 0) {
                return state.packs();
            }
        }

        return newPacks;
    }

    private PackedPacksState reduceProfile(PackedPacksState state, ProfileIntent intent) {
        return switch (intent) {
            case ProfileIntent.Select select ->
                    this.computeProfilePacks(state.withProfiles(state.profiles().withSelected(select.profile())));
            case ProfileIntent.SelectNone(PackGroup packs) -> {
                ProfilesState newProfiles = state.profiles().withSelected(null);
                PackListState newAvailable = state.available();
                PackListState newEnabled = state.enabled();
                PackOptions options = newProfiles.options();
                yield state.with(
                        newAvailable.with(packs.unselected(), Collections.emptyList(), newAvailable.query().withSearch(""), options),
                        newEnabled.with(packs.selected(), Collections.emptyList(), newEnabled.query().withSearch(""), options),
                        newProfiles
                );
            }
            case ProfileIntent.Delete delete -> {
                if (!delete.status().success()) yield state;
                List<Profile> newProfileList = new ObjectArrayList<>(state.profiles().profiles());
                newProfileList.remove(delete.profile());

                ProfilesState newProfiles = state.profiles().withProfiles(newProfileList);
                if (!Objects.equals(state.profiles().selectedProfile(), delete.profile())) {
                    yield state.withProfiles(newProfiles);
                }

                PackedPacksState newState = state.withProfiles(newProfiles.withSelected(null));
                yield delete.fallback() == null ? newState : this.reduceProfile(newState, new ProfileIntent.SelectNone(delete.fallback()));
            }
            case ProfileIntent.Add add -> {
                List<Profile> newList = new ObjectArrayList<>(state.profiles().profiles());
                newList.add(add.profile());
                yield this.reduceProfile(state.withProfiles(state.profiles().withProfiles(newList)), new ProfileIntent.Select(add.profile()));
            }
            case ProfileIntent.SetDefault(Profile profile) -> profile == null
                    ? state.withProfiles(state.profiles().withDefault(null))
                    : this.reduceProfile(state.withProfiles(state.profiles().withDefault(profile)), new ProfileIntent.Select(profile));
            case ProfileIntent.ToggleLock ignored -> state.withProfiles(new ProfilesState(
                    state.profiles().profiles(),
                    state.profiles().selectedProfile(),
                    state.profiles().defaultProfile(),
                    state.profiles().options()
            ));
            case ProfileIntent.Rename ignored -> state;
        };
    }

    private PackedPacksState computeProfilePacks(PackedPacksState state) {
        Profile profile = state.profiles().selectedProfile();
        PackOptions options = state.profiles().options();

        ObjectOpenHashSet<Pack> allPacks = new ObjectOpenHashSet<>(state.available().packs().size() + state.enabled().packs().size());
        allPacks.addAll(state.available().packs());
        allPacks.addAll(state.enabled().packs());

        List<Pack> selectedPacks;
        if (profile == null) {
            selectedPacks = state.enabled().packs();
        } else {
            List<String> profilePackIds = profile.getPackIds();
            selectedPacks = profilePackIds.isEmpty()
                    ? Collections.emptyList()
                    : CollectionsUtil.lookup(profilePackIds, CollectionsUtil.toMap(allPacks, Pack::getId));
        }

        PackGroup packLists = PackUtil.syncPackSelection(allPacks, state.available().packs(), selectedPacks, options);

        return state.withPackLists(
                state.available().with(packLists.unselected(), Collections.emptyList(), state.available().query().withSearch(""), options),
                state.enabled().with(packLists.selected(), Collections.emptyList(), state.enabled().query().withSearch(""), options)
        );
    }
}
