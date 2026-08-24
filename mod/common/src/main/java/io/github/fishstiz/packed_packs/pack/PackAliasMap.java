package io.github.fishstiz.packed_packs.pack;

import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.config.DevConfig;
import io.github.fishstiz.packed_packs.config.ProfileManager;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
    public void putAll(@NotNull Map<? extends String, ? extends Pack> m) {
        this.map.putAll(m);
    }

    @Override
    public void clear() {
        this.map.clear();
    }

    @Override
    public @NotNull Set<String> keySet() {
        return this.map.keySet();
    }

    @Override
    public @NotNull Collection<Pack> values() {
        return this.map.values();
    }

    @Override
    public @NotNull Set<Entry<String, Pack>> entrySet() {
        return this.map.entrySet();
    }

    @Override
    @SuppressWarnings("SuspiciousMethodCalls")
    public Pack getOrDefault(Object key, Pack defaultValue) {
        return this.map.getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super String, ? super Pack> action) {
        this.map.forEach(action);
    }

    @Override
    public void replaceAll(BiFunction<? super String, ? super Pack, ? extends Pack> function) {
        this.map.replaceAll(function);
    }

    @Override
    public @Nullable Pack putIfAbsent(String key, Pack value) {
        return this.map.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return this.map.remove(key, value);
    }

    @Override
    public boolean replace(String key, Pack oldValue, Pack newValue) {
        return this.map.replace(key, oldValue, newValue);
    }

    @Override
    public @Nullable Pack replace(String key, Pack value) {
        return this.map.replace(key, value);
    }

    @Override
    public Pack computeIfAbsent(String key, @NotNull Function<? super String, ? extends Pack> mappingFunction) {
        return this.map.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public Pack computeIfPresent(String key, @NotNull BiFunction<? super String, ? super Pack, ? extends Pack> remappingFunction) {
        return this.map.computeIfPresent(key, remappingFunction);
    }

    @Override
    public Pack compute(String key, @NotNull BiFunction<? super String, ? super Pack, ? extends Pack> remappingFunction) {
        return this.map.compute(key, remappingFunction);
    }

    @Override
    public Pack merge(String key, @NotNull Pack value, @NotNull BiFunction<? super Pack, ? super Pack, ? extends Pack> remappingFunction) {
        return this.map.merge(key, value, remappingFunction);
    }

    @Override
    @SuppressWarnings("EqualsDoesntCheckParameterClass")
    public boolean equals(Object obj) {
        return this.map.equals(obj);
    }

    @Override
    public int hashCode() {
        return this.map.hashCode();
    }

    private @Nullable Pack resolvePackId(Object key) {
        if (!(key instanceof String packId)) {
            return null;
        }

        if (this.unresolvedIds != null && this.unresolvedIds.contains(key)) {
            return null;
        }

        String resolvedPackId = this.config.resolveCanonicalId(packId);
        if (resolvedPackId == null) {
            this.setUnresolved(packId);
            return null;
        }

        Pack resolvedPack = null;

        if (DevConfig.Packs.isRegexPrefixed(resolvedPackId)) {
            try {
                Pattern pattern = Pattern.compile(resolvedPackId);
                for (Pack availablePack : values()) {
                    if (pattern.matcher(availablePack.getId()).matches()) {
                        resolvedPack = availablePack;
                        break;
                    }
                }
            } catch (PatternSyntaxException e) {
                PackedPacks.LOGGER.error("[packed_packs] Invalid regex syntax '{}' for key '{}'. ", resolvedPackId, key, e);
            }
        } else {
            resolvedPack = this.get(resolvedPackId);

            if (!this.config.isAlias(packId)) {
                PackedPacks.LOGGER.info(
                        "[packed_packs] Unknown pack '{}' matched via regex to '{}', caching result.",
                        packId,
                        resolvedPackId
                );
                this.config.putAlias(packId, resolvedPackId);
                DevConfig.get().save();
            }
        }

        if (resolvedPack != null) {
            PackedPacks.LOGGER.info("[packed_packs] Resolved unknown pack '{}' to '{}', remapping profiles.", packId, resolvedPackId);
            ProfileManager.get(this.config.packType()).remapAndSavePackIds(packId, resolvedPackId);
            this.put(resolvedPackId, resolvedPack);
        } else {
            PackedPacks.LOGGER.warn(
                    "[packed_packs] Unknown pack '{}' mapped to '{}', but no such pack is available.",
                    packId,
                    resolvedPackId
            );
            this.setUnresolved(packId);
        }

        return resolvedPack;
    }

    private void setUnresolved(String packId) {
        if (this.unresolvedIds == null) {
            this.unresolvedIds = new ObjectOpenHashSet<>();
        }
        this.unresolvedIds.add(packId);
    }
}
