package io.github.fishstiz.packed_packs.gui2.states;

import io.github.fishstiz.packed_packs.gui.model.PackListKey;
import io.github.fishstiz.packed_packs.gui.states.PackListState;
import io.github.fishstiz.packed_packs.gui.states.ProfilesState;
import io.github.fishstiz.packed_packs.gui2.models.PackEntry;

import java.util.List;

import static io.github.fishstiz.packed_packs.gui.model.PackListUtils.*;
import static io.github.fishstiz.packed_packs.gui.model.PackListUtils.getMoveDownIndex;

public class PackListEntrySelector {
    private final PackListKey key;
    private final PackEntry pack;

    public PackListEntrySelector(PackListKey key, PackEntry pack) {
        this.key = key;
        this.pack = pack;
    }

    // todo memoize and diff dependencies

    public boolean isSelected(PackListState listState) {
        return listState.selectedPacks().contains(pack);
    }

    public boolean canMoveUp(PackListState listState, ProfilesState profilesState) {
        if (key.depth() == 0 && key.type().available()) {
            return false;
        }
        if (profilesState.isLocked()) {
            return false;
        }
        if (listState.query().hasQuery() || profilesState.isPackFixed(pack)) {
            return false;
        }
        if (listState.selectedPacks().contains(pack)) {
            List<PackEntry> selection = sortByOrderOf(listState.visiblePacks(), listState.selectedPacks());
            if (selection.size() > 1) {
                int index = listState.packs().indexOf(selection.getFirst());
                int moveIndex = index > -1 ? getMoveUpIndex(listState.packs(), pack, profilesState) : -1;
                return index > 0 && moveIndex > -1 && !profilesState.isPackFixed(listState.packs().get(moveIndex));
            }
        }
        int index = listState.packs().indexOf(pack);
        int moveIndex = getMoveUpIndex(listState.packs(), pack, profilesState);
        return index > 0 && moveIndex > -1 && !profilesState.isPackFixed(listState.packs().get(moveIndex));
    }

    public boolean canMoveDown(PackListState listState, ProfilesState profilesState) {
        if (key.depth() == 0 && key.type().available()) {
            return false;
        }
        if (profilesState.isLocked()) {
            return false;
        }
        if (listState.query().hasQuery() || profilesState.isPackFixed(pack)) {
            return false;
        }

        int size = listState.packs().size();
        if (listState.selectedPacks().contains(pack)) {
            List<PackEntry> selection = sortByOrderOf(listState.visiblePacks(), listState.selectedPacks());
            if (selection.size() > 1) {
                int index = listState.packs().indexOf(selection.getLast());
                int moveIndex = index > -1 ? getMoveDownIndex(listState.packs(), pack, profilesState) : -1;
                return index > -1 && index < size - 1 && moveIndex > -1 && !profilesState.isPackFixed(listState.packs().get(moveIndex));
            }
        }

        int index = listState.packs().indexOf(pack);
        int moveIndex = getMoveDownIndex(listState.packs(), pack, profilesState);
        return index > -1 && index < size - 1 && moveIndex > -1 && !profilesState.isPackFixed(listState.packs().get(moveIndex));
    }
}
