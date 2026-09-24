package io.github.fishstiz.packed_packs.util;

import io.github.fishstiz.packed_packs.gui.states.PackListKey;
import io.github.fishstiz.packed_packs.gui.states.ActiveAction;
import io.github.fishstiz.packed_packs.gui.states.PackListState;
import io.github.fishstiz.packed_packs.pack.PackNode;
import io.github.fishstiz.packed_packs.gui.states.ProfileSelection;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.packs.repository.Pack;

import java.util.*;

public final class PackListComputedUtils {
    private PackListComputedUtils() {
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
        List<PackNode> packs = state.packs();
        List<PackNode> visible = state.visiblePacks();
        if (visibleIndex == -1) {
            return visible.isEmpty() ? -1 : packs.indexOf(visible.getLast()) + 1;
        }
        return Math.clamp(packs.indexOf(visible.get(visibleIndex)), 0, packs.size());
    }

    public static int getMoveUpIndex(List<PackNode> packs, PackNode pack, ProfileSelection profile) {
        for (int i = packs.indexOf(pack) - 1; i >= 0; i--) {
            PackNode nextPack = packs.get(i);
            if (profile.isPackFixed(nextPack)) {
                return -1;
            }
            if (!profile.isPackHidden(nextPack)) {
                return i;
            }
        }
        return -1;
    }

    public static int getMoveDownIndex(List<PackNode> packs, PackNode pack, ProfileSelection profile) {
        for (int i = packs.indexOf(pack) + 1; i < packs.size(); i++) {
            PackNode nextPack = packs.get(i);
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
                PackNode pack = state.packs().get(i);
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
            SequencedCollection<PackNode> payload
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
            PackNode pack = state.packs().get(i);
            if (profiles.isPackFixed(pack)) {
                switch (profiles.getPackPosition(pack)) {
                    case TOP -> minDropIndex = i + 1;
                    case BOTTOM -> maxDropIndex = Math.min(i, maxDropIndex);
                }
            }
        }

        return absoluteIndex >= minDropIndex && absoluteIndex <= maxDropIndex;
    }

    public static boolean canDrag(PackListKey src, boolean srcModule, PackNode pack, ProfileSelection profiles) {
        if (src.type().available() || (src.depth() > 0 && !srcModule)) {
            return true;
        }
        return !profiles.isPackFixed(pack) || !profiles.isPackRequired(pack);
    }

    public static boolean canTransfer(PackListKey src, boolean srcModule, PackNode pack, ProfileSelection profiles) {
        return !srcModule && (src.type().available() || !profiles.isPackRequired(pack));
    }

    public static boolean isDropCandidate(
            ActiveAction.Dragging dragging,
            PackListKey dest,
            PackListState destState
    ) {
        PackListKey src = dragging.src();
        boolean srcModule = dragging.srcModule();

        if (srcModule || destState.module()) {
            return src.equals(dest);
        }
        if (src.depth() > 0 && src.equals(dest)) {
            return destState.query().sort() == null || !destState.query().sort().canSort();
        }
        if (src.type().enabled() && dest.type().available() && destState.parent() != null) {
            return destState.parent().children().contains(dragging.srcPack());
        }
        if (src.type().available()) {
            return dest.type().enabled();
        }
        return src.type().enabled();
    }

    public static boolean canDrop(
            ActiveAction.Dragging dragging,
            PackListKey dest,
            PackListState destState,
            int index,
            ProfileSelection profiles
    ) {
        PackListKey src = dragging.src();
        boolean srcModule = dragging.srcModule();
        PackNode srcPack = dragging.srcPack();
        SequencedCollection<PackNode> payload = dragging.packs();

        if (dest.type().available() && !destState.module()) {
            return !payload.isEmpty()
                   && isDropCandidate(dragging, dest, destState)
                   && canTransfer(src, srcModule, srcPack, profiles);
        }

        if (destState.query().hasQuery() || payload.isEmpty() || !isDropCandidate(dragging, dest, destState)) {
            return false;
        }
        if (destState.packs().isEmpty()) {
            return true;
        }
        if (src.equals(dest) && profiles.isPackFixed(srcPack)) {
            return false;
        }
        if (!isValidDropPosition(destState, getAbsoluteIndex(destState, index), profiles)) {
            return false;
        }
        if (!src.equals(dest)) {
            return canTransfer(src, srcModule, srcPack, profiles);
        }
        return isValidInsertPosition(destState, index, payload);
    }
}
