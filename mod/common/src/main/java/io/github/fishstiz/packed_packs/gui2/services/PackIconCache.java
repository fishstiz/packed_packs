package io.github.fishstiz.packed_packs.gui2.services;

import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.gui2.models.PackEntry;
import io.github.fishstiz.packed_packs.util.PackUtil;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.Util;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class PackIconCache {
    private final Map<String, Identifier> cachedIcons = new ConcurrentHashMap<>();
    private final Executor mainThreadExecutor;
    private final TextureManager textureManager;
    private Map<String, Identifier> staleIcons;

    public PackIconCache(Executor mainThreadExecutor, TextureManager textureManager) {
        this.mainThreadExecutor = mainThreadExecutor;
        this.textureManager = textureManager;
    }

    public Identifier get(PackEntry pack) {
        if (!cachedIcons.containsKey(pack.id())) {
            Identifier fallback = (this.staleIcons != null) ? this.staleIcons.get(pack.id()) : null;
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
    private CompletableFuture<@Nullable Identifier> loadPackIcon(PackEntry pack) {
        return CompletableFuture.supplyAsync(() -> {
            try (PackResources packResources = pack.open()) {
                IoSupplier<@NonNull InputStream> iconIoSupplier = packResources.getRootResource(PackUtil.ICON_FILENAME);
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
            Identifier icon = Identifier.withDefaultNamespace(hashIconName(pack.id()));
            this.textureManager.register(icon, new DynamicTexture(icon::toString, nativeImage));
            return icon;
        }, this.mainThreadExecutor);
    }

    @SuppressWarnings("deprecation")
    private static String hashIconName(String id) {
        return "pack/" + Util.sanitizeName(id, Identifier::validPathChar) + "/" + Hashing.sha1().hashUnencodedChars(id) + "/icon";
    }
}
