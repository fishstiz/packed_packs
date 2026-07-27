package io.github.fishstiz.packed_packs.pack.folder;

import io.github.fishstiz.packed_packs.util.PackUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public record FolderResources(PackLocationInfo location, Path path) implements PackResources {
    public static final String FOLDER_CONFIG_FILENAME = "packed_packs.folderpack.json";

    @Override
    public @Nullable IoSupplier<InputStream> getRootResource(String... elements) {
        if (elements.length > 0) {
            if (Objects.equals(elements[0], PackUtil.ICON_FILENAME)) {
                return () -> Files.newInputStream(this.path.resolve(PackUtil.ICON_FILENAME));
            } else if (Objects.equals(elements[0], FOLDER_CONFIG_FILENAME)) {
                return () -> Files.newInputStream(this.path.resolve(FOLDER_CONFIG_FILENAME));
            }
        }
        return null;
    }

    @Override
    public @Nullable IoSupplier<InputStream> getResource(PackType packType, ResourceLocation location) {
        return null;
    }

    @Override
    public void listResources(PackType packType, String namespace, String path, ResourceOutput resourceOutput) {
        // no-op
    }

    @Override
    public @NotNull Set<String> getNamespaces(PackType type) {
        return Collections.emptySet();
    }

    @Override
    public @Nullable <T> T getMetadataSection(MetadataSectionType<T> type) {
        return null;
    }

    @Override
    public void close() {
        // no-op
    }
}