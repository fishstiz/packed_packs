package io.github.fishstiz.packed_packs.gui2.services;

import com.google.common.collect.ImmutableList;
import io.github.fishstiz.fidgetz.v0.utils.CollectionUtils;
import io.github.fishstiz.fidgetz.v0.utils.FunctionUtils;
import io.github.fishstiz.packed_packs.config.FolderPackMeta;
import io.github.fishstiz.packed_packs.config.JsonLoader;
import io.github.fishstiz.packed_packs.gui2.models.PackEntry;
import io.github.fishstiz.packed_packs.pack.PackIconManager;
import io.github.fishstiz.packed_packs.pack.folder.FolderLocationInfo;
import io.github.fishstiz.packed_packs.pack.folder.FolderResourcesSupplier;
import io.github.fishstiz.packed_packs.transform.interfaces.FilePack;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionModelAccessor;
import io.github.fishstiz.packed_packs.transform.mixin.folders.additional.FolderRepositorySourceAccessor;
import io.github.fishstiz.packed_packs.transform.mixin.folders.additional.PackRepositoryAccessor;
import io.github.fishstiz.packed_packs.util.PackUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PackRepositoryService {
    private final PackRepository repository;
    private final Path packDir;

    // folder meta is decoupled from folder entries to prevent overwriting unsaved state when refreshing packs
    private final Map<String, FolderPackMeta> folderMeta = new ConcurrentHashMap<>();

    private PackSelectionModel selectionModel;
    private Map<String, PackEntry> packs = Collections.emptyMap();
    private Set<String> selectedPackIds = Collections.emptySet();

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
                PackIconManager::getDefault,
                this.repository,
                FunctionUtils.nopConsumer()
        );
        ((PackSelectionModelAccessor) this.selectionModel).packed_packs$filterHidden(false);
    }

    private static void populatePacks(
            Collection<Pack> rawPacks,
            Map<String, PackEntry> packs,
            Map<FolderLocationInfo, List<PackEntry>> folders
    ) {
        for (Pack pack : rawPacks) {
            PackEntry entry;
            FilePack filePack = (FilePack) pack;
            FolderLocationInfo folderLocationInfo = filePack.packed_packs$getFolderLocationInfo();

            if (folderLocationInfo != null) {
                entry = new PackEntry.Leaf(pack, filePack.packed_packs$getPath(), folderLocationInfo.id());
                folders.computeIfAbsent(folderLocationInfo, _ -> new ObjectArrayList<>()).add(entry);
            } else {
                entry = new PackEntry.Leaf(pack, filePack.packed_packs$getPath(), null);
            }

            packs.put(pack.getId(), entry);
        }
    }

    private void refreshPacks() {
        Map<String, PackEntry> newPacks = new Object2ObjectLinkedOpenHashMap<>();
        Map<FolderLocationInfo, List<PackEntry>> folders = new Object2ObjectLinkedOpenHashMap<>();

        PackSelectionModelAccessor selectionModel = (PackSelectionModelAccessor) this.selectionModel;
        populatePacks(selectionModel.getUnselectedPacks(), newPacks, folders);
        populatePacks(selectionModel.getSelectedPacks(), newPacks, folders);

        // todo: remove, and add callback in repository and folder repository source on folder discovery
        // load metadata only when syncing the selected pack lists that contain a nested pack or when opening folder
        for (Map.Entry<FolderLocationInfo, List<PackEntry>> entry : folders.entrySet()) {
            FolderLocationInfo folderLocationInfo = entry.getKey();

            List<PackEntry> unsortedChildren = entry.getValue();
            List<PackEntry> sortedChildren = new ObjectArrayList<>(unsortedChildren.size());
            Set<PackEntry> seen = new ObjectOpenHashSet<>(unsortedChildren.size());

            Path folderPath = folderLocationInfo.path();
            FolderPackMeta folderMetadata = this.folderMeta.computeIfAbsent(
                    folderLocationInfo.id(),
                    _ -> JsonLoader.loadOrDefault(folderPath, FolderPackMeta.class, FolderPackMeta::new)
            );

            for (String childId : folderMetadata.packIds()) {
                PackEntry childEntry = newPacks.get(childId);
                if (childEntry != null && seen.add(childEntry)) {
                    sortedChildren.add(childEntry);
                }
            }

            for (PackEntry childEntry : unsortedChildren) {
                if (seen.add(childEntry)) {
                    sortedChildren.add(childEntry);
                }
            }

            newPacks.put(folderLocationInfo.id(), new PackEntry.Parent(
                    folderLocationInfo.path(),
                    folderLocationInfo.packLocationInfo(),
                    new FolderResourcesSupplier(folderLocationInfo.path()),
                    sortedChildren
            ));
        }

        this.packs = newPacks;
        this.selectedPackIds = Set.copyOf(this.repository.getSelectedIds());
    }

    public void refreshSources() {
        refreshSelectionModel();
        selectionModel.findNewPacks();
        refreshPacks();
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
    }

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

    public boolean isEnabled(String packId) {
        PackEntry entry = this.packs.get(packId);

        if (entry instanceof PackEntry.Parent parent) {
            FolderPackMeta metadata = folderMeta.get(parent.id());
            if (metadata != null && metadata.module()) {
                for (PackEntry child : parent.children()) {
                    if (this.selectedPackIds.contains(child.id())) {
                        return true;
                    }
                }
            }
        }

        return this.selectedPackIds.contains(packId);
    }

    public void removePack(String packId) {
        repository.removePack(packId);

        Map<String, PackEntry> newPacks = new Object2ObjectLinkedOpenHashMap<>(this.packs);
        PackEntry pack = newPacks.get(packId);
        if (pack != null) {
            pack.visitPacks(entry -> repository.removePack(entry.getId()));
        }
        this.packs = newPacks;

        refreshSelectionModel();
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
