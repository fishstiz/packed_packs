package io.github.fishstiz.packed_packs.gui.model;

import io.github.fishstiz.packed_packs.config.PackOptions;
import io.github.fishstiz.packed_packs.gui.states.PackListState;
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
        List<Pack> packs = state.packs();
        List<Pack> visible = state.visiblePacks();
        if (visibleIndex == -1) {
            return visible.isEmpty() ? -1 : packs.indexOf(visible.getLast()) + 1;
        }
        return Math.clamp(packs.indexOf(visible.get(visibleIndex)), 0, packs.size());
    }

    public static int getMoveUpIndex(List<Pack> packs, Pack pack, PackOptions options) {
        for (int i = packs.indexOf(pack) - 1; i >= 0; i--) {
            Pack nextPack = packs.get(i);
            if (options.isFixed(nextPack)) {
                return -1;
            }
            if (!options.isHidden(nextPack)) {
                return i;
            }
        }
        return -1;
    }

    public static int getMoveDownIndex(List<Pack> packs, Pack pack, PackOptions options) {
        for (int i = packs.indexOf(pack) + 1; i < packs.size(); i++) {
            Pack nextPack = packs.get(i);
            if (options.isFixed(nextPack)) {
                return -1;
            }
            if (!options.isHidden(nextPack)) {
                return i;
            }
        }
        return -1;
    }

    public static int clampIndex(PackListState state, int index, PackOptions options) {
        if (index == -1) {
            int minIndex = 0;
            for (int i = 0; i < state.packs().size(); i++) {
                Pack pack = state.packs().get(i);
                if (options.isFixed(pack) && options.getPosition(pack) == Pack.Position.TOP) {
                    minIndex = i + 1;
                }
            }
            return minIndex;
        }
        return index;
    }

    private static boolean isValidInsertPosition(PackListState state, int visibleIndex, SequencedCollection<Pack> payload) {
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

    private static boolean isValidDropPosition(PackListState state, int absoluteIndex, PackOptions options) {
        if (absoluteIndex < 0 || absoluteIndex > state.packs().size()) return false;

        int minDropIndex = 0;
        int maxDropIndex = state.packs().size();
        for (int i = 0; i < state.packs().size(); i++) {
            Pack pack = state.packs().get(i);
            if (options.isFixed(pack)) {
                switch (options.getPosition(pack)) {
                    case TOP -> minDropIndex = i + 1;
                    case BOTTOM -> maxDropIndex = Math.min(i, maxDropIndex);
                }
            }
        }

        return absoluteIndex >= minDropIndex && absoluteIndex <= maxDropIndex;
    }

    public static boolean canDrag(PackListKey target, Pack pack, PackOptions options) {
        if (target.depth() == 0 && target.type().available()) {
            return true;
        }
        return !options.isFixed(pack);
    }

    public static boolean canTransfer(PackListKey target, Pack pack, PackOptions options) {
        return target.depth() == 0 && (target.type().available() || !options.isRequired(pack));
    }

    public static boolean canInteract(PackListKey target, PackListKey destination) {
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
            Pack pack,
            SequencedCollection<Pack> payload,
            PackListKey destination,
            PackListState targetState,
            int index,
            PackOptions options
    ) {
        if (destination.type().available() && destination.depth() == 0) {
            return !payload.isEmpty() && canInteract(target, destination) && canTransfer(target, pack, options);
        }
        if (targetState.query().hasQuery() || payload.isEmpty() || !canInteract(target, destination)) {
            return false;
        }
        if (targetState.packs().isEmpty()) {
            return true;
        }
        if (target.equals(destination) && options.isFixed(pack)) {
            return false;
        }
        if (!isValidDropPosition(targetState, getAbsoluteIndex(targetState, index), options)) {
            return false;
        }
        if (!target.equals(destination)) {
            return canTransfer(target, pack, options);
        }
        return isValidInsertPosition(targetState, index, payload);
    }
}
