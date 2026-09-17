package io.github.fishstiz.packed_packs.gui.services;

import com.google.common.collect.ImmutableList;
import io.github.fishstiz.fidgetz.v0.utils.CollectionUtils;
import io.github.fishstiz.fidgetz.v0.utils.FunctionUtils;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.config.FolderPackMeta;
import io.github.fishstiz.packed_packs.config.JsonLoader;
import io.github.fishstiz.packed_packs.pack.FolderLocationInfo;
import io.github.fishstiz.packed_packs.pack.FolderResourcesSupplier;
import io.github.fishstiz.packed_packs.pack.PackEntry;
import io.github.fishstiz.packed_packs.transform.interfaces.FilePack;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionModelAccessor;
import io.github.fishstiz.packed_packs.transform.mixin.folders.additional.FolderRepositorySourceAccessor;
import io.github.fishstiz.packed_packs.transform.mixin.folders.additional.PackRepositoryAccessor;
import io.github.fishstiz.packed_packs.util.PackUtil;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.IoSupplier;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PackRepositoryService {
    private final PackRepository repository;
    private final Path packDir;

    // folder meta is decoupled from folder entries to prevent overwriting unsaved state when refreshing packs
    private final Map<String, FolderPackMeta> folderMeta = new ConcurrentHashMap<>();

    private volatile PackSelectionModel selectionModel;
    private volatile Map<String, PackEntry> packs = Collections.emptyMap();
    private volatile Set<String> selectedPackIds = Collections.emptySet();

    public PackRepositoryService(PackRepository repository, Path packDir) {
        this.repository = repository;
        this.packDir = packDir;
        this.refreshSelectionModel();
    }

    public PackRepository getRepository() {
        return this.repository;
    }

    private void refreshSelectionModel() {
        this.selectionModel = new PackSelectionModel(
                FunctionUtils.nopConsumer(),
                ignored -> PackEntry.Leaf.DEFAULT_ICON,
                this.repository,
                FunctionUtils.nopConsumer()
        );
        ((PackSelectionModelAccessor) this.selectionModel).packed_packs$filterHidden(false);
    }

    private void loadFolderMetadata(PackEntry entry) {
        // load metadata on first discover only to prevent overwriting unsaved state
        if (!(entry instanceof PackEntry.Parent parent) || folderMeta.containsKey(entry.id())) {
            return;
        }

        try (PackResources resources = parent.open()) {
            IoSupplier<InputStream> streamSupplier = resources.getRootResource(FolderPackMeta.FILENAME);
            if (streamSupplier != null) {
                try (InputStream stream = streamSupplier.get()) {
                    folderMeta.put(entry.id(), JsonLoader.loadOrDefault(
                            stream,
                            FolderPackMeta.class,
                            FolderPackMeta::new
                    ));
                    return;
                } catch (Exception e) {
                    PackedPacks.LOGGER.error("[packed_packs] Failed to read folder metadata at {}", entry.path(), e);
                }
            }

            folderMeta.put(entry.id(), new FolderPackMeta());
        }
    }

    private void collectDescendantLeaves(PackEntry.Parent current, List<PackEntry> collector) {
        FolderPackMeta meta = Objects.requireNonNullElseGet(folderMeta.get(current.id()), FolderPackMeta::new);
        if (meta.module()) {
            collector.add(current);
            return;
        }

        for (PackEntry child : current.children()) {
            if (child instanceof PackEntry.Leaf leaf) {
                collector.add(leaf);
            }
            if (child instanceof PackEntry.Parent inner) {
                collectDescendantLeaves(inner, collector);
            }
        }
    }

    public List<PackEntry> findDescendantLeaves(PackEntry.Parent parent) {
        List<PackEntry> leaves = new ObjectArrayList<>();
        collectDescendantLeaves(parent, leaves);
        return leaves;
    }

    public PackEntry.@Nullable Parent findAncestor(PackEntry pack) {
        Map<String, PackEntry> packs = this.packs;

        PackEntry current = packs.get(pack.parentId());
        PackEntry.Parent ancestor = null;

        while (current instanceof PackEntry.Parent parent) {
            ancestor = parent;
            current = current.parentId() == null ? null : packs.get(current.parentId());
        }

        return ancestor;
    }

    public PackEntry.@Nullable Parent findModuleAncestor(PackEntry pack) {
        Map<String, PackEntry> packs = this.packs;

        PackEntry current = packs.get(pack.parentId());
        PackEntry.Parent moduleAncestor = null;

        while (current instanceof PackEntry.Parent parent) {
            FolderPackMeta meta = Objects.requireNonNullElseGet(folderMeta.get(current.id()), FolderPackMeta::new);

            if (meta.module()) {
                moduleAncestor = parent;
            }
            current = current.parentId() == null ? null : packs.get(current.parentId());
        }

        return moduleAncestor;
    }

    private void refreshSelectedCache() {
        Map<String, PackEntry> packs = this.packs;

        Collection<String> selectedLeaves = repository.getSelectedIds();
        Set<String> newSelected = new ObjectOpenHashSet<>(selectedLeaves.size());

        for (String id : selectedLeaves) {
            newSelected.add(id);

            PackEntry pack = packs.get(id);
            if (pack != null) {
                PackEntry.Parent moduleAncestor = findModuleAncestor(pack);
                // the entire module should be selected if any of its descendants are selected
                if (moduleAncestor != null && !newSelected.contains(moduleAncestor.id())) {
                    moduleAncestor.visitEntries(entry -> newSelected.add(entry.id()));
                }
            }
        }

        this.selectedPackIds = newSelected;
    }

    public void refreshSources() {
        long start = 0;
        if (PackedPacks.DEBUG) {
            start = System.nanoTime();
            PackedPacks.LOGGER.info("[packed_packs] ======== Refreshing Repository Sources ========");
        }

        refreshSelectionModel();

        this.selectionModel.findNewPacks();

        PackSelectionModelAccessor model = (PackSelectionModelAccessor) this.selectionModel;
        List<Pack> allPacks = new ObjectArrayList<>(model.getSelectedPacks().size() + model.getUnselectedPacks().size());
        allPacks.addAll(model.getSelectedPacks());
        allPacks.addAll(model.getUnselectedPacks());

        Map<String, Pack> leavesById = new Object2ObjectLinkedOpenHashMap<>();
        Map<String, FolderLocationInfo> folderInfoById = new Object2ObjectOpenHashMap<>();
        Map<String, Set<String>> folderChildIds = new Object2ObjectOpenHashMap<>();

        for (Pack pack : allPacks) {
            String id = pack.getId();
            leavesById.putIfAbsent(id, pack);

            FolderLocationInfo parentInfo = ((FilePack) pack).packed_packs$getParent();
            String childId = id;

            while (parentInfo != null) {
                String folderId = parentInfo.location().id();
                folderInfoById.putIfAbsent(folderId, parentInfo);
                folderChildIds.computeIfAbsent(folderId, ignored -> new ObjectLinkedOpenHashSet<>()).add(childId);

                childId = folderId;
                parentInfo = parentInfo.parent();
            }
        }

        Map<String, PackEntry> newPacks = new Object2ObjectLinkedOpenHashMap<>();

        for (String id : leavesById.keySet()) {
            buildEntry(id, leavesById, folderInfoById, folderChildIds, newPacks);
        }

        for (String id : folderInfoById.keySet()) {
            buildEntry(id, leavesById, folderInfoById, folderChildIds, newPacks);
        }

        this.packs = newPacks;
        folderMeta.keySet().retainAll(newPacks.keySet());
        refreshSelectedCache();

        if (PackedPacks.DEBUG) {
            PackedPacks.LOGGER.info("[packed_packs] ======== Repository Sources Refreshed in {}ms ========", PackedPacks.duration(start));
        }
    }

    private PackEntry buildEntry(
            String id,
            Map<String, Pack> leavesById,
            Map<String, FolderLocationInfo> folderInfoById,
            Map<String, Set<String>> folderChildIds,
            Map<String, PackEntry> newPacks
    ) {
        PackEntry existing = newPacks.get(id);
        if (existing != null) return existing;

        Pack leafPack = leavesById.get(id);
        if (leafPack != null) {
            FolderLocationInfo parent = ((FilePack) leafPack).packed_packs$getParent();
            String parentId = parent == null ? null : parent.location().id();
            Path path = ((FilePack) leafPack).packed_packs$getPath();

            PackEntry entry = new PackEntry.Leaf(leafPack, path, parentId);
            newPacks.put(id, entry);
            return entry;
        }

        FolderLocationInfo info = folderInfoById.get(id);
        List<PackEntry> children = new ObjectArrayList<>();
        for (String childId : folderChildIds.getOrDefault(id, Set.of())) {
            children.add(buildEntry(childId, leavesById, folderInfoById, folderChildIds, newPacks));
        }

        FolderLocationInfo parentInfo = info.parent();
        String parentId = parentInfo == null ? null : parentInfo.location().id();

        PackEntry entry = new PackEntry.Parent(
                parentId,
                info.path(),
                info.location(),
                new FolderResourcesSupplier(info.path()),
                children
        );

        loadFolderMetadata(entry);
        newPacks.put(id, entry);
        return entry;
    }

    public void setEnabledPacks(List<PackEntry> packs) {
        MutableBoolean highContrast = new MutableBoolean(false);
        List<String> packIds = new ObjectArrayList<>();

        packs.forEach(selectedEntry -> selectedEntry.visitPacks(entry -> {
            String id = entry.getId();
            packIds.add(id);

            if (id.equals(PackUtil.HIGH_CONTRAST_ID)) {
                highContrast.setTrue();
            }
        }));

        this.repository.setSelected(ImmutableList.copyOf(packIds));

        OptionInstance<@NonNull Boolean> highContrastOption = Minecraft.getInstance().options.highContrast();
        if (highContrastOption.get() != highContrast.booleanValue()) {
            highContrastOption.set(highContrast.booleanValue());
        }

        refreshSelectionModel();
        refreshSelectedCache();
    }

    /**
     * @return flattened AND grouped pack entries.
     */
    public List<PackEntry> getPacks() {
        return List.copyOf(this.packs.values());
    }

    public @Nullable PackEntry getPackById(String id) {
        return this.packs.get(id);
    }

    public List<PackEntry> getEnabledPacks() {
        Map<String, PackEntry> currentPacks = this.packs;
        return ((PackSelectionModelAccessor) this.selectionModel).getSelectedPacks()
                .stream()
                .map(pack -> currentPacks.get(pack.getId()))
                .filter(Objects::nonNull)
                .toList();
    }

    public List<PackEntry> getPacksById(List<String> packIds) {
        return CollectionUtils.lookup(packIds, this.packs);
    }

    public @Nullable FolderPackMeta getFolderMetadata(String folderId) {
        return folderMeta.get(folderId);
    }

    public boolean setFolderMetadata(String folderId, FolderPackMeta metadata) {
        if (folderMeta.containsKey(folderId)) {
            folderMeta.put(folderId, metadata);
            return true;
        } else {
            PackedPacks.LOGGER.warn("[packed_packs] Tried to update folder metadata from non-existing folder '{}'", folderId);
            return false;
        }
    }

    public void removeFolderMetadata(String folderId) {
        folderMeta.remove(folderId);
    }

    public boolean isEnabled(String packId) {
        return this.selectedPackIds.contains(packId);
    }

    public void removePack(String packId) {
        repository.removePack(packId);

        Map<String, PackEntry> newPacks = new Object2ObjectLinkedOpenHashMap<>(this.packs);
        PackEntry entry = newPacks.get(packId);
        if (entry != null) {
            entry.visitEntries(e -> {
                newPacks.remove(e.id());
                repository.removePack(e.id());
            });

            this.packs = newPacks;
            refreshSelectionModel();
        }
    }

    public Path baseDirectorySource() {
        return packDir;
    }

    public List<Path> otherDirectorySources() {
        Path normalizedBaseDir = baseDirectorySource().toAbsolutePath().normalize();

        return ((PackRepositoryAccessor) this.repository).packed_packs$getSources().stream()
                .filter(FolderRepositorySourceAccessor.class::isInstance)
                .map(source -> ((FolderRepositorySourceAccessor) source).packed_packs$getFolder().toAbsolutePath().normalize())
                .filter(path -> !path.equals(normalizedBaseDir))
                .distinct()
                .toList();
    }
}
