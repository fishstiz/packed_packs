package io.github.fishstiz.packed_packs.pack;

import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.config.DevConfig;
import io.github.fishstiz.packed_packs.config.ProfileManager;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

// TreeMap in vanilla and fabric, LinkedHashMap in NeoForge
public class PackAliasMap implements Map<String, Pack> {
    private final DevConfig.Packs config;
    private final Map<String, Pack> map;
    private Set<String> unresolvedIds;

    public PackAliasMap(DevConfig.Packs config, Map<String, Pack> map) {
        this.config = config;
        this.map = map;
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.map.containsKey(key) || this.resolvePackId(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        return this.map.containsValue(value);
    }

    /**
     * Called when rebuilding selected ids, so hopefully the resolved map is also built immediately.
     */
    @Override
    public Pack get(Object key) {
        Pack pack = this.map.get(key);
        if (pack != null) {
            return pack;
        }
        return this.resolvePackId(key);
    }

    @Override
    public @Nullable Pack put(String key, Pack value) {
        return this.map.put(key, value);
    }

    @Override
    public Pack remove(Object key) {
        return this.map.remove(key);
    }

    @Override
    public void putAll(@NonNull Map<? extends String, ? extends Pack> m) {
        this.map.putAll(m);
    }

    @Override
    public void clear() {
        this.map.clear();
    }

    @Override
    public @NonNull Set<String> keySet() {
        return this.map.keySet();
    }

    @Override
    public @NonNull Collection<Pack> values() {
        return this.map.values();
    }

    @Override
    public @NonNull Set<Entry<String, Pack>> entrySet() {
        return this.map.entrySet();
    }

    private @Nullable Pack resolvePackId(Object key) {
        if (!(key instanceof String packId)) {
            return null;
        }
        if (this.unresolvedIds != null && this.unresolvedIds.contains(key)) {
            return null;
        }

        String resolvedPackId = this.config.resolveCanonicalId(packId);
        if (resolvedPackId != null) {
            if (!this.config.isAlias(packId)) {
                PackedPacks.LOGGER.info("[packed_packs] Unknown pack '{}' matched via regex to '{}', caching result.", packId, resolvedPackId);
                this.config.putAlias(packId, resolvedPackId);
                ProfileManager.get(this.config.packType()).remapAndSavePackIds(packId, resolvedPackId);
                DevConfig.get().save();
            }

            Pack resolvedPack = this.map.get(resolvedPackId);
            if (resolvedPack != null) {
                PackedPacks.LOGGER.info("[packed_packs] Resolved unknown pack '{}' to '{}'.", packId, resolvedPackId);
                this.put(resolvedPackId, resolvedPack);
            } else {
                PackedPacks.LOGGER.warn("[packed_packs] Unknown pack '{}' resolved to '{}', but no such pack is available.", packId, resolvedPackId);
                this.setUnresolved(packId);
            }
            return resolvedPack;
        }

        this.setUnresolved(packId);
        return null;
    }

    private void setUnresolved(String packId) {
        if (this.unresolvedIds == null) {
            this.unresolvedIds = new ObjectOpenHashSet<>();
        }
        this.unresolvedIds.add(packId);
    }
}
