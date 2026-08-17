package io.github.fishstiz.packed_packs.gui.states;

import io.github.fishstiz.fidgetz.v0.utils.TriState;
import io.github.fishstiz.packed_packs.util.ObjectIntBiConsumer;
import io.github.fishstiz.packed_packs.util.PackListComputedUtils;
import io.github.fishstiz.packed_packs.pack.PackNode;
import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.fishstiz.packed_packs.util.PackListComputedUtils.*;

public class PackListComputed {
    private final PackListKey key;
    private final Map<String, Entry> entryStates = new Object2ObjectOpenHashMap<>();

    private PackListState state;
    private ProfileSelection profiles;
    private @Nullable PackNode selected;
    private ActiveAction.@Nullable Dragging dragging;

    private boolean entriesDirty;
    private int selectionGen = 0;
    private int transferableGen = 0;
    private int moveUpGen = 0;
    private int moveDownGen = 0;
    private int canDragGen = 0;
    private final Int2BooleanMap canDropCache;
    private TriState dropCandidateCache = TriState.DEFAULT;

    public PackListComputed(PackListKey key, PackListState initialState, ProfileSelection initialProfiles) {
        this.key = key;
        this.state = initialState;
        this.profiles = initialProfiles;
        this.selected = initialState.selectedPacks().isEmpty() ? null : initialState.selectedPacks().getLast();
        int dropCacheExpectedSize = key.type().available() && key.depth() == 0 ? 2 : 8;
        this.canDropCache = new Int2BooleanOpenHashMap(dropCacheExpectedSize, 0.99f);
    }

    public PackListKey key() {
        return key;
    }

    public PackListState state() {
        return state;
    }

    public ProfileSelection profiles() {
        return profiles;
    }

    public void onStateChanged(PackListState newState, ProfileSelection newProfiles) {
        PackListState prev = this.state;
        ProfileSelection prevProfiles = this.profiles;

        boolean moduleStateChanged = prev.module() != newState.module();
        boolean packsChanged = prev.packs() != newState.packs();
        boolean selectionChanged = prev.selectedPacks() != newState.selectedPacks();
        boolean profilesChanged = newProfiles != prevProfiles;

        this.state = newState;
        this.profiles = newProfiles;

        if (packsChanged) {
            entriesDirty = true;
        }
        if (selectionChanged) {
            selectionGen++;
            selected = newState.selectedPacks().isEmpty() ? null : newState.selectedPacks().getLast();
        }
        if (packsChanged || profilesChanged || moduleStateChanged) {
            transferableGen++;
            canDragGen++;
            canDropCache.clear();
        }
        if (packsChanged
            || selectionChanged
            || profilesChanged
            || moduleStateChanged
            || prev.visiblePacks() != newState.visiblePacks()
            || prev.query() != newState.query()) {
            moveUpGen++;
            moveDownGen++;
        }
    }

    public void onDrag(ActiveAction.@Nullable Dragging dragging) {
        if (this.dragging != dragging) {
            this.dragging = dragging;
            this.canDropCache.clear();
            this.dropCandidateCache = TriState.DEFAULT;
        }
    }

    public void forEachEntry(ObjectIntBiConsumer<Entry> action) {
        for (int i = 0; i < state.visiblePacks().size(); i++) {
            PackNode pack = state.visiblePacks().get(i);
            Entry entry = entryStates.computeIfAbsent(pack.id(), ignored -> new Entry(pack));
            entry.pack = pack;
            action.accept(entry, i);
        }
        if (entriesDirty) {
            entriesDirty = false;
            entryStates.keySet().retainAll(state.packs().stream().map(PackNode::id).collect(Collectors.toSet()));
        }
    }

    public boolean canReorder() {
        return state.module() || (key.depth() == 0 && key.type().enabled());
    }

    public boolean canDrop(int index) {
        if (canDropCache.containsKey(index)) {
            return canDropCache.get(index);
        }
        boolean canDrop = dragging != null && PackListComputedUtils.canDrop(dragging, key, state, index, profiles);
        canDropCache.put(index, canDrop);
        return canDrop;
    }

    public boolean isFolderOpened() {
        return state.isFolderOpened();
    }

    public boolean isLocked() {
        return profiles.isLocked();
    }

    public @Nullable PackNode getSelected() {
        return selected;
    }

    public boolean isDragging() {
        return dragging != null;
    }

    public boolean isDropCandidate() {
        if (dropCandidateCache == TriState.DEFAULT) {
            dropCandidateCache = TriState.from(dragging != null && PackListComputedUtils.isDropCandidate(dragging, key, state));
        }

        return dropCandidateCache == TriState.TRUE;
    }

