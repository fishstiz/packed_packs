package io.github.fishstiz.packed_packs.util;

import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.config.FolderPackMeta;
import io.github.fishstiz.packed_packs.gui.model.PackListType;
import io.github.fishstiz.packed_packs.gui.states.PackListState;
import io.github.fishstiz.packed_packs.pack.PackEntry;
import io.github.fishstiz.packed_packs.gui.states.ProfileSelection;
import io.github.fishstiz.packed_packs.gui.services.PackRepositoryService;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.*;

public class PackEntryResolver {
    private static boolean canRequire(PackRepositoryService repository, PackEntry pack) {
        if (pack instanceof PackEntry.Parent parent) {
            return Objects.requireNonNullElseGet(repository.getFolderMetadata(parent.id()), FolderPackMeta::new).module();
        } else {
            return true;
        }
    }

    private static void addToEnabled(
            PackRepositoryService repository,
            ProfileSelection profiles,
            List<PackEntry> enabled,
            Set<String> seen,
            PackEntry pack,
            boolean usePosition
    ) {
        pack = repository.getPackById(pack.id()); // canonical
        if (pack == null || seen.contains(pack.id())) {
            return;
        }

        PackEntry.Parent moduleAncestor = repository.findModuleAncestor(pack);
        if (pack instanceof PackEntry.Parent parent) {
            FolderPackMeta meta = Objects.requireNonNullElseGet(repository.getFolderMetadata(parent.id()), FolderPackMeta::new);
            if (meta.module() && moduleAncestor == null) {
                moduleAncestor = parent;
            }
            if (!meta.module() && moduleAncestor == null) {
                // flatten non-module folders in enabled list
                PackEntry.Parent ancestor = Objects.requireNonNullElse(repository.findAncestor(parent), parent);

                for (PackEntry leaf : repository.findDescendantLeaves(ancestor)) {
                    leaf = repository.getPackById(leaf.id()); // canonical
                    if (leaf != null && seen.add(leaf.id())) {
                        if (usePosition) {
                            profiles.getPackPosition(leaf).insert(enabled, leaf, profiles::getPackSelectionConfig, true);
                        } else {
                            enabled.add(leaf);
                        }
                    }
                }
                return;
            }
        }

        // a module's contents must not leak in the enabled list
        if (moduleAncestor != null) {
            if (seen.add(moduleAncestor.id())) {
                profiles.getPackPosition(moduleAncestor).insert(enabled, moduleAncestor, profiles::getPackSelectionConfig, true);
            }
        } else if (seen.add(pack.id())) {
            if (usePosition) {
                profiles.getPackPosition(pack).insert(enabled, pack, profiles::getPackSelectionConfig, true);
            } else {
                enabled.add(pack);
            }
        }
    }

    private static void addToDisabled(
            PackRepositoryService repository,
            ProfileSelection profiles,
            List<PackEntry> disabled,
            List<PackEntry> enabled,
            Set<String> seen,
            PackEntry pack
    ) {
        pack = repository.getPackById(pack.id()); // canonical
        if (pack == null || seen.contains(pack.id())) {
            return;
        }

        PackEntry.Parent ancestor = repository.findAncestor(pack);

        // a folder's contents must not leak in the disabled list
        if (ancestor != null) {
            FolderPackMeta meta = Objects.requireNonNullElseGet(repository.getFolderMetadata(ancestor.id()), FolderPackMeta::new);
            if (meta.module() && profiles.isPackRequired(ancestor)) {
                addToEnabled(repository, profiles, enabled, seen, pack, true);
            } else if (seen.add(ancestor.id())) {
                disabled.add(ancestor);
            }
        } else {
            if (profiles.isPackRequired(pack) && canRequire(repository, pack)) {
                addToEnabled(repository, profiles, enabled, seen, pack, true);
            } else if (seen.add(pack.id())) {
                disabled.add(pack);
            }
        }
    }

    /**
     * <ol>
     *  <li>Assume all packs in the repository are flattened AND grouped.</li>
     *  <li>A folder in enabled list cannot be a non-module.</li>
     *  <li>A non-module folder must not have its contents leak in the unselected list.</li>
     *  <li>A module folder must not have its contents leak in any list.</li>
     *  <li>If any child of a module folder is in the enabled list or is required, the entire module should be enabled.</li>
     * </ol>
     */
    // this could really use some automated tests
    public static PackEntryLists syncPackLists(
            PackRepositoryService repository,
            ProfileSelection profiles,
            List<PackEntry> disabled,
            List<PackEntry> enabled
    ) {
        List<PackEntry> packs = repository.getPacks();
        Set<String> seen = new ObjectOpenHashSet<>(packs.size());

        List<PackEntry> newEnabled = new ObjectArrayList<>(enabled.size());
        List<PackEntry> newDisabled = new ObjectArrayList<>(disabled.size());

        for (PackEntry pack : enabled) {
            addToEnabled(repository, profiles, newEnabled, seen, pack, false);
        }
        for (PackEntry pack : packs) {
            if (profiles.isPackRequired(pack) && canRequire(repository, pack)) {
                addToEnabled(repository, profiles, newEnabled, seen, pack, true);
            }
        }

        for (PackEntry pack : disabled) {
            addToDisabled(repository, profiles, newDisabled, newEnabled, seen, pack);
        }
        for (PackEntry pack : packs) {
            if (!seen.contains(pack.id())) {
                addToDisabled(repository, profiles, newDisabled, newEnabled, seen, pack);
            }
        }

        return new PackEntryLists(newDisabled, newEnabled);
    }

    public static List<PackEntry> resolveChildren(
            List<PackEntry> unsortedChildren,
            FolderPackMeta metadata,
            PackListType type,
            PackListState enabledPacks
    ) {
        if (metadata.module()) {
            Map<String, PackEntry> contentById = new Object2ObjectOpenHashMap<>(
                    unsortedChildren.size(),
                    0.99f
            );

            for (PackEntry pack : unsortedChildren) {
                contentById.put(pack.id(), pack);
            }

            List<String> orderedIds = metadata.packIds();
            Set<PackEntry> seen = new ObjectOpenHashSet<>();
            List<PackEntry> sorted = new ObjectArrayList<>(unsortedChildren.size());

            for (String id : orderedIds) {
                PackEntry pack = contentById.get(id);
                if (pack != null && seen.add(pack)) {
                    sorted.add(pack);
                }
            }

            for (PackEntry pack : unsortedChildren) {
                if (seen.add(pack)) {
                    sorted.add(pack);
                }
            }

            return sorted;
        } else {
            boolean enabledList = type.enabled();
            if (enabledList) {
                PackedPacks.LOGGER.warn(
                        "[packed_packs] Opening an unlocked folder pack from the enabled list, which should not happen"
                );
            }

            List<PackEntry> filtered = new ObjectArrayList<>(unsortedChildren.size());
            for (PackEntry pack : unsortedChildren) {
                if (enabledList || !enabledPacks.containsRecursively(pack)) {
                    filtered.add(pack);
                }
            }

            return filtered;
        }
    }
}
