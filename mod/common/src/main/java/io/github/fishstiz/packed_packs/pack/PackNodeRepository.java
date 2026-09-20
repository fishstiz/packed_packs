package io.github.fishstiz.packed_packs.pack;

import com.google.common.collect.ImmutableList;
import io.github.fishstiz.fidgetz.v0.utils.CollectionUtils;
import io.github.fishstiz.fidgetz.v0.utils.FunctionUtils;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.config.FolderPackMeta;
import io.github.fishstiz.packed_packs.config.JsonLoader;
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
import net.minecraft.util.TriState;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class PackNodeRepository {
    private final PackRepository repository;
    private final Path packDir;

    // folder meta is decoupled from folder entries to prevent overwriting unsaved state when refreshing packs
    private final Map<String, FolderPackMeta> folderMeta = new ConcurrentHashMap<>();

    private volatile PackSelectionModel selectionModel;
    private volatile Map<String, PackNode> packs = Collections.emptyMap();
    private volatile Set<String> selectedPackIds = Collections.emptySet();

    public PackNodeRepository(PackRepository repository, Path packDir) {
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
                ignored -> PackNode.Leaf.DEFAULT_ICON,
                this.repository,
                FunctionUtils.nopConsumer()
        );
        ((PackSelectionModelAccessor) this.selectionModel).packed_packs$filterHidden(false);
    }

    private void loadFolderMetadata(PackNode entry) {
        // load metadata on first discover only to prevent overwriting unsaved state
        if (!(entry instanceof PackNode.Parent parent) || folderMeta.containsKey(entry.id())) {
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

    /**
     * @param includeModules <ul>
     *                       <li>{@link TriState#DEFAULT} - includes modules grouped</li>
     *                       <li>{@link TriState#TRUE} - includes modules and flattens children</li>
     *                       <li>{@link TriState#FALSE} - excludes modules and flattens children</li>
     *                       </ul>
     */
    public void collectDescendantLeaves(PackNode.Parent current, TriState includeModules, Consumer<PackNode> collector) {
        FolderPackMeta meta = Objects.requireNonNullElseGet(folderMeta.get(current.id()), FolderPackMeta::new);
        if (meta.module()) {
            if (includeModules == TriState.DEFAULT) {
                collector.accept(current);
                return;
            }
            if (includeModules == TriState.TRUE) {
                collector.accept(current);
            }
        }

        for (PackNode child : getSortedChildren(current)) {
            if (child instanceof PackNode.Leaf leaf) {
                collector.accept(leaf);
            }
            if (child instanceof PackNode.Parent inner) {
                collectDescendantLeaves(inner, includeModules, collector);
            }
        }
    }

    public void collectNodes(PackNode pack, Consumer<PackNode> collector) {
        switch (pack) {
            case PackNode.Leaf leaf -> collector.accept(leaf);
            case PackNode.Parent parent -> collectDescendantLeaves(parent, TriState.TRUE, collector);
        }
    }

    public void collectPacks(PackNode pack, Consumer<Pack> collector) {
        switch (pack) {
            case PackNode.Leaf leaf -> collector.accept(leaf.pack());
            case PackNode.Parent parent ->
                    collectDescendantLeaves(parent, TriState.FALSE, node -> node.visitPacks(collector));
        }
    }

    public Stream<PackNode> flattenNodes(PackNode pack) {
        Stream.Builder<PackNode> builder = Stream.builder();
        collectNodes(pack, builder::add);
        return builder.build();
    }

    public Stream<Pack> flattenPacks(PackNode pack) {
        Stream.Builder<Pack> builder = Stream.builder();
        collectPacks(pack, builder::add);
        return builder.build();
    }

    public PackNode.@Nullable Parent findAncestor(PackNode pack) {
        Map<String, PackNode> packs = this.packs;

        PackNode current = packs.get(pack.parentId());
        PackNode.Parent ancestor = null;

        while (current instanceof PackNode.Parent parent) {
            ancestor = parent;
            current = current.parentId() == null ? null : packs.get(current.parentId());
        }

        return ancestor;
    }

    public PackNode.@Nullable Parent findModuleAncestor(PackNode pack) {
        Map<String, PackNode> packs = this.packs;

        PackNode current = packs.get(pack.parentId());
        PackNode.Parent moduleAncestor = null;

        while (current instanceof PackNode.Parent parent) {
            FolderPackMeta meta = Objects.requireNonNullElseGet(folderMeta.get(current.id()), FolderPackMeta::new);

            if (meta.module()) {
                moduleAncestor = parent;
            }
            current = current.parentId() == null ? null : packs.get(current.parentId());
        }

        return moduleAncestor;
    }

    public void collectSortedChildren(PackNode.Parent parent, Consumer<PackNode> collector) {
        Map<String, PackNode> packs = this.packs;
        List<PackNode> children = parent.children();

        FolderPackMeta meta = Objects.requireNonNullElseGet(folderMeta.get(parent.id()), FolderPackMeta::new);

        List<String> orderedIds = meta.packIds();
        Set<String> seen = new ObjectOpenHashSet<>();

        for (String id : orderedIds) {
            PackNode pack = packs.get(id);
            if (pack != null && seen.add(pack.id())) {
                collector.accept(pack);
            }
        }

        for (PackNode pack : children) {
            if (seen.add(pack.id())) {
                collector.accept(pack);
            }
        }
    }

    public List<PackNode> getSortedChildren(PackNode.Parent parent) {
        List<PackNode> children = new ObjectArrayList<>(parent.children().size());
        collectSortedChildren(parent, children::add);
        return children;
    }

    private void refreshSelectedCache() {
        Map<String, PackNode> packs = this.packs;

        Collection<String> selectedLeaves = repository.getSelectedIds();
        Set<String> newSelected = new ObjectOpenHashSet<>(selectedLeaves.size());

        for (String id : selectedLeaves) {
            newSelected.add(id);

            PackNode pack = packs.get(id);
            if (pack != null) {
                PackNode.Parent moduleAncestor = findModuleAncestor(pack);
                // the entire module should be selected if any of its descendants are selected
                if (moduleAncestor != null && !newSelected.contains(moduleAncestor.id())) {
                    moduleAncestor.visitNodes(node -> newSelected.add(node.id()));
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
        if (PackedPacks.DEBUG) {
            PackedPacks.LOGGER.info("[packed_packs] ======== Pack Repository reloaded in {}ms ========", PackedPacks.duration(start));
        }

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

        Map<String, PackNode> newPacks = new Object2ObjectLinkedOpenHashMap<>();

        for (String id : leavesById.keySet()) {
            buildNode(id, leavesById, folderInfoById, folderChildIds, newPacks);
        }

        for (String id : folderInfoById.keySet()) {
            buildNode(id, leavesById, folderInfoById, folderChildIds, newPacks);
        }

        this.packs = newPacks;
        folderMeta.keySet().retainAll(newPacks.keySet());
        refreshSelectedCache();

        if (PackedPacks.DEBUG) {
            PackedPacks.LOGGER.info("[packed_packs] ======== Repository Sources Refreshed in {}ms ========", PackedPacks.duration(start));
        }
    }

    private PackNode buildNode(
            String id,
            Map<String, Pack> leavesById,
            Map<String, FolderLocationInfo> folderInfoById,
            Map<String, Set<String>> folderChildIds,
            Map<String, PackNode> newPacks
    ) {
        PackNode existing = newPacks.get(id);
        if (existing != null) return existing;

        Pack leafPack = leavesById.get(id);
        if (leafPack != null) {
            FolderLocationInfo parent = ((FilePack) leafPack).packed_packs$getParent();
            String parentId = parent == null ? null : parent.location().id();
            Path path = ((FilePack) leafPack).packed_packs$getPath();

            PackNode leaf = new PackNode.Leaf(leafPack, path, parentId);
            newPacks.put(id, leaf);
            return leaf;
        }

        FolderLocationInfo info = folderInfoById.get(id);
        List<PackNode> children = new ObjectArrayList<>();
        for (String childId : folderChildIds.getOrDefault(id, Set.of())) {
            children.add(buildNode(childId, leavesById, folderInfoById, folderChildIds, newPacks));
        }

        FolderLocationInfo parentInfo = info.parent();
        String parentId = parentInfo == null ? null : parentInfo.location().id();

        PackNode parent = new PackNode.Parent(
                parentId,
                info.path(),
                info.location(),
                new FolderResourcesSupplier(info.path()),
                children
        );

        loadFolderMetadata(parent);
        newPacks.put(id, parent);
        return parent;
    }

    public void setEnabledPacks(List<PackNode> packs) {
        MutableBoolean highContrast = new MutableBoolean(false);
        List<String> packIds = new ObjectArrayList<>();

        Consumer<Pack> collector = (pack) -> {
            if (!highContrast.booleanValue() && pack.getId().equals(PackUtil.HIGH_CONTRAST_ID)) {
                highContrast.setTrue();
            }
            packIds.add(pack.getId());
        };

        for (PackNode pack : packs.reversed()) {
            switch (pack) {
                case PackNode.Leaf leaf -> collector.accept(leaf.pack());
                case PackNode.Parent parent -> collectPacks(parent, collector);
            }
        }

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
    public List<PackNode> getPacks() {
        return List.copyOf(this.packs.values());
    }

    public @Nullable PackNode getPackById(String id) {
        return this.packs.get(id);
    }

    public List<PackNode> getEnabledPacks() {
        Map<String, PackNode> currentPacks = this.packs;
        return ((PackSelectionModelAccessor) this.selectionModel).getSelectedPacks()
                .stream()
                .map(pack -> currentPacks.get(pack.getId()))
                .filter(Objects::nonNull)
                .toList();
    }

    public List<PackNode> getPacksById(List<String> packIds) {
        return CollectionUtils.lookup(packIds, this.packs);
    }

    public @Nullable FolderPackMeta getFolderMetadata(String folderId) {
        return folderMeta.get(folderId);
    }

    public FolderPackMeta getFolderMetadata(PackNode.Parent parent) {
        return Objects.requireNonNullElseGet(folderMeta.get(parent.id()), FolderPackMeta::new);
    }

    public boolean setFolderMetadata(String folderId, FolderPackMeta metadata) {
        if (folderMeta.containsKey(folderId)) {
            FolderPackMeta old = folderMeta.put(folderId, metadata);
            return !Objects.equals(old, metadata);
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

        Map<String, PackNode> newPacks = new Object2ObjectLinkedOpenHashMap<>(this.packs);
        PackNode entry = newPacks.get(packId);
        if (entry != null) {
            entry.visitNodes(e -> {
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
