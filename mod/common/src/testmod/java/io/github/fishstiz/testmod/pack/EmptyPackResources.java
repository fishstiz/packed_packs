package io.github.fishstiz.testmod.pack;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

public record EmptyPackResources(PackLocationInfo location) implements PackResources {
    @Override
    public @Nullable IoSupplier<InputStream> getRootResource(String... elements) {
        return null;
    }

    @Override
    public @Nullable IoSupplier<InputStream> getResource(PackType packType, ResourceLocation location) {
        return null;
    }

    @Override
    public void listResources(PackType packType, String namespace, String path, ResourceOutput resourceOutput) {
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return Collections.emptySet();
    }

    @Override
    public @Nullable <T> T getMetadataSection(MetadataSectionType<T> metadataSectionType) {
        return null;
    }

    @Override
    public void close() {
    }
}
