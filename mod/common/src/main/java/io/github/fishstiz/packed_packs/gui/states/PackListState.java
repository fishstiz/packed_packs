package io.github.fishstiz.packed_packs.gui.states;

import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.config.PackOptions;
import io.github.fishstiz.packed_packs.gui.components.pack.Query;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.Nullable;

import java.util.*;

public record PackListState(
        List<Pack> packs,
        List<Pack> visiblePacks,
        SequencedCollection<Pack> selectedPacks,
        Query query,
        @Nullable Folder folder
) {
    public record Folder(FolderPack pack, PackListState contents) {
        public Folder withContents(PackListState contents) {
            return new Folder(this.pack, contents);
        }
    }

    private static final PackListState EMPTY = new PackListState(Collections.emptyList(), Collections.emptyList(), Collections.emptySortedSet(), Query.empty(), null);

    public static PackListState empty() {
        return EMPTY;
    }

    public PackListState(List<Pack> packs) {
        this(packs, List.copyOf(packs), Collections.emptyList(), Query.empty(), null);
    }

    public PackListState with(List<Pack> newPacks, SequencedCollection<Pack> newSelection, Query query, PackOptions options) {
        List<Pack> newVisiblePacks = processQuery(newPacks, query, options);
        // use a SequencedSet for faster lookups as this is queried every frame for each visible item within view
        // to avoid refreshing the pack list entries on each selection change
        // ... which I realize may not actually be worth it now that I'm writing this out,
        // but it has always worked that way since the creation of this project
        SequencedSet<Pack> newSelectedPacks = new ObjectLinkedOpenHashSet<>(newSelection.size());
        for (Pack pack : newSelection) {
            if (newVisiblePacks.contains(pack)) newSelectedPacks.add(pack);
        }
        return new PackListState(newPacks, newVisiblePacks, Collections.unmodifiableSequencedSet(newSelectedPacks), query, null);
    }

    public PackListState with(List<Pack> newPacks, SequencedCollection<Pack> newSelection, PackOptions options) {
        return this.with(newPacks, newSelection, this.query, options);
    }

    public PackListState withPacks(List<Pack> newPacks, PackOptions options) {
        return this.with(newPacks, this.selectedPacks, options);
    }

    public PackListState withQuery(Query query, PackOptions options) {
        return this.with(this.packs, this.selectedPacks, query, options);
    }

    public PackListState withSelection(SequencedCollection<Pack> newSelection) {
        SequencedSet<Pack> newSelectedPacks = new ObjectLinkedOpenHashSet<>(newSelection);
        newSelectedPacks.retainAll(this.visiblePacks);
        return new PackListState(this.packs, this.visiblePacks, Collections.unmodifiableSequencedSet(newSelectedPacks), this.query, this.folder);
    }

    public PackListState withFolder(PackListState.@Nullable Folder newFolder) {
        return new PackListState(this.packs, this.visiblePacks, this.selectedPacks, this.query, newFolder);
    }

    private static List<Pack> processQuery(List<Pack> sourcePacks, Query query, PackOptions options) {
        List<Pack> filtered = new ObjectArrayList<>(sourcePacks.size());
        for (Pack pack : sourcePacks) {
            if ((Config.get().isDevMode() || !options.isHidden(pack)) && query.test(pack)) {
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
        if (sequencedCollection instanceof List<E>) {
            return sequencedCollection.hashCode();
        }

        // copy of AbstractList#hashCode
        int selectionHash = 1;
        for (E obj : sequencedCollection) {
            selectionHash = 31 * selectionHash + (obj == null ? 0 : obj.hashCode());
        }
        return selectionHash;
    }

    private static <E> boolean sequencedEquals(SequencedCollection<E> a, SequencedCollection<E> b) {
        if (a == b) return true;
        if (a instanceof List<E> && b instanceof List<E>) {
            return Objects.equals(a, b);
        }

        if (a.size() != b.size()) return false;
        Iterator<E> itA = a.iterator();
        Iterator<E> itB = b.iterator();
        while (itA.hasNext()) {
            if (!Objects.equals(itA.next(), itB.next())) return false;
        }
        return true;
    }
}
