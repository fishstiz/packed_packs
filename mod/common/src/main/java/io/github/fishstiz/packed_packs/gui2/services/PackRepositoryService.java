package io.github.fishstiz.packed_packs.gui2.services;

import com.google.common.collect.ImmutableList;
import io.github.fishstiz.fidgetz.v0.utils.CollectionUtils;
import io.github.fishstiz.fidgetz.v0.utils.FunctionUtils;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.config.FolderPackMeta;
import io.github.fishstiz.packed_packs.config.JsonLoader;
import io.github.fishstiz.packed_packs.models.PackEntry;
import io.github.fishstiz.packed_packs.pack.PackIconManager;
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
import net.minecraft.server.packs.PackResources;
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

    private void loadFolderMetadata(PackEntry entry) {
        // load metadata on first discover only to prevent overwriting unsaved state
        if (!(entry instanceof PackEntry.Parent parent) || folderMeta.containsKey(entry.id())) {
            return;
        }

        // todo sort children on open in GUI only
        try (PackResources resources = parent.open()) {
            IoSupplier<InputStream> streamSupplier = resources.getRootResource(FolderPackMeta.FILENAME);
            if (streamSupplier != null) {
                try (InputStream stream = streamSupplier.get()) {
                    folderMeta.put(
                            entry.id(),
                            JsonLoader.loadOrDefault(stream, FolderPackMeta.class, FolderPackMeta::new)
                    );
                } catch (Exception e) {
                    PackedPacks.LOGGER.error("[packed_packs] Failed to read folder metadata at {}", entry.path(), e);
                }
            }
        }
    }

    private void addParentsRecursively(String id, Collection<String> collection) {
        PackEntry entry = this.packs.get(id);
        if (entry instanceof PackEntry.Parent parent) {
            FolderPackMeta meta = folderMeta.getOrDefault(parent.id(), new FolderPackMeta());
            if (meta.module()) {
                collection.add(parent.id());
                String parentId = parent.parentId();
                if (parentId != null) {
                    addParentsRecursively(parentId, collection);
                }
            }
        }
    }

    private void refreshSelectedCache() {
        Collection<String> selectedLeaves = repository.getSelectedIds();
        Set<String> newSelected = new ObjectOpenHashSet<>(selectedLeaves.size());

        for (String id : selectedLeaves) {
            newSelected.add(id);
            addParentsRecursively(id, newSelected);
        }

        this.selectedPackIds = newSelected;
    }

    public void refreshSources() {
        refreshSelectionModel();
        Map<String, PackEntry> newPacks = new Object2ObjectLinkedOpenHashMap<>();
        ScopedValue.where(PackEntry.ENTRY_CALLBACK, entry -> {
                    // ids are based on file names so its unreliable, preserve only the first ones
                    newPacks.putIfAbsent(entry.id(), entry);
                    loadFolderMetadata(entry);
                })
                .run(selectionModel::findNewPacks);
        this.packs = newPacks;
        refreshSelectedCache();
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

    public void setFolderMetadata(String folderId, FolderPackMeta metadata) {
        if (folderMeta.containsKey(folderId)) {
            folderMeta.put(folderId, metadata);
        } else {
            PackedPacks.LOGGER.warn("[packed_packs] Tried to update folder metadata from non-existing folder '{}'", folderId);
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
