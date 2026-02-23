package io.github.fishstiz.packed_packs.util;

import io.github.fishstiz.packed_packs.PackedPacks;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public class ResourceUtil {
    private ResourceUtil() {
    }

    public static MutableComponent getModName() {
        return Component.literal(PackedPacks.MOD_NAME);
    }

    public static MutableComponent getText(String keySuffix, Object... args) {
        return Component.translatable(PackedPacks.MOD_ID + "." + keySuffix, args);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(PackedPacks.MOD_ID, path);
    }

    public static ResourceLocation getIcon(String icon) {
        return id("textures/gui/sprites/icon/").withSuffix(icon + ".png");
    }

    public static ResourceLocation getVanillaSprite(String path) {
        return ResourceLocation.withDefaultNamespace("textures/gui/sprites/" + path + ".png");
    }

    public static ResourceLocation getVanilla(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }
}
