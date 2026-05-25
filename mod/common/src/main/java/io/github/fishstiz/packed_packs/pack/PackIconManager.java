package io.github.fishstiz.packed_packs.pack;

import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import io.github.fishstiz.packed_packs.util.PackUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.Util;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class PackIconManager {
    public static final Identifier DEFAULT_ICON = Identifier.withDefaultNamespace("textures/misc/unknown_pack.png");
    public static final Identifier DEFAULT_FOLDER_ICON = PackedPacks.id("textures/misc/unknown_folder.png");
    private final Map<String, Identifier> cachedIcons = new Object2ObjectOpenHashMap<>();
    private final Executor mainThreadExecutor;
    private final TextureManager textureManager;
    private Map<String, Identifier> staleIcons;

    public PackIconManager(Executor mainThreadExecutor, TextureManager textureManager) {
        this.mainThreadExecutor = mainThreadExecutor;
        this.textureManager = textureManager;
    }

    public Identifier get(Pack pack) {
        if (!cachedIcons.containsKey(pack.getId())) {
            Identifier fallback = (this.staleIcons != null) ? this.staleIcons.get(pack.getId()) : null;
            if (fallback == null) {
                fallback = getDefault(pack);
            }
            cachedIcons.put(pack.getId(), fallback);
            loadPackIcon(pack).thenAcceptAsync(icon -> {
                if (icon != null) {
                    cachedIcons.put(pack.getId(), icon);
                }
            }, mainThreadExecutor);
        }

        return cachedIcons.getOrDefault(pack.getId(), getDefault(pack));
    }

    public void clear() {
        this.staleIcons = Map.copyOf(cachedIcons);
        cachedIcons.clear();
    }

    public static Identifier getDefault(Pack pack) {
        return pack instanceof FolderPack ? DEFAULT_FOLDER_ICON : DEFAULT_ICON;
    }

    /**
     * Copied from {@code PackSelectionScreen#loadPackIcon(TextureManager, Pack)}
     */
    private CompletableFuture<@Nullable Identifier> loadPackIcon(Pack pack) {
        return CompletableFuture.supplyAsync(() -> {
            try (PackResources packResources = pack.open()) {
                IoSupplier<@NonNull InputStream> iconIoSupplier = packResources.getRootResource(PackUtil.ICON_FILENAME);
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
            Identifier icon = Identifier.withDefaultNamespace(hashIconName(pack.getId()));
            this.textureManager.register(icon, new DynamicTexture(icon::toString, nativeImage));
            return icon;
        }, this.mainThreadExecutor);
    }

    @SuppressWarnings("deprecation")
    private static String hashIconName(String id) {
        return "pack/" + Util.sanitizeName(id, Identifier::validPathChar) + "/" + Hashing.sha1().hashUnencodedChars(id) + "/icon";
    }
}
