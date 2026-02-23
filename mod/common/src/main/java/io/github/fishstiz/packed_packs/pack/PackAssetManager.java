package io.github.fishstiz.packed_packs.pack;

import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class PackAssetManager {
    public static final Sprite DEFAULT_FOLDER_ICON = Sprite.of16(ResourceUtil.id("textures/misc/unknown_folder.png"));
    public static final Sprite DEFAULT_ICON = Sprite.of16(ResourceLocation.withDefaultNamespace("textures/misc/unknown_pack.png"));
    private final Map<String, Sprite> cachedIcons = new Object2ObjectOpenHashMap<>();
    private final Executor mainThreadExecutor;
    private final TextureManager textureManager;
    private Map<String, Sprite> staleIcons;

    public PackAssetManager(Executor mainThreadExecutor, TextureManager textureManager) {
        this.mainThreadExecutor = mainThreadExecutor;
        this.textureManager = textureManager;
    }

    public Sprite getIcon(Pack pack) {
        if (!this.cachedIcons.containsKey(pack.getId())) {
            Sprite fallback = (this.staleIcons != null) ? this.staleIcons.get(pack.getId()) : null;
            if (fallback == null) {
                fallback = getDefaultIcon(pack);
            }
            this.cachedIcons.put(pack.getId(), fallback);
            this.loadPackIcon(pack).thenAcceptAsync(icon -> {
                if (icon != null) {
                    this.cachedIcons.put(pack.getId(), Sprite.of16(icon));
                }
            }, this.mainThreadExecutor);
        }

        return this.cachedIcons.getOrDefault(pack.getId(), getDefaultIcon(pack));
    }

    public void clearIconCache() {
        this.staleIcons = new Object2ObjectOpenHashMap<>(this.cachedIcons);
        this.cachedIcons.clear();
    }

    public static Sprite getDefaultIcon(Pack pack) {
        return pack instanceof FolderPack ? DEFAULT_FOLDER_ICON : DEFAULT_ICON;
    }

    public static ResourceLocation getDefaultLocation(Pack pack) {
        return getDefaultIcon(pack).location;
    }

    /**
     * Copied from {@link PackSelectionScreen#loadPackIcon(TextureManager, Pack)}
     */
    private CompletableFuture<@Nullable ResourceLocation> loadPackIcon(Pack pack) {
        return CompletableFuture.supplyAsync(() -> {
            try (PackResources packResources = pack.open()) {
                IoSupplier<InputStream> iconIoSupplier = packResources.getRootResource(PackUtil.ICON_FILENAME);
                if (iconIoSupplier == null) return null;

                try (InputStream iconStream = iconIoSupplier.get()) {
                    return NativeImage.read(iconStream);
                }
            } catch (Exception e) {
                if (!(e instanceof NoSuchFileException)) {
                    PackedPacks.LOGGER.warn("Failed to load icon from pack '{}'", pack.getId(), e);
                }
                return null;
            }
        }, Util.backgroundExecutor()).thenApplyAsync(nativeImage -> {
            if (nativeImage == null) return null;
            ResourceLocation icon = ResourceLocation.withDefaultNamespace(hashIconName(pack.getId()));
            this.textureManager.register(icon, new DynamicTexture(nativeImage));
            return icon;
        }, this.mainThreadExecutor);
    }

    @SuppressWarnings("deprecation")
    private static String hashIconName(String id) {
        return "pack/" + Util.sanitizeName(id, ResourceLocation::validPathChar) + "/" + Hashing.sha1().hashUnencodedChars(id) + "/icon";
    }
}