    public class Entry {
        private PackNode pack;
        private int selectionGenSeen = -1;
        private int moveUpGenSeen = -1;
        private int moveDownGenSeen = -1;
        private int transferableGenSeen = -1;
        private int canDragGenSeen = -1;
        private boolean canEnableCache;
        private boolean canDisableCache;
        private boolean canTransferCache;
        private boolean canMoveUpCache;
        private boolean canMoveDownCache;
        private boolean canDragCache;
        private boolean selectedCache;
        private boolean lastSelectedCache;

        private Entry(PackNode pack) {
            this.pack = pack;
        }

        public PackNode pack() {
            return pack;
        }

        private void computeTransferableCache() {
            if (transferableGenSeen != transferableGen) {
                transferableGenSeen = transferableGen;
                canEnableCache = !profiles.isLocked() && !state.module() && key.type().available();
                canDisableCache = !profiles.isLocked() && !state.module() && key.type().enabled() && !profiles.isPackRequired(pack);
                canTransferCache = key.type().available() ? canEnableCache : canDisableCache;
            }
        }

        public boolean canTransfer() {
            computeTransferableCache();
            return canTransferCache;
        }

        public boolean canEnable() {
            computeTransferableCache();
            return canEnableCache;
        }

        public boolean canDisable() {
            computeTransferableCache();
            return canDisableCache;
        }

        private void computeCanMoveUpCache() {
            if (moveUpGenSeen == moveUpGen) {
                return;
            }

            moveUpGenSeen = moveUpGen;

            if (!canReorder()) {
                canMoveUpCache = false;
                return;
            }
            if (profiles.isLocked()) {
                canMoveUpCache = false;
                return;
            }
            if (state.query().hasQuery() || profiles.isPackFixed(pack)) {
                canMoveUpCache = false;
                return;
            }
            if (state.selectedPacks().contains(pack)) {
                List<PackNode> selection = sortByOrderOf(state.visiblePacks(), state.selectedPacks());
                if (selection.size() > 1) {
                    int index = state.packs().indexOf(selection.getFirst());
                    int moveIndex = index > -1 ? getMoveUpIndex(state.packs(), pack, profiles) : -1;
                    canMoveUpCache = index > 0 && moveIndex > -1 && !profiles.isPackFixed(state.packs().get(moveIndex));
                    return;
                }
            }

            int index = state.packs().indexOf(pack);
            int moveIndex = getMoveUpIndex(state.packs(), pack, profiles);
            canMoveUpCache = index > 0 && moveIndex > -1 && !profiles.isPackFixed(state.packs().get(moveIndex));
        }

        public boolean canMoveUp() {
            computeCanMoveUpCache();
            return canMoveUpCache;
        }

        private void computeCanMoveDownCache() {
            if (moveDownGenSeen == moveDownGen) {
                return;
            }

            moveDownGenSeen = moveDownGen;

            if (!canReorder()) {
                canMoveDownCache = false;
                return;
            }
            if (profiles.isLocked()) {
                canMoveDownCache = false;
                return;
            }
            if (state.query().hasQuery() || profiles.isPackFixed(pack)) {
                canMoveDownCache = false;
                return;
            }

            int size = state.packs().size();
            if (state.selectedPacks().contains(pack)) {
                List<PackNode> selection = sortByOrderOf(state.visiblePacks(), state.selectedPacks());
                if (selection.size() > 1) {
                    int index = state.packs().indexOf(selection.getLast());
                    int moveIndex = index > -1 ? getMoveDownIndex(state.packs(), pack, profiles) : -1;
                    canMoveDownCache = index > -1 && index < size - 1 && moveIndex > -1 && !profiles.isPackFixed(state.packs().get(moveIndex));
                    return;
                }
            }

            int index = state.packs().indexOf(pack);
            int moveIndex = getMoveDownIndex(state.packs(), pack, profiles);
            canMoveDownCache = index > -1 && index < size - 1 && moveIndex > -1 && !profiles.isPackFixed(state.packs().get(moveIndex));
        }

        public boolean canMoveDown() {
            computeCanMoveDownCache();
            return canMoveDownCache;
        }

        public boolean canDrag() {
            if (canDragGenSeen == canDragGen) {
                return canDragCache;
            }

            canDragGenSeen = canDragGen;
            canDragCache = !profiles.isLocked() && PackListComputedUtils.canDrag(key, state.module(), pack, profiles);
            return canDragCache;
        }

        private void computeSelectionCache() {
            if (selectionGenSeen != selectionGen) {
                selectionGenSeen = selectionGen;
                selectedCache = state.selectedPacks().contains(pack);
                lastSelectedCache = selected != null && selected.id().equals(pack.id());
            }
        }

        public boolean isSelected() {
            computeSelectionCache();
            return selectedCache;
        }

        public boolean isSelectedLast() {
            computeSelectionCache();
            return lastSelectedCache;
        }

        public boolean isSelectedExclusively() {
            return isSelectedLast() && state.selectedPacks().size() == 1;
        }
    }
}
