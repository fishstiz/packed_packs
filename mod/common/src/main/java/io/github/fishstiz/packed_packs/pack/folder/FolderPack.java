package io.github.fishstiz.packed_packs.pack.folder;

import io.github.fishstiz.packed_packs.config.JsonLoader;
import io.github.fishstiz.packed_packs.config.FolderPackMeta;
import io.github.fishstiz.packed_packs.transform.interfaces.FilePack;
import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.fidgetz.util.lang.ObjectsUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.world.flag.FeatureFlagSet;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class FolderPack extends Pack implements FilePack {
    public static final Component FOLDER_OPEN_TEXT = ResourceUtil.getText("folder.open");
    public static final Component FOLDER_DESCRIPTION = ResourceUtil.getText("folder");
    public static final PackSelectionConfig FOLDER_SELECTION_CONFIG = new PackSelectionConfig(false, Position.TOP, false);
    public static final Metadata FOLDER_METADATA = new Metadata(FOLDER_DESCRIPTION, PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), Collections.emptyList());
    private final Function<FolderPack, List<Pack>> nestedPacksProvider;
    private final Path path;

    public FolderPack(String id, String name, Function<FolderPack, List<Pack>> nestedPacksProvider, Path path) {
        super(
                new PackLocationInfo(id, Component.literal(name), PackUtil.PACK_SOURCE, Optional.empty()),
                new FolderResourcesSupplier(path),
                FOLDER_METADATA,
                FOLDER_SELECTION_CONFIG
        );
        this.nestedPacksProvider = nestedPacksProvider;
        this.path = path;
    }

    public List<Pack> contents() {
        return ObjectsUtil.getOrDefault(this.nestedPacksProvider.apply(this), Collections.emptyList());
    }

    public List<Pack> flatten() {
        List<Pack> result = new ObjectArrayList<>();
        result.add(this);
        result.addAll(ObjectsUtil.getOrDefault(this.nestedPacksProvider.apply(this), Collections.emptyList()));
        return result;
    }

    public CompletableFuture<FolderPackMeta> loadConfig() {
        return CompletableFuture.supplyAsync(() -> {
            try (PackResources resources = this.open()) {
                var configIoSupplier = resources.getRootResource(FolderResources.FOLDER_CONFIG_FILENAME);
                if (configIoSupplier == null) {
                    throw new IOException();
                }
                try (InputStream inputStream = configIoSupplier.get()) {
                    return JsonLoader.loadJson(inputStream, FolderPackMeta.class);
                }
            } catch (NoSuchFileException e) {
                return ObjectsUtil.peek(new FolderPackMeta(), this::saveConfig);
            } catch (IOException e) {
                return new FolderPackMeta();
            }
        }, Util.backgroundExecutor());
    }

    public void saveConfig(FolderPackMeta folder) {
        if (folder != null) {
            folder.save(this.path.resolve(FolderResources.FOLDER_CONFIG_FILENAME));
        }
    }

    @Override
    public @Nullable Path packed_packs$getPath() {
        return this.path;
    }
}
