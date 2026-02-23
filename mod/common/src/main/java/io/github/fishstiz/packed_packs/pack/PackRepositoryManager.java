package io.github.fishstiz.packed_packs.pack;

import com.google.common.collect.ImmutableList;
import io.github.fishstiz.fidgetz.util.lang.FunctionsUtil;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.config.FolderPackMeta;
import io.github.fishstiz.packed_packs.config.PackOptions;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import io.github.fishstiz.packed_packs.transform.interfaces.FilePack;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionModelAccessor;
import io.github.fishstiz.packed_packs.transform.mixin.folders.additional.FolderRepositorySourceAccessor;
import io.github.fishstiz.packed_packs.transform.mixin.folders.additional.PackRepositoryAccessor;
import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.fidgetz.util.lang.CollectionsUtil;
import io.github.fishstiz.fidgetz.util.lang.ObjectsUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class PackRepositoryManager {
    private final Map<String, Pack> availablePacks = new Object2ObjectLinkedOpenHashMap<>();
    private final Map<String, List<Pack>> folderPacks = new Object2ObjectOpenHashMap<>();
    private final Map<String, CompletableFuture<FolderPackMeta>> folderConfigs = new Object2ObjectOpenHashMap<>();
    private final Set<String> selectedPacksCache = new ObjectOpenHashSet<>();
    private final PackRepository repository;
    private final Path packDir;
    private PackSelectionModel model;

    public PackRepositoryManager(PackRepository repository, Path packDir) {
        this.repository = repository;
        this.packDir = packDir;
        this.refreshModel();
        this.regenerateAvailablePacks();
    }

    public PackRepository getRepository() {
        return this.repository;
    }

    private List<Pack> getSelectedPacks() {
        return ((PackSelectionModelAccessor) this.model).getSelectedPacks();
    }

    private List<Pack> getUnselectedPacks() {
        return ((PackSelectionModelAccessor) this.model).getUnselectedPacks();
    }

    private void refreshModel() {
        this.model = new PackSelectionModel(FunctionsUtil.nopConsumer(), PackAssetManager::getDefaultLocation, this.repository, FunctionsUtil.nopConsumer());
        ((PackSelectionModelAccessor) this.model).packed_packs$filterHidden(false);
    }

    public List<Pack> getPacks() {
        return List.copyOf(this.availablePacks.values());
    }

    public PackGroup getPacksBySelected(PackOptions options) {
        return this.validateAndGroupPacks(this.getUnselectedPacks(), this.getSelectedPacks(), options);
    }

    public void removePack(Pack pack) {
        if (pack instanceof FolderPack) {
            ObjectsUtil.ifPresent(this.folderPacks.get(pack.getId()), packs -> packs.forEach(this::removePack));
            this.folderPacks.remove(pack.getId());
            this.folderConfigs.remove(pack.getId());
        }

        this.repository.removePack(pack.getId());
        this.availablePacks.remove(pack.getId());

        try {
            this.getSelectedPacks().remove(pack);
            this.getUnselectedPacks().remove(pack);
        } catch (UnsupportedOperationException e) {
            // just in case
            PackedPacks.LOGGER.warn("[packed_packs] Failed to mutate PackSelectionModel lists. Report this issue to mod author.");
        }
    }

    /**
     * @param unselected ungrouped list of unselected packs
     * @param selected   ungrouped list of selected packs
     * @return validated and grouped list of packs
     */
    public PackGroup validateAndGroupPacks(List<Pack> unselected, List<Pack> selected, PackOptions options) {
        return this.validatePacks(this.groupByFolders(unselected), this.groupByFolders(selected), options);
    }

    /**
     * @param unselected grouped list of unselected packs
     * @param selected   grouped list of selected packs
     * @return validated and grouped list of packs
     */
    public PackGroup validatePacks(List<Pack> unselected, List<Pack> selected, PackOptions options) {
        return PackUtil.syncPackSelection(new ObjectOpenHashSet<>(this.availablePacks.values()), unselected, selected, options);
    }

    /**
     * @param folderPack   the folder pack
     * @param orderedPacks the nested packs that define the preferred order
     * @return a validated and ordered list of all packs under the folder pack
     */
    public List<Pack> validateAndOrderNestedPacks(FolderPack folderPack, List<Pack> orderedPacks) {
        Set<Pack> seen = new ObjectOpenHashSet<>();
        List<Pack> orderedValidPacks = this.folderPacks.get(folderPack.getId());
        ObjectOpenHashSet<Pack> validPacks = new ObjectOpenHashSet<>(orderedValidPacks);
        List<Pack> finalOrderedPacks = new ObjectArrayList<>(validPacks.size());

        for (Pack pack : orderedPacks) {
            Pack validPack = validPacks.get(pack); // metadata can change
            if (validPack != null && (seen.add(pack))) {
                finalOrderedPacks.add(validPack);
            }
        }

        for (Pack validPack : orderedValidPacks) {
            if (seen.add(validPack)) {
                finalOrderedPacks.add(validPack);
            }
        }

        this.folderPacks.put(folderPack.getId(), finalOrderedPacks);
        return finalOrderedPacks;
    }

    /**
     * @param folderPack   the folder pack
     * @param orderedPacks the nested pack ids that define the preferred order
     * @return a validated and ordered list of all packs under the folder pack
     */
    public List<Pack> validateAndOrderNestedPackIds(FolderPack folderPack, List<String> orderedPacks) {
        return this.validateAndOrderNestedPacks(folderPack, this.getPacksById(orderedPacks, this.folderPacks.get(folderPack.getId())));
    }

    /**
     * @param packIds grouped pack ids
     * @param source  grouped packs
     * @return grouped packs by id
     */
    public List<Pack> getPacksById(Collection<String> packIds, Map<String, Pack> source) {
        return CollectionsUtil.lookup(packIds, source);
    }

    /**
     * @param packIds grouped pack ids
     * @param source  grouped packs
     * @return grouped packs by id
     */
    public List<Pack> getPacksById(Collection<String> packIds, Collection<Pack> source) {
        return CollectionsUtil.lookup(packIds, CollectionsUtil.toMap(source, Pack::getId));
    }

    /**
     * @param packIds grouped pack ids
     * @return grouped packs by id
     */
    public List<Pack> getPacksById(Collection<String> packIds) {
        return this.getPacksById(packIds, this.availablePacks);
    }

    public List<Pack> getPacksByFlattenedIds(Collection<String> packIds) {
        List<Pack> folderPacks = CollectionsUtil.filter(this.availablePacks.values(), FolderPack.class::isInstance, ObjectArrayList::new);
        List<Pack> available = CollectionsUtil.addAll(folderPacks, this.repository.getAvailablePacks());
        return this.groupByFolders(this.getPacksById(packIds, available));
    }

    /**
     * @param packs ungrouped collection of packs
     */
    private void populateAvailablePacks(Collection<Pack> packs) {
        for (Pack pack : packs) {
            FilePack filePack = (FilePack) pack;
            if (filePack.packed_packs$nestedPack()) {
                Path folderPath = Objects.requireNonNull(filePack.packed_packs$getPath()).getParent();
                String folderName = PackUtil.generatePackName(folderPath);
                String folderId = PackUtil.generatePackId(folderName);
                if (!this.availablePacks.containsKey(folderId)) {
                    FolderPack folderPack = new FolderPack(folderId, folderName, this::getNestedPacks, folderPath);
                    this.folderConfigs.put(folderId, folderPack.loadConfig());
                    this.availablePacks.put(folderId, folderPack);
                }
                this.folderPacks.computeIfAbsent(folderId, id -> new ObjectArrayList<>()).add(pack);
            } else {
                this.availablePacks.put(pack.getId(), pack);
            }
        }
    }

    private void regenerateAvailablePacks() {
        this.selectedPacksCache.clear();
        this.availablePacks.clear();
        this.folderPacks.clear();
        this.folderConfigs.clear();
        this.selectedPacksCache.addAll(this.repository.getSelectedIds());
        this.populateAvailablePacks(this.getSelectedPacks());
        this.populateAvailablePacks(this.getUnselectedPacks());
    }

    public void refresh() {
        this.refreshModel();
        this.model.findNewPacks();
        this.regenerateAvailablePacks();
    }

    /**
     * @param selected grouped list of selected packs
     */
    public void selectPacks(List<Pack> selected) {
        boolean hasHighContrast = false;
        List<String> packIds = new ObjectArrayList<>();

        for (Pack pack : PackUtil.flattenPacks(selected).reversed()) {
            String packId = pack.getId();

            packIds.add(packId);
            if (!hasHighContrast && packId.equals(PackUtil.HIGH_CONTRAST_ID)) {
                hasHighContrast = true;
            }
        }

        this.repository.setSelected(ImmutableList.copyOf(packIds));

        OptionInstance<@NonNull Boolean> highContrastOption = Minecraft.getInstance().options.highContrast();
        if (highContrastOption.get() != hasHighContrast) {
            highContrastOption.set(hasHighContrast);
        }

        this.refreshModel();
        this.selectedPacksCache.clear();
        this.selectedPacksCache.addAll(this.repository.getSelectedIds());
    }

    /**
     * @param flatPacks ungrouped list of packs
     * @return grouped list of packs
     */
    public List<Pack> groupByFolders(List<Pack> flatPacks) {
        if (this.folderPacks.isEmpty()) return flatPacks;

        Map<String, String> packToFolder = new Object2ObjectOpenHashMap<>();
        for (Map.Entry<String, List<Pack>> entry : this.folderPacks.entrySet()) {
            for (Pack pack : entry.getValue()) {
                packToFolder.put(pack.getId(), entry.getKey());
            }
        }

        Set<String> seenFolders = new ObjectOpenHashSet<>();
        List<Pack> grouped = new ObjectArrayList<>(flatPacks.size());

        for (Pack pack : flatPacks) {
            String folderId = packToFolder.get(pack.getId());
            if (folderId != null) {
                if (seenFolders.add(folderId)) {
                    grouped.add(this.availablePacks.get(folderId));
                }
            } else {
                grouped.add(pack);
            }
        }
        return grouped;
    }

    public boolean isEnabled(Pack pack) {
        if (pack instanceof FolderPack folderPack) {
            for (Pack nestedPack : this.folderPacks.get(folderPack.getId())) {
                if (this.selectedPacksCache.contains(nestedPack.getId())) {
                    return true;
                }
            }
            return false;
        }

        return this.selectedPacksCache.contains(pack.getId());
    }

    public Path getBaseDir() {
        return this.packDir;
    }

    public List<Path> getAdditionalDirs() {
        Path normalizedBaseDir = this.getBaseDir().toAbsolutePath().normalize();

        return ((PackRepositoryAccessor) this.repository).packed_packs$getSources().stream()
                .filter(FolderRepositorySourceAccessor.class::isInstance)
                .map(source -> ((FolderRepositorySourceAccessor) source).packed_packs$getFolder().toAbsolutePath().normalize())
                .filter(path -> !path.equals(normalizedBaseDir))
                .distinct()
                .toList();
    }

    public @Nullable FolderPackMeta getFolderConfig(@Nullable FolderPack folderPack) {
        if (folderPack == null) return null;
        CompletableFuture<FolderPackMeta> future = this.folderConfigs.get(folderPack.getId());
        return future != null ? future.join() : null;
    }

    public List<Pack> getNestedPacks(FolderPack folderPack) {
        FolderPackMeta config = this.getFolderConfig(folderPack);
        if (config == null) return Collections.emptyList();
        return this.validateAndOrderNestedPackIds(folderPack, config.getPackIds());
    }
}
