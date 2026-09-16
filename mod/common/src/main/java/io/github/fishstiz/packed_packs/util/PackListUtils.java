package io.github.fishstiz.packed_packs.util;

import io.github.fishstiz.packed_packs.gui.model.PackListKey;
import io.github.fishstiz.packed_packs.gui.states.PackListState;
import io.github.fishstiz.packed_packs.pack.PackEntry;
import io.github.fishstiz.packed_packs.gui.states.ProfileSelection;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.packs.repository.Pack;

import java.util.*;

public final class PackListUtils {
    private PackListUtils() {
    }

    public static boolean hasGap(int[] arr, boolean sorted) {
        if (arr == null || arr.length <= 1) {
            return false;
        }

        int[] sortedArray = arr.clone();
        if (!sorted) {
            Arrays.sort(sortedArray);
        }

        for (int i = 1; i < sortedArray.length; i++) {
            if (sortedArray[i] == sortedArray[i - 1]) {
                continue;
            }
            if (sortedArray[i] != sortedArray[i - 1] + 1) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasGap(int[] arr) {
        return hasGap(arr, false);
    }

    public static <T> List<T> sortByOrderOf(List<T> source, Collection<T> reference) {
        List<T> orderBy = new ObjectArrayList<>(reference);
        orderBy.retainAll(source);
        orderBy.sort(Comparator.comparingInt(source::indexOf));
        return orderBy;
    }

    public static <T> int[] indicesOf(List<T> source, SequencedCollection<T> reference) {
        int[] indices = new int[reference.size()];
        int i = 0;
        for (T element : reference) {
            int index = source.indexOf(element);
            indices[i++] = index;
        }
        return indices;
    }

    // 0 first/top, -1 last/bottom
    public static int getAbsoluteIndex(PackListState state, int visibleIndex) {
        List<PackEntry> packs = state.packs();
        List<PackEntry> visible = state.visiblePacks();
        if (visibleIndex == -1) {
            return visible.isEmpty() ? -1 : packs.indexOf(visible.getLast()) + 1;
        }
        return Math.clamp(packs.indexOf(visible.get(visibleIndex)), 0, packs.size());
    }

    public static int getMoveUpIndex(List<PackEntry> packs, PackEntry pack, ProfileSelection profile) {
        for (int i = packs.indexOf(pack) - 1; i >= 0; i--) {
            PackEntry nextPack = packs.get(i);
            if (profile.isPackFixed(nextPack)) {
                return -1;
            }
            if (!profile.isPackHidden(nextPack)) {
                return i;
            }
        }
        return -1;
    }

    public static int getMoveDownIndex(List<PackEntry> packs, PackEntry pack, ProfileSelection profile) {
        for (int i = packs.indexOf(pack) + 1; i < packs.size(); i++) {
            PackEntry nextPack = packs.get(i);
            if (profile.isPackFixed(nextPack)) {
                return -1;
            }
            if (!profile.isPackHidden(nextPack)) {
                return i;
            }
        }
        return -1;
    }

    public static int clampIndex(PackListState state, int index, ProfileSelection profiles) {
        if (index == -1) {
            int minIndex = 0;
            for (int i = 0; i < state.packs().size(); i++) {
                PackEntry pack = state.packs().get(i);
                if (profiles.isPackFixed(pack) && profiles.getPackPosition(pack) == Pack.Position.TOP) {
                    minIndex = i + 1;
                }
            }
            return minIndex;
        }
        return index;
    }

    private static boolean isValidInsertPosition(
            PackListState state,
            int visibleIndex,
            SequencedCollection<PackEntry> payload
    ) {
        int[] indices = indicesOf(state.visiblePacks(), payload);
        if (indices.length == 0) return false;
        if (hasGap(indices)) return true;
        Arrays.sort(indices);

        int lastSelectionIndex = indices[indices.length - 1];
        if (!state.packs().isEmpty()) {
            int lastItemIndex = state.packs().indexOf(state.packs().getLast());
            if (visibleIndex == -1 && lastItemIndex == lastSelectionIndex) {
                return false;
            }
        }

        return visibleIndex != indices[0] && visibleIndex - 1 != lastSelectionIndex;
    }

    private static boolean isValidDropPosition(PackListState state, int absoluteIndex, ProfileSelection profiles) {
        if (absoluteIndex < 0 || absoluteIndex > state.packs().size()) return false;

        int minDropIndex = 0;
        int maxDropIndex = state.packs().size();
        for (int i = 0; i < state.packs().size(); i++) {
            PackEntry pack = state.packs().get(i);
            if (profiles.isPackFixed(pack)) {
                switch (profiles.getPackPosition(pack)) {
                    case TOP -> minDropIndex = i + 1;
                    case BOTTOM -> maxDropIndex = Math.min(i, maxDropIndex);
                }
            }
        }

        return absoluteIndex >= minDropIndex && absoluteIndex <= maxDropIndex;
    }

    // todo do more than check depth
    public static boolean canDrag(PackListKey target, PackEntry pack, ProfileSelection profiles) {
        if (target.depth() == 0 && target.type().available()) {
            return true;
        }
        return !profiles.isPackFixed(pack);
    }

    // todo do more than check depth
    public static boolean canTransfer(PackListKey target, PackEntry pack, ProfileSelection profiles) {
        return target.depth() == 0 && (target.type().available() || !profiles.isPackRequired(pack));
    }

    public static boolean canInteract(PackListKey target, PackListKey destination) {
        // todo do more than check depth
        if (target.depth() > 0 || destination.depth() > 0) {
            return target.equals(destination);
        }
        if (target.type().available()) {
            return destination.type().enabled();
        }
        return target.type().enabled();
    }

    public static boolean canDrop(
            PackListKey target,
            PackEntry pack,
            SequencedCollection<PackEntry> payload,
            PackListKey destination,
            PackListState targetState,
            int index,
            ProfileSelection profiles
    ) {
        if (destination.type().available() && destination.depth() == 0) {
            return !payload.isEmpty() && canInteract(target, destination) && canTransfer(target, pack, profiles);
        }
        if (targetState.query().hasQuery() || payload.isEmpty() || !canInteract(target, destination)) {
            return false;
        }
        if (targetState.packs().isEmpty()) {
            return true;
        }
        if (target.equals(destination) && profiles.isPackFixed(pack)) {
            return false;
        }
        if (!isValidDropPosition(targetState, getAbsoluteIndex(targetState, index), profiles)) {
            return false;
        }
        if (!target.equals(destination)) {
            return canTransfer(target, pack, profiles);
        }
        return isValidInsertPosition(targetState, index, payload);
    }
}
