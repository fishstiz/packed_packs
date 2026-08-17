package io.github.fishstiz.packed_packs.pack;

import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.util.PackUtil;
import net.minecraft.Util;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class PackIconCache {
    private final Map<String, ResourceLocation> cachedIcons = new ConcurrentHashMap<>();
    private final Executor mainThreadExecutor;
    private final TextureManager textureManager;
    private Map<String, ResourceLocation> staleIcons;

    public PackIconCache(Executor mainThreadExecutor, TextureManager textureManager) {
        this.mainThreadExecutor = mainThreadExecutor;
        this.textureManager = textureManager;
    }

    public ResourceLocation get(PackNode pack) {
        if (!cachedIcons.containsKey(pack.id())) {
            ResourceLocation fallback = (this.staleIcons != null) ? this.staleIcons.get(pack.id()) : null;
            if (fallback == null) {
                fallback = pack.defaultIcon();
            }

            cachedIcons.put(pack.id(), fallback);

            loadPackIcon(pack).thenAcceptAsync(icon -> {
                if (icon != null) {
                    cachedIcons.put(pack.id(), icon);
                }
            }, mainThreadExecutor);
        }

        return cachedIcons.getOrDefault(pack.id(), pack.defaultIcon());
    }

    public void clear() {
        this.staleIcons = Map.copyOf(cachedIcons);
        cachedIcons.clear();
    }

    /**
     * Refer to {@code PackSelectionScreen#loadPackIcon(TextureManager, Pack)}
     */
    private CompletableFuture<@Nullable ResourceLocation> loadPackIcon(PackNode pack) {
        return CompletableFuture.supplyAsync(() -> {
            try (PackResources packResources = pack.open()) {
                IoSupplier<InputStream> iconIoSupplier = packResources.getRootResource(PackUtil.ICON_FILENAME);
                if (iconIoSupplier == null) return null;

                try (InputStream iconStream = iconIoSupplier.get()) {
                    return NativeImage.read(iconStream);
                }
            } catch (Exception e) {
                if (!(e instanceof NoSuchFileException)) {
                    PackedPacks.LOGGER.warn("[packed_packs] Failed to load icon from pack '{}'", pack.id(), e);
                }
                return null;
            }
        }, Util.backgroundExecutor()).thenApplyAsync(nativeImage -> {
            if (nativeImage == null) return null;
            ResourceLocation icon = ResourceLocation.withDefaultNamespace(hashIconName(pack.id()));
            this.textureManager.register(icon, new DynamicTexture(nativeImage));
            return icon;
        }, this.mainThreadExecutor);
    }

    @SuppressWarnings("deprecation")
    private static String hashIconName(String id) {
        return "pack/" + Util.sanitizeName(id, ResourceLocation::validPathChar) + "/" + Hashing.sha1().hashUnencodedChars(id) + "/icon";
    }
}
