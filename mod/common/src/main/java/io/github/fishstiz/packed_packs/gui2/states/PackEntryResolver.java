package io.github.fishstiz.packed_packs.gui2.states;

import io.github.fishstiz.packed_packs.config.FolderPackMeta;
import io.github.fishstiz.packed_packs.gui2.models.PackEntry;
import io.github.fishstiz.packed_packs.gui2.models.PackEntryLists;
import io.github.fishstiz.packed_packs.gui2.models.ProfileSelection;
import io.github.fishstiz.packed_packs.gui2.services.PackRepositoryService;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class PackEntryResolver {
    /**
     * <ol>
     *  <li>Assume all packs in the repository are flattened AND grouped.</li>
     *  <li>A folder in enabled list cannot be a module.</li>
     *  <li>A non-module folder must not have its contents leak in the unselected list.</li>
     *  <li>A module folder must not have its contents leak in any list.</li>
     *  <li>If any child of a module folder is in the enabled list or is required, the entire module should be enabled.</li>
     * </ol>
     */
    public static PackEntryLists syncPackLists(
            PackRepositoryService repository,
            ProfileSelection profiles,
            List<PackEntry> disabledEntries,
            List<PackEntry> enabledEntries
    ) {
        Set<String> enabledIds = enabledEntries.stream()
                .map(PackEntry::id)
                .collect(Collectors.toCollection(ObjectOpenHashSet::new));

        List<PackEntry> packs = repository.getPacks();
        Set<String> seen = new ObjectOpenHashSet<>(packs.size());
        List<PackEntry> newEnabled = new ObjectArrayList<>(enabledEntries.size());
        List<PackEntry> newDisabled = new ObjectArrayList<>(disabledEntries.size());

        for (PackEntry entry : enabledEntries) {
            PackEntry row = resolve(repository, enabledIds, profiles, entry, seen, newEnabled, true);
            if (row != null) newEnabled.add(row);
        }

        for (PackEntry entry : disabledEntries) {
            PackEntry row = resolve(repository, enabledIds, profiles, entry, seen, newEnabled, false);
            if (row != null) newDisabled.add(row);
        }

        for (PackEntry entry : packs) {
            if (entry.parentId() != null) continue;
            PackEntry row = resolve(repository, enabledIds, profiles, entry, seen, newEnabled, false);
            if (row != null) newDisabled.add(row);
        }

        return new PackEntryLists(newDisabled, newEnabled);
    }

    private static @Nullable PackEntry resolve(
            PackRepositoryService repository,
            Set<String> enabledIds,
            ProfileSelection profiles,
            PackEntry entry,
            Set<String> seen,
            List<PackEntry> enabledEntries,
            boolean forEnabled
    ) {
        PackEntry canonical = repository.getPackById(entry.id());
        if (canonical == null) return null;

        if (canonical instanceof PackEntry.Parent parent) {
            if (isModule(repository, parent.id())) {
                return resolveModule(profiles, parent, enabledIds, seen, enabledEntries);
            }
            if (forEnabled) return null;
            return resolveNonModuleParent(profiles, parent, seen, enabledEntries);
        }

        String parentId = canonical.parentId();
        if (parentId == null) {
            return resolveLeaf(profiles, canonical, seen, enabledEntries);
        }

        PackEntry parentEntry = repository.getPackById(parentId);
        if (parentEntry instanceof PackEntry.Parent parent && isModule(repository, parent.id())) {
            return resolveModule(profiles, parent, enabledIds, seen, enabledEntries); // rule 5
        }

        if (!forEnabled) {
            if (profiles.isPackRequired(canonical) && seen.add(canonical.id())) {
                profiles.getPackPosition(canonical).insert(enabledEntries, canonical, profiles::getPackSelectionConfig, true);
            }
            return null;
        }
        return resolveLeaf(profiles, canonical, seen, enabledEntries);
    }

    private static @Nullable PackEntry resolveModule(
            ProfileSelection profiles,
            PackEntry.Parent parent,
            Set<String> enabledIds,
            Set<String> seen,
            List<PackEntry> enabledEntries
    ) {
        MutableBoolean forceEnabled = new MutableBoolean(false);

        parent.children().forEach(child -> child.visitEntries(entry -> {
            seen.add(entry.id());

            if (enabledIds.contains(entry.id()) || profiles.isPackRequired(entry)) {
                forceEnabled.setTrue();
            }
        }));

        if (!seen.add(parent.id())) return null;

        if (forceEnabled.booleanValue()) {
            profiles.getPackPosition(parent).insert(enabledEntries, parent, profiles::getPackSelectionConfig, true);
            return null;
        }

        return parent;
    }

    private static @Nullable PackEntry resolveNonModuleParent(
            ProfileSelection profiles,
            PackEntry.Parent parent,
            Set<String> seen,
            List<PackEntry> enabledEntries
    ) {
        parent.children().forEach(child -> child.visitEntries(entry -> {
            if (profiles.isPackRequired(entry) && seen.add(entry.id())) {
                profiles.getPackPosition(entry).insert(enabledEntries, entry, profiles::getPackSelectionConfig, true);
            }
        }));

        return seen.add(parent.id()) ? parent : null;
    }

    private static @Nullable PackEntry resolveLeaf(
            ProfileSelection profiles,
            PackEntry leaf,
            Set<String> seen,
            List<PackEntry> enabledEntries
    ) {
        if (!seen.add(leaf.id())) {
            return null;
        }

        if (profiles.isPackRequired(leaf)) {
            profiles.getPackPosition(leaf).insert(enabledEntries, leaf, profiles::getPackSelectionConfig, true);
            return null;
        }

        return leaf;
    }

    private static boolean isModule(PackRepositoryService repository, String folderId) {
        FolderPackMeta meta = repository.getFolderMetadata(folderId);
        return meta != null && meta.module();
    }
}
