package io.github.fishstiz.testmod.pack;

import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.flag.FeatureFlagSet;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class TestPackFactory {
    public static final Pack.ResourcesSupplier DEFAULT_RESOURCES_SUPPLIER = new EmptyPackResourcesSupplier();
    public static final PackSelectionConfig DEFAULT_SELECTION_CONFIG = new PackSelectionConfig(false, Pack.Position.TOP, false);

    public static PackLocationInfo createPackLocationInfo(String id, Component title) {
        return new PackLocationInfo(id, title, PackSource.DEFAULT, Optional.empty());
    }

    public static Pack.Metadata createPackMetadata(Component description, boolean compatible) {
        PackCompatibility compatibility;
        if (compatible) {
            compatibility = PackCompatibility.COMPATIBLE;
        } else {
            compatibility = ThreadLocalRandom.current().nextBoolean() ? PackCompatibility.TOO_OLD : PackCompatibility.TOO_NEW;
        }
        return new Pack.Metadata(description, compatibility, FeatureFlagSet.of(), Collections.emptyList());
    }

    public static Pack createPack(String id, Component title, Component description, boolean compatible) {
        return new Pack(createPackLocationInfo(id, title), DEFAULT_RESOURCES_SUPPLIER, createPackMetadata(description, compatible), DEFAULT_SELECTION_CONFIG);
    }

    private TestPackFactory() {
    }
}
