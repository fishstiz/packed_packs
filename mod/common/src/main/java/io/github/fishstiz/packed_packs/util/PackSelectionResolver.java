package io.github.fishstiz.packed_packs.util;

import io.github.fishstiz.packed_packs.config.FolderPackMeta;
import io.github.fishstiz.packed_packs.pack.PackNode;
import io.github.fishstiz.packed_packs.gui.states.ProfileSelection;
import io.github.fishstiz.packed_packs.pack.PackNodeRepository;
import io.github.fishstiz.packed_packs.pack.PackSelection;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.util.TriState;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class PackSelectionResolver {
    private static boolean canRequire(PackNodeRepository repository, PackNode pack) {
        if (pack instanceof PackNode.Parent parent) {
            return repository.getFolderMetadata(parent).module();
        } else {
            return true;
        }
    }

    private static void insertNode(List<PackNode> nodes, PackNode node, @Nullable ProfileSelection profiles) {
        if (profiles != null) {
            profiles.getPackPosition(node).insert(nodes, node, profiles::getPackSelectionConfig, true);
        } else {
            nodes.add(node);
        }
    }

    private static void addToEnabled(
            PackNodeRepository repository,
            @Nullable ProfileSelection profiles,
            List<PackNode> enabled,
            Set<String> enabledIds,
            Set<String> seen,
            PackNode pack
    ) {
        pack = repository.getPackById(pack.id()); // canonical
        if (pack == null || seen.contains(pack.id())) {
            return;
        }

        PackNode.Parent moduleAncestor = repository.findModuleAncestor(pack);
        if (pack instanceof PackNode.Parent parent) {
            FolderPackMeta meta = repository.getFolderMetadata(parent);
            if (meta.module() && moduleAncestor == null) {
                moduleAncestor = parent;
            }
            if (!meta.module() && moduleAncestor == null) {
                // flatten non-module folders in enabled list
                repository.collectDescendantLeaves(parent, TriState.DEFAULT, leaf -> {
                    leaf = repository.getPackById(leaf.id()); // canonical
                    // preserve order from enabled ids, do no eagerly add
                    if (leaf != null && !enabledIds.contains(leaf.id()) && seen.add(leaf.id())) {
                        insertNode(enabled, leaf, profiles);
                    }
                });
                return;
            }
        }

        // a module's contents must not leak in the enabled list
        PackNode resolved = moduleAncestor == null ? pack : moduleAncestor;
        if (seen.add(resolved.id())) {
            insertNode(enabled, resolved, profiles);
        }
    }

    private static void addToDisabled(
            PackNodeRepository repository,
            ProfileSelection profiles,
            List<PackNode> disabled,
            List<PackNode> enabled,
            Set<String> enabledIds,
            Set<String> seen,
            PackNode pack
    ) {
        pack = repository.getPackById(pack.id()); // canonical
        if (pack == null || seen.contains(pack.id())) {
            return;
        }

        PackNode.Parent ancestor = repository.findAncestor(pack);
        // a folder's contents must not leak in the disabled list
        if (ancestor != null) {
            FolderPackMeta meta = repository.getFolderMetadata(ancestor);
            if (meta.module() && profiles.isPackRequired(ancestor)) {
                addToEnabled(repository, profiles, enabled, enabledIds, seen, pack);
            } else if (seen.add(ancestor.id())) {
                disabled.add(ancestor);
            }
        } else {
            if (profiles.isPackRequired(pack) && canRequire(repository, pack)) {
                addToEnabled(repository, profiles, enabled, enabledIds, seen, pack);
            } else if (seen.add(pack.id())) {
                disabled.add(pack);
            }
        }
    }

    /**
     * <ol>
     *  <li>Assume all packs in the repository are flattened AND grouped.</li>
     *  <li>A folder in enabled list cannot be a non-module and must be flattened.</li>
     *  <li>A folder must not have its contents leak in the unselected list.</li>
     *  <li>A module folder must not have its contents leak in the selected list.</li>
     *  <li>If any child of a module folder is in the enabled list or is required, the entire module should be enabled.</li>
     *  <li>It should not be possible for a module folder to have non-module folder descendants.</li>
     * </ol>
     */
    public static PackSelection syncPacksWithRepository( // this could really use some automated tests
            PackNodeRepository repository,
            ProfileSelection profiles,
            List<PackNode> disabled,
            List<PackNode> enabled
    ) {
        List<PackNode> packs = repository.getPacks();
        Set<String> enabledIds = enabled.stream().map(PackNode::id).collect(Collectors.toCollection(ObjectOpenHashSet::new));
        Set<String> seen = new ObjectOpenHashSet<>(packs.size());

        List<PackNode> newEnabled = new ObjectArrayList<>(enabled.size());
        List<PackNode> newDisabled = new ObjectArrayList<>(disabled.size());

        for (PackNode pack : enabled) {
            addToEnabled(repository, null, newEnabled, enabledIds, seen, pack);
        }
        for (PackNode pack : packs) {
            if (profiles.isPackRequired(pack) && canRequire(repository, pack)) {
                addToEnabled(repository, profiles, newEnabled, enabledIds, seen, pack);
            }
        }

        for (PackNode pack : disabled) {
            addToDisabled(repository, profiles, newDisabled, newEnabled, enabledIds, seen, pack);
        }
        for (PackNode pack : packs) {
            if (!seen.contains(pack.id())) {
                addToDisabled(repository, profiles, newDisabled, newEnabled, enabledIds, seen, pack);
            }
        }

        return new PackSelection(newDisabled, newEnabled);
    }
}
