package io.github.fishstiz.packed_packs.util;

import io.github.fishstiz.packed_packs.PackedPacks;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

public class ResourceUtil {
    private ResourceUtil() {
    }

    public static MutableComponent getModName() {
        return Component.literal(PackedPacks.MOD_NAME);
    }

    public static MutableComponent getText(String keySuffix, Object... args) {
        return Component.translatable(PackedPacks.MOD_ID + "." + keySuffix, args);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(PackedPacks.MOD_ID, path);
    }

    public static Identifier getIcon(String icon) {
        return id("textures/gui/sprites/icon/").withSuffix(icon + ".png");
    }

    public static Identifier getVanillaSprite(String path) {
        return Identifier.withDefaultNamespace("textures/gui/sprites/" + path + ".png");
    }

    public static Identifier getVanilla(String path) {
        return Identifier.withDefaultNamespace(path);
    }
}
