package io.github.fishstiz.packed_packs.pack;

import io.github.fishstiz.packed_packs.config.FolderPackMeta;
import io.github.fishstiz.packed_packs.util.PackUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public record FolderResources(PackLocationInfo location, Path path) implements PackResources {
    @Override
    public @Nullable IoSupplier<InputStream> getRootResource(String... elements) {
        if (elements.length > 0) {
            if (Objects.equals(elements[0], PackUtil.ICON_FILENAME)) {
                return getRootResource(path.resolve(PackUtil.ICON_FILENAME));
            } else if (Objects.equals(elements[0], FolderPackMeta.FILENAME)) {
                return getRootResource(path.resolve(FolderPackMeta.FILENAME));
            }
        }
        return null;
    }

    private @Nullable IoSupplier<InputStream> getRootResource(Path path) {
        return Files.exists(path, LinkOption.NOFOLLOW_LINKS) ? () -> Files.newInputStream(path) : null;
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
    public @Nullable <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) {
        return null;
    }

    @Override
    public void close() {
        // no-op
    }
}