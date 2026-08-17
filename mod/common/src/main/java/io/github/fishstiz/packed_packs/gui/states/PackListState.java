package io.github.fishstiz.packed_packs.gui.states;

import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.gui2.models.ProfileSelection;
import io.github.fishstiz.packed_packs.gui.model.Query;
import io.github.fishstiz.packed_packs.gui2.models.PackEntry;
import io.github.fishstiz.packed_packs.util.Utils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import org.jspecify.annotations.Nullable;

import java.util.*;

public record PackListState(
        List<PackEntry> packs,
        List<PackEntry> visiblePacks,
        SequencedCollection<PackEntry> selectedPacks,
        // todo track loading entries
        Query query,
        @Nullable Folder folder
) {
    public record Folder(PackEntry.Parent pack, boolean locked, PackListState contents) {
        public Folder withContents(PackListState contents) {
            return new Folder(pack, locked, contents);
        }
    }

    private static final PackListState EMPTY = new PackListState(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySortedSet(),
            Query.empty(),
            null
    );

    public static PackListState empty() {
        return EMPTY;
    }

    public PackListState(List<PackEntry> packs) {
        this(packs, List.copyOf(packs), Collections.emptyList(), Query.empty(), null);
    }

    public PackListState with( // todo this should be done in reducer
            List<PackEntry> newPacks,
            SequencedCollection<PackEntry> newSelection,
            Query query,
            ProfileSelection profiles
    ) {
        List<PackEntry> newVisiblePacks = processQuery(newPacks, query, profiles);
        // use a SequencedSet for faster lookups as this is queried every frame for each visible item within view
        // to avoid refreshing the pack list entries on each selection change
        // ... which I realize may not actually be worth it now that I'm writing this out,
        // but it has always worked that way since the creation of this project
        SequencedSet<PackEntry> newSelectedPacks = new ObjectLinkedOpenHashSet<>(newSelection.size());
        for (PackEntry pack : newSelection) {
            if (newVisiblePacks.contains(pack)) newSelectedPacks.add(pack);
        }
        return new PackListState(newPacks, newVisiblePacks, Collections.unmodifiableSequencedSet(newSelectedPacks), query, null);
    }

    public PackListState with(
            List<PackEntry> newPacks,
            SequencedCollection<PackEntry> newSelection,
            ProfileSelection profiles
    ) {
        return this.with(newPacks, newSelection, this.query, profiles);
    }

    public PackListState withPacks(List<PackEntry> newPacks, ProfileSelection profiles) {
        return this.with(newPacks, this.selectedPacks, profiles);
    }

    public PackListState withQuery(Query query, ProfileSelection profiles) {
        return this.with(this.packs, this.selectedPacks, query, profiles);
    }

    public PackListState withSelection(SequencedCollection<PackEntry> newSelection) {
        SequencedSet<PackEntry> newSelectedPacks = new ObjectLinkedOpenHashSet<>(newSelection);
        newSelectedPacks.retainAll(this.visiblePacks);
        return new PackListState(this.packs, this.visiblePacks, Collections.unmodifiableSequencedSet(newSelectedPacks), this.query, this.folder);
    }

    public PackListState withPacksAndSelectedLast(List<PackEntry> newPacks, PackEntry selectedLast, ProfileSelection profiles) {
        if (this.packs() == newPacks) return this;

        if (!this.selectedPacks().contains(selectedLast)) {
            return this.with(newPacks, List.of(selectedLast), profiles);
        } else if (this.selectedPacks().getLast() != selectedLast) {
            ObjectLinkedOpenHashSet<PackEntry> newSelection = new ObjectLinkedOpenHashSet<>(this.selectedPacks());
            newSelection.addAndMoveToLast(selectedLast);
            return this.with(newPacks, newSelection, profiles);
        }

        return this.withPacks(newPacks, profiles);
    }

    public PackListState withFolder(PackListState.@Nullable Folder newFolder) {
        return new PackListState(this.packs, this.visiblePacks, this.selectedPacks, this.query, newFolder);
    }

    public boolean isFolderOpened() {
        return this.folder != null;
    }

    // todo this should be done in reducer
    private static List<PackEntry> processQuery(List<PackEntry> sourcePacks, Query query, ProfileSelection profiles) {
        List<PackEntry> filtered = new ObjectArrayList<>(sourcePacks.size());
        for (PackEntry pack : sourcePacks) {
            if ((Config.get().isDevMode() || !profiles.isPackHidden(pack)) && query.test(pack)) {
                filtered.add(pack);
            }
        }
        if (query.sort() != null) {
            filtered.sort(query.sort().comparator(filtered));
        }
        return List.copyOf(filtered);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.packs, this.visiblePacks, sequencedHashCode(this.selectedPacks), this.query, this.folder);
    }

    // take into account the sequence of selectedPacks for UI state management
    // selectedPacks is normally a SequencedSet which does not check the order in #equals
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        //noinspection DeconstructionCanBeUsed
        if (!(obj instanceof PackListState that)) return false;
        return Objects.equals(this.query, that.query) &&
               Objects.equals(this.folder, that.folder) &&
               Objects.equals(this.packs, that.packs) &&
               Objects.equals(this.visiblePacks, that.visiblePacks) &&
               sequencedEquals(this.selectedPacks, that.selectedPacks);
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
        return Utils.orderEquals(a, b);
    }
}
