package io.github.fishstiz.packed_packs.pack;

import com.google.common.collect.ImmutableList;
import io.github.fishstiz.fidgetz.v0.utils.CollectionUtils;
import io.github.fishstiz.fidgetz.v0.utils.FunctionUtils;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.config.PackOptions;
import io.github.fishstiz.packed_packs.pack.folder.FolderLocationInfo;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
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
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;

public class PackRepositoryManager {
    private final PackRepository repository;
    private final Path packDir;
    private PackSelectionModel model;
    private Map<String, Pack> availablePacks = Collections.emptyMap();
    private Set<String> selectedPacksCache = Collections.emptySet();

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
        this.model = new PackSelectionModel(
                FunctionUtils.nop(),
                PackIconManager::getDefault,
                this.repository,
                FunctionUtils.nopConsumer()
        );
        ((PackSelectionModelAccessor) this.model).packed_packs$filterHidden(false);
    }

    public List<Pack> getPacks() {
        return List.copyOf(this.availablePacks.values());
    }

    public PackGroup getPacksBySelected(PackOptions options) {
        return this.validatePacks(this.groupByFolders(this.getUnselectedPacks()), this.groupByFolders(this.getSelectedPacks()), options);
    }

    public void removePack(Pack pack) {
        if (pack instanceof FolderPack folderPack) {
            folderPack.contents().forEach(this::removePack);
        }

        this.repository.removePack(pack.getId());

        if (!this.availablePacks.isEmpty()) {
            // available packs is immutable until populated
            // ideally should keep being immutable but probably not worth it when you have to rebuild the map on delete
            this.availablePacks.remove(pack.getId());
        }

        try {
            // PackSelectionModel#selected and PackSelectionModel#unselected are mutable in vanilla
            this.getSelectedPacks().remove(pack);
            this.getUnselectedPacks().remove(pack);
        } catch (UnsupportedOperationException e) {
            // just in case
            PackedPacks.LOGGER.warn("[packed_packs] Failed to mutate PackSelectionModel lists. Report this issue to mod author.");
        }
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
     * @param packIds grouped pack ids
     * @param source  grouped packs
     * @return grouped packs by id
     */
    public List<Pack> getPacksById(Collection<String> packIds, Map<String, Pack> source) {
        return CollectionUtils.lookup(packIds, source);
    }

    /**
     * @param packIds grouped pack ids
     * @param source  grouped packs
     * @return grouped packs by id
     */
    public List<Pack> getPacksById(Collection<String> packIds, Collection<Pack> source) {
        return CollectionUtils.lookup(packIds, CollectionUtils.toMap(source, Pack::getId));
    }

    /**
     * @param packIds grouped pack ids
     * @return grouped packs by id
     */
    public List<Pack> getPacksById(Collection<String> packIds) {
        return this.getPacksById(packIds, this.availablePacks);
    }

    public @Nullable Pack getPackById(String id) {
        return this.availablePacks.get(id);
    }

    public List<Pack> getPacksByFlattenedIds(Collection<String> packIds) {
        List<Pack> folderPacks = CollectionUtils.filter(this.availablePacks.values(), FolderPack.class::isInstance);
        List<Pack> available = CollectionUtils.addAll(folderPacks, this.repository.getAvailablePacks());
        return this.groupByFolders(this.getPacksById(packIds, available));
    }

    /**
     * @param packs ungrouped collection of packs
     */
    private void populateAvailablePacks(
            Collection<Pack> packs,
            Map<String, Pack> currentAvailable,
            Map<FolderLocationInfo, List<Pack>> folders
    ) {
        for (Pack pack : packs) {
            FolderLocationInfo folderLocationInfo = ((FilePack) pack).packed_packs$getFolderLocationInfo();
            if (folderLocationInfo != null) {
                folders.computeIfAbsent(folderLocationInfo, k -> new ObjectArrayList<>()).add(pack);
            } else {
                currentAvailable.put(pack.getId(), pack);
            }
        }
    }

    private void regenerateAvailablePacks() {
        this.selectedPacksCache = Set.copyOf(this.repository.getSelectedIds());

        Map<String, Pack> newAvailablePacks = new Object2ObjectLinkedOpenHashMap<>();
        Map<FolderLocationInfo, List<Pack>> folders = new Object2ObjectLinkedOpenHashMap<>();

        this.populateAvailablePacks(this.getSelectedPacks(), newAvailablePacks, folders);
        this.populateAvailablePacks(this.getUnselectedPacks(), newAvailablePacks, folders);

        for (Map.Entry<FolderLocationInfo, List<Pack>> folderEntry : folders.entrySet()) {
            FolderPack folderPack = FolderPack.createAndPreloadMetadata(folderEntry.getKey(), folderEntry.getValue());
            newAvailablePacks.putIfAbsent(folderPack.getId(), folderPack);
        }

        this.availablePacks = newAvailablePacks;
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

        OptionInstance<Boolean> highContrastOption = Minecraft.getInstance().options.highContrast();
        if (highContrastOption.get() != hasHighContrast) {
            highContrastOption.set(hasHighContrast);
        }

        this.refreshModel();
        this.selectedPacksCache = Set.copyOf(this.repository.getSelectedIds());
    }

    /**
     * @param flatPacks ungrouped list of packs
     * @return grouped list of packs
     */
    private List<Pack> groupByFolders(List<Pack> flatPacks) {
        Set<String> seenFolders = new ObjectOpenHashSet<>();
        List<Pack> grouped = new ObjectArrayList<>(flatPacks.size());

        for (Pack pack : flatPacks) {
            FolderLocationInfo folderLocationInfo = ((FilePack) pack).packed_packs$getFolderLocationInfo();
            if (folderLocationInfo != null) {
                Pack folder = this.availablePacks.get(folderLocationInfo.id());
                if (folder instanceof FolderPack valid && seenFolders.add(valid.getId())) {
                    grouped.add(this.availablePacks.get(valid.getId()));
                }
            } else {
                grouped.add(pack);
            }
        }

        return grouped;
    }

    public boolean isEnabled(Pack pack) {
        if (pack instanceof FolderPack folderPack) {
            for (Pack nestedPack : folderPack.contents()) {
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
}
