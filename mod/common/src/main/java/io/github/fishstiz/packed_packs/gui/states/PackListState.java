package io.github.fishstiz.packed_packs.gui.states;

import io.github.fishstiz.packed_packs.gui.components.SortOption;
import io.github.fishstiz.packed_packs.gui.components.SortOptions;
import io.github.fishstiz.packed_packs.pack.PackNode;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import org.jspecify.annotations.Nullable;

import java.util.*;

public record PackListState(
        PackNode.@Nullable Parent parent,
        boolean module,
        List<PackNode> packs,
        List<PackNode> visiblePacks,
        SequencedCollection<PackNode> selectedPacks,
        Query query,
        @Nullable PackListState folder
) {
    private static final PackListState EMPTY = new PackListState(
            null,
            false,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySortedSet(),
            Query.empty(),
            null
    );

    public static PackListState empty() {
        return EMPTY;
    }

    public static PackListState folder(PackNode.Parent parent, boolean module, Query query) {
        return new PackListState(
                parent,
                module,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptySortedSet(),
                module && !(query.sort() instanceof SortOption.Locked)
                        ? query.withSort(new SortOption.Locked(query.sort()))
                        : query,
                null
        );
    }

    public PackListState with(
            List<PackNode> newPacks,
            SequencedCollection<PackNode> newSelection,
            Query query,
            ProfileSelection profiles,
            boolean devMode
    ) {
        List<PackNode> newVisiblePacks = processQuery(newPacks, query, profiles, devMode);
        // use a SequencedSet for faster lookups as this is queried every frame for each visible item within view
        // to avoid refreshing the pack list entries on each selection change
        // ... which I realize may not actually be worth it now that I'm writing this out,
        // but it has always worked that way since the creation of this project
        SequencedSet<PackNode> newSelectedPacks = new ObjectLinkedOpenHashSet<>(newSelection.size());
        for (PackNode pack : newSelection) {
            if (newVisiblePacks.contains(pack)) newSelectedPacks.add(pack);
        }
        return new PackListState(
                parent,
                module,
                newPacks,
                newVisiblePacks,
                Collections.unmodifiableSequencedSet(newSelectedPacks),
                query,
                folder
        );
    }

    public PackListState with(
            List<PackNode> newPacks,
            SequencedCollection<PackNode> newSelection,
            ProfileSelection profiles,
            boolean devMode
    ) {
        return this.with(newPacks, newSelection, this.query, profiles, devMode);
    }

    public PackListState withPacks(List<PackNode> newPacks, ProfileSelection profiles, boolean devMode) {
        return this.with(newPacks, this.selectedPacks, profiles, devMode);
    }

    public PackListState withQuery(Query query, ProfileSelection profiles, boolean devMode) {
        return this.with(this.packs, this.selectedPacks, query, profiles, devMode);
    }

    public PackListState withSelection(SequencedCollection<PackNode> newSelection) {
        SequencedSet<PackNode> newSelectedPacks = new ObjectLinkedOpenHashSet<>(newSelection);
        newSelectedPacks.retainAll(this.visiblePacks);
        return new PackListState(
                parent,
                module,
                packs,
                visiblePacks,
                Collections.unmodifiableSequencedSet(newSelectedPacks),
                query,
                folder
        );
    }

    public PackListState withPacksAndSelectedLast(
            List<PackNode> newPacks,
            PackNode selectedLast,
            ProfileSelection profiles,
            boolean devMode
    ) {
        if (packs == newPacks) return this;

        if (!selectedPacks.contains(selectedLast)) {
            return with(newPacks, List.of(selectedLast), profiles, devMode);
        } else if (selectedPacks.getLast() != selectedLast) {
            ObjectLinkedOpenHashSet<PackNode> newSelection = new ObjectLinkedOpenHashSet<>(selectedPacks);
            newSelection.addAndMoveToLast(selectedLast);
            return with(newPacks, newSelection, profiles, devMode);
        }

        return withPacks(newPacks, profiles, devMode);
    }

    public PackListState withFolder(@Nullable PackListState newFolder, ProfileSelection profiles, boolean devMode) {
        PackListState newState = new PackListState(
                parent,
                module,
                packs,
                visiblePacks,
                selectedPacks,
                query,
                newFolder
        );

        if (folder == null || folder.query.equals(query)) {
            return newState;
        }

        Query newQuery = folder.query;
        if (query.sort() instanceof SortOption.Locked
            || folder.query.sort() instanceof SortOption.Locked
            || folder.query.sort() == SortOptions.NONE) {
            newQuery = newQuery.withSort(query.sort());
        }

        if (newQuery.equals(folder.query)) {
            return newState;
        }

        return newState.withQuery(newQuery, profiles, devMode);
    }

    public PackListState withModule(boolean newModule, ProfileSelection profiles, boolean devMode) {
        if (module == newModule) {
            return this;
        }
        if (newModule && parent == null) {
            throw new IllegalStateException("Module pack lists cannot have a null parent");
        }

        return resolveFolderQuery(profiles, devMode, new PackListState(
                parent,
                newModule,
                packs,
                visiblePacks,
                selectedPacks,
                query,
                newModule ? null : folder
        ));
    }

    private static PackListState resolveFolderQuery(ProfileSelection profiles, boolean devMode, PackListState folder) {
        Query query = folder.query;
        Query newQuery = folder.query;
        if (folder.module) {
            newQuery = query.sort() instanceof SortOption.Locked ? query : query.withSort(new SortOption.Locked(query.sort()));
        } else if (query.sort() instanceof SortOption.Locked(SortOption sort)) {
            newQuery = query.withSort(sort);
        }

        if (newQuery != query) {
            return folder.withQuery(newQuery, profiles, devMode);
        }

        return folder;
    }

    public boolean isFolderOpened() {
        return this.folder != null;
    }

    private static List<PackNode> processQuery(
            List<PackNode> sourcePacks,
            Query query,
            ProfileSelection profiles,
            boolean devMode
    ) {
        List<PackNode> filtered = new ObjectArrayList<>(sourcePacks.size());
        for (PackNode pack : sourcePacks) {
            if ((devMode || !profiles.isPackHidden(pack)) && query.test(pack)) {
                filtered.add(pack);
            }
        }
        if (query.sort() != null) {
            Comparator<PackNode> comparator = query.sort().comparator(filtered);
            if (comparator != null) {
                filtered.sort(comparator);
            }
        }
        return List.copyOf(filtered);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packs, visiblePacks, sequencedHashCode(selectedPacks), query, folder);
    }

    // take into account the sequence of selectedPacks for UI state management
    // selectedPacks is normally a SequencedSet which does not check the order in #equals
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        //noinspection DeconstructionCanBeUsed
        if (!(obj instanceof PackListState that)) return false;

        return module == that.module
               && Objects.equals(parent, that.parent)
               && Objects.equals(query, that.query)
               && Objects.equals(packs, that.packs)
               && Objects.equals(visiblePacks, that.visiblePacks)
               && sequencedEquals(selectedPacks, that.selectedPacks)
               && Objects.equals(folder, that.folder);
    }

    private static <E> int sequencedHashCode(SequencedCollection<E> sequencedCollection) {
        // copy of AbstractList#hashCode
        int selectionHash = 1;
        for (E obj : sequencedCollection) {
            selectionHash = 31 * selectionHash + (obj == null ? 0 : obj.hashCode());
        }
        return selectionHash;
    }

    private static <E> boolean sequencedEquals(SequencedCollection<E> a, SequencedCollection<E> b) {
        if (a == b) return true;
        if (a.size() != b.size()) return false;
        Iterator<E> itA = a.iterator();
        Iterator<E> itB = b.iterator();
        while (itA.hasNext()) {
            if (!Objects.equals(itA.next(), itB.next())) return false;
        }
        return true;
    }
}
