package io.github.fishstiz.packed_packs.gui.states;

import io.github.fishstiz.packed_packs.gui.model.PackListKey;
import io.github.fishstiz.packed_packs.util.PackListUtils;
import io.github.fishstiz.packed_packs.pack.PackEntry;
import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIntBiConsumer;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.fishstiz.packed_packs.util.PackListUtils.*;

public class PackListComputed {
    private final PackListKey key;
    private final Map<String, Entry> entryStates = new Object2ObjectOpenHashMap<>();

    private PackListState state;
    private ProfileSelection profiles;
    private @Nullable PackEntry selected;
    private ActiveAction.@Nullable Dragging dragging;

    private boolean entriesDirty;
    private int selectionGen = 0;
    private int transferableGen = 0;
    private int moveUpGen = 0;
    private int moveDownGen = 0;
    private int canDragGen = 0;
    private final Int2BooleanMap canDropCache;

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

        boolean packsChanged = prev.packs() != newState.packs();
        boolean selectionChanged = prev.selectedPacks() != newState.selectedPacks();
        boolean profilesChanged = newProfiles != prevProfiles;

        this.state = newState;
        this.profiles = newProfiles;

        if (packsChanged) {
            entriesDirty = true;
            transferableGen++;
            canDragGen++;
        }
        if (profilesChanged) {
            transferableGen++;
            canDragGen++;
        }
        if (selectionChanged) {
            selectionGen++;
            selected = newState.selectedPacks().isEmpty() ? null : newState.selectedPacks().getLast();
        }
        if (profilesChanged
            || selectionChanged
            || packsChanged
            || prev.visiblePacks() != newState.visiblePacks()
            || prev.query() != newState.query()) {
            moveUpGen++;
            moveDownGen++;
            canDragGen++;
            canDropCache.clear();
        }
    }

    public void onDrag(ActiveAction.@Nullable Dragging dragging) {
        if (this.dragging != dragging) {
            this.dragging = dragging;
            this.canDropCache.clear();
        }
    }

    public void forEachEntry(ObjectIntBiConsumer<Entry> action) {
        for (int i = 0; i < state.visiblePacks().size(); i++) {
            PackEntry pack = state.visiblePacks().get(i);
            Entry entry = entryStates.computeIfAbsent(pack.id(), ignored -> new Entry(pack));
            entry.pack = pack;
            action.accept(entry, i);
        }
        if (entriesDirty) {
            entriesDirty = false;
            entryStates.keySet().retainAll(state.packs().stream().map(PackEntry::id).collect(Collectors.toSet()));
        }
    }

    public boolean canReorder() {
        // todo do more than check depth
        return key.depth() > 0 || key.type().enabled();
    }

    public boolean canDrop(int index) {
        if (canDropCache.containsKey(index)) {
            return canDropCache.get(index);
        }
        boolean canDrop = dragging != null && PackListUtils.canDrop(
                dragging.target(),
                dragging.srcPack(),
                dragging.packs(),
                key,
                state,
                index,
                profiles
        );
        canDropCache.put(index, canDrop);
        return canDrop;
    }

    public boolean isFolderOpened() {
        return state.isFolderOpened();
    }

    public boolean isLocked() {
        return profiles.isLocked();
    }

    public @Nullable PackEntry getSelected() {
        return selected;
    }

    public ActiveAction.@Nullable Dragging getDragging() {
        return dragging;
    }

    public class Entry {
        private PackEntry pack;
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

        private Entry(PackEntry pack) {
            this.pack = pack;
        }

        public PackEntry pack() {
            return pack;
        }

        private void computeTransferableCache() {
            if (transferableGenSeen != transferableGen) {
                transferableGenSeen = transferableGen;
                // todo do more than check depth
                canEnableCache = !profiles.isLocked() && key.depth() == 0 && key.type().available();
                canDisableCache = !profiles.isLocked() && key.depth() == 0 && key.type().enabled() && !profiles.isPackRequired(pack);
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

        public boolean canMoveUp() {
            if (moveUpGenSeen == moveUpGen) {
                return canMoveUpCache;
            }

            moveUpGenSeen = moveUpGen;

            if (key.depth() == 0 && key.type().available()) {
                return false;
            }
            if (profiles.isLocked()) {
                return false;
            }
            if (state.query().hasQuery() || profiles.isPackFixed(pack)) {
                return false;
            }
            if (state.selectedPacks().contains(pack)) {
                List<PackEntry> selection = sortByOrderOf(state.visiblePacks(), state.selectedPacks());
                if (selection.size() > 1) {
                    int index = state.packs().indexOf(selection.getFirst());
                    int moveIndex = index > -1 ? getMoveUpIndex(state.packs(), pack, profiles) : -1;
                    canMoveUpCache = index > 0 && moveIndex > -1 && !profiles.isPackFixed(state.packs().get(moveIndex));
                    return canMoveUpCache;
                }
            }

            int index = state.packs().indexOf(pack);
            int moveIndex = getMoveUpIndex(state.packs(), pack, profiles);
            canMoveUpCache = index > 0 && moveIndex > -1 && !profiles.isPackFixed(state.packs().get(moveIndex));
            return canMoveUpCache;
        }

        public boolean canMoveDown() {
            if (moveDownGenSeen == moveDownGen) {
                return canMoveDownCache;
            }

            moveDownGenSeen = moveDownGen;

            if (key.depth() == 0 && key.type().available()) {
                return false;
            }
            if (profiles.isLocked()) {
                return false;
            }
            if (state.query().hasQuery() || profiles.isPackFixed(pack)) {
                return false;
            }

            int size = state.packs().size();
            if (state.selectedPacks().contains(pack)) {
                List<PackEntry> selection = sortByOrderOf(state.visiblePacks(), state.selectedPacks());
                if (selection.size() > 1) {
                    int index = state.packs().indexOf(selection.getLast());
                    int moveIndex = index > -1 ? getMoveDownIndex(state.packs(), pack, profiles) : -1;
                    canMoveDownCache = index > -1 && index < size - 1 && moveIndex > -1 && !profiles.isPackFixed(state.packs().get(moveIndex));
                    return canMoveDownCache;
                }
            }

            int index = state.packs().indexOf(pack);
            int moveIndex = getMoveDownIndex(state.packs(), pack, profiles);
            canMoveDownCache = index > -1 && index < size - 1 && moveIndex > -1 && !profiles.isPackFixed(state.packs().get(moveIndex));
            return canMoveDownCache;
        }

        public boolean canDrag() {
            if (canDragGenSeen == canDragGen) {
                return canDragCache;
            }

            canDragGenSeen = canDragGen;
            canDragCache = !profiles.isLocked() && PackListUtils.canDrag(key, pack, profiles);
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
