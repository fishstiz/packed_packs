package io.github.fishstiz.packed_packs.pack.folder;

import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.config.JsonLoader;
import io.github.fishstiz.packed_packs.config.FolderPackMeta;
import io.github.fishstiz.packed_packs.transform.interfaces.FilePack;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.world.flag.FeatureFlagSet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class FolderPack extends Pack implements FilePack {
    public static final Component FOLDER_OPEN_TEXT = Component.translatable("packed_packs.folder.open");
    public static final Component FOLDER_DESCRIPTION = Component.translatable("packed_packs.folder");
    public static final PackSelectionConfig FOLDER_SELECTION_CONFIG = new PackSelectionConfig(false, Position.TOP, false);
    public static final Metadata FOLDER_METADATA = new Metadata(FOLDER_DESCRIPTION, PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), Collections.emptyList());
    private final CompletableFuture<FolderPackMeta> folderPackMetaFuture;
    private CompletableFuture<List<Pack>> orderedContentsFuture;
    private final Path path;

    private FolderPack(
            PackLocationInfo locationInfo,
            FolderResourcesSupplier resourcesSupplier,
            CompletableFuture<FolderPackMeta> folderPackMetaFuture,
            CompletableFuture<List<Pack>> orderedContentsFuture
    ) {
        super(locationInfo, resourcesSupplier, FOLDER_METADATA, FOLDER_SELECTION_CONFIG);
        this.folderPackMetaFuture = folderPackMetaFuture;
        this.orderedContentsFuture = orderedContentsFuture;
        this.path = resourcesSupplier.path();
    }

    public FolderPack(FolderLocationInfo locationInfo, FolderPackMeta folderPackMeta, List<Pack> contents) {
        this(locationInfo.packLocationInfo(), new FolderResourcesSupplier(locationInfo.path()),
                CompletableFuture.completedFuture(folderPackMeta), CompletableFuture.completedFuture(contents));
    }

    public static FolderPack createAndPreloadMetadata(FolderLocationInfo folderLocationInfo, List<Pack> contents) {
        PackLocationInfo locationInfo = folderLocationInfo.packLocationInfo();
        FolderResourcesSupplier resourcesSupplier = new FolderResourcesSupplier(folderLocationInfo.path());
        CompletableFuture<FolderPackMeta> folderPackMetaFuture = CompletableFuture.supplyAsync(() -> {
            try (PackResources resources = resourcesSupplier.openFull(locationInfo, FOLDER_METADATA)) {
                var configIoSupplier = resources.getRootResource(FolderResources.FOLDER_CONFIG_FILENAME);
                if (configIoSupplier == null) {
                    throw new RuntimeException("FolderPack does not supply metadata");
                }
                try (InputStream inputStream = configIoSupplier.get()) {
                    return JsonLoader.loadOrDefault(inputStream, FolderPackMeta.class, FolderPackMeta::new);
                }
            } catch (IOException e) {
                if (!(e instanceof NoSuchFileException)) {
                    PackedPacks.LOGGER.error("[packed_packs] Failed to load folder pack metadata from '{}'", locationInfo.id(), e);
                }
                return new FolderPackMeta();
            }
        }, Util.backgroundExecutor());
        CompletableFuture<List<Pack>> orderedContentsFuture = folderPackMetaFuture.thenApply(metadata -> {
            Map<String, Pack> contentById = new Object2ObjectOpenHashMap<>(contents.size(), 0.99f);
            for (Pack pack : contents) {
                contentById.put(pack.getId(), pack);
            }

            List<String> orderedIds = metadata.getPackIds();
            Set<Pack> seen = new ObjectOpenHashSet<>();
            List<Pack> ordered = new ObjectArrayList<>(contents.size());

            for (String id : orderedIds) {
                Pack pack = contentById.get(id);
                if (pack != null && seen.add(pack)) ordered.add(pack);
            }

            for (Pack pack : contents) {
                if (seen.add(pack)) ordered.add(pack);
            }

            return new ObjectImmutableList<>(ordered);
        });
        return new FolderPack(locationInfo, resourcesSupplier, folderPackMetaFuture, orderedContentsFuture);
    }

    public FolderPackMeta folderMetadata() {
        return this.folderPackMetaFuture.join();
    }

    public List<Pack> contents() {
        return this.orderedContentsFuture.join();
    }

    public List<Pack> flatten() {
        List<Pack> contents = this.contents();
        List<Pack> result = new ObjectArrayList<>(contents.size() + 1);
        result.add(this);
        result.addAll(contents);
        return result;
    }

    public void setContents(List<Pack> contents) {
        FolderPackMeta metadata = this.folderMetadata();
        if (metadata.trySetPacks(contents)) {
            metadata.save(this.path.resolve(FolderResources.FOLDER_CONFIG_FILENAME));
            this.orderedContentsFuture = CompletableFuture.completedFuture(new ObjectImmutableList<>(contents));
        }
    }

    @Override
    public Path packed_packs$getPath() {
        return this.path;
    }
}
