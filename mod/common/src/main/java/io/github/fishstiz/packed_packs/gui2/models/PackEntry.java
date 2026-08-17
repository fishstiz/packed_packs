package io.github.fishstiz.packed_packs.gui2.models;

import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.transform.interfaces.ConfiguredPack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.flag.FeatureFlagSet;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public sealed interface PackEntry {
    String id();

    @Nullable String parentId();

    Component title();

    Pack.Metadata metadata();

    PackCompatibility compatibility();

    PackSelectionConfig selectionConfig();

    PackSource packSource();

    @Nullable Path path();

    default Component decoratedDescription() {
        return packSource().decorate(metadata().description());
    }

    Identifier defaultIcon();

    PackResources open();

    void visitEntries(Consumer<PackEntry> visitor);

    void visitPacks(Consumer<Pack> visitor);

    record Parent(
            Path path,
            PackLocationInfo location,
            Pack.ResourcesSupplier resourcesSupplier,
            List<PackEntry> children
    ) implements PackEntry {
        public static final Identifier DEFAULT_ICON = PackedPacks.id("textures/misc/unknown_folder.png");
        public static final Component DESCRIPTION = Component.translatable("packed_packs.folder");
        public static final PackSelectionConfig SELECTION_CONFIG = new PackSelectionConfig(false, Pack.Position.TOP, false);
        public static final Pack.Metadata METADATA = new Pack.Metadata(
                DESCRIPTION,
                PackCompatibility.COMPATIBLE,
                FeatureFlagSet.of(),
                Collections.emptyList()
        );

        public Parent {
            Objects.requireNonNull(path, "path cannot be null");
            children = List.copyOf(children);
        }

        @Override
        public String id() {
            return location.id();
        }

        @Override
        public @Nullable String parentId() {
            return null;
        }

        @Override
        public Component title() {
            return location.title();
        }

        @Override
        public Pack.Metadata metadata() {
            return METADATA;
        }

        @Override
        public PackCompatibility compatibility() {
            return METADATA.compatibility();
        }

        @Override
        public PackSelectionConfig selectionConfig() {
            return SELECTION_CONFIG;
        }

        @Override
        public PackSource packSource() {
            return location.source();
        }

        @Override
        public Identifier defaultIcon() {
            return DEFAULT_ICON;
        }

        @Override
        public PackResources open() {
            return resourcesSupplier.openFull(location, metadata());
        }

        @Override
        public void visitEntries(Consumer<PackEntry> visitor) {
            visitor.accept(this);
            children.forEach(child -> child.visitEntries(visitor));
        }

        @Override
        public void visitPacks(Consumer<Pack> visitor) {
            children.forEach(child -> child.visitPacks(visitor));
        }

        @Override
        public int hashCode() {
            return location.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || (obj instanceof Parent other && location.equals(other.location));
        }
    }

    record Leaf(Pack pack, @Nullable Path path, @Nullable String parentId) implements PackEntry {
        public static final Identifier DEFAULT_ICON = Identifier.withDefaultNamespace("textures/misc/unknown_pack.png");

        @Override
        public String id() {
            return pack.getId();
        }

        @Override
        public Component title() {
            return pack.getTitle();
        }

        @Override
        public Pack.Metadata metadata() {
            return ((ConfiguredPack) pack).packed_packs$getMetadata();
        }

        @Override
        public PackCompatibility compatibility() {
            return pack.getCompatibility();
        }

        @Override
        public PackSelectionConfig selectionConfig() {
            return ((ConfiguredPack) pack).packed_packs$originalConfig();
        }

        @Override
        public PackSource packSource() {
            return pack.getPackSource();
        }

        @Override
        public Identifier defaultIcon() {
            return DEFAULT_ICON;
        }

        @Override
        public PackResources open() {
            return pack.open();
        }

        @Override
        public void visitEntries(Consumer<PackEntry> visitor) {
            visitor.accept(this);
        }

        @Override
        public void visitPacks(Consumer<Pack> visitor) {
            visitor.accept(pack);
        }

        @Override
        public int hashCode() {
            return pack.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || (obj instanceof Leaf other && pack.equals(other.pack));
        }
    }
}
