package io.github.fishstiz.packed_packs.config;

import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.util.PackUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static io.github.fishstiz.fidgetz.util.lang.ObjectsUtil.mapOrDefault;

public final class Profile implements PackOptions, Serializable {
    private boolean locked = false;
    private String name;
    private Map<String, PackOverride> overrides = new Object2ObjectOpenHashMap<>();
    private Set<String> packIds = new ObjectLinkedOpenHashSet<>();
    transient String id;
    transient boolean temp = false;

    Profile(String id) {
        this.id = id;
        this.name = id;
    }

    private Profile(String id, Set<String> packIds, Map<String, PackOverride> overrides) {
        this.id = id;
        this.name = id;
        this.packIds = new ObjectLinkedOpenHashSet<>(packIds);
        this.overrides = new Object2ObjectOpenHashMap<>(overrides);
        this.overrides.replaceAll((ignored, override) -> new PackOverride(override.hidden(), override.required(), override.position()));
    }

    public String getId() {
        return this.id;
    }

    Profile copy(String id) {
        return new Profile(id, this.packIds, this.overrides);
    }

    boolean remapPackId(String packId, String newId) {
        boolean remapped = false;
        if (this.packIds.contains(packId) && !this.packIds.contains(newId)) {
            List<String> packIdsList = new ObjectArrayList<>(this.packIds);
            int index = packIdsList.indexOf(packId);
            if (index != -1) {
                PackedPacks.LOGGER.info("[packed_packs] Updating pack id '{}' to '{}' in profile '{}'", packId, newId, this.name);
                packIdsList.add(index, newId);
                this.packIds = new ObjectLinkedOpenHashSet<>(packIdsList);
                remapped = true;
            }
        }
        PackOverride packOverride = this.overrides.get(packId);
        if (packOverride != null) {
            PackedPacks.LOGGER.info("[packed_packs] Copying overrides from pack id '{}' to '{}' in profile '{}'", packId, newId, this.name);
            this.overrides.put(newId, packOverride);
            remapped = true;
        }
        return remapped;
    }

    public String getName() {
        return this.name;
    }

    void setName(String name) {
        this.name = name;
    }

    public boolean includes(Pack pack) {
        return this.packIds.contains(pack.getId());
    }

    public List<String> getPackIds() {
        return List.copyOf(this.packIds);
    }

    public void setPacks(Collection<Pack> selected) {
        if (!this.locked) {
            this.packIds = new ObjectLinkedOpenHashSet<>(PackUtil.flattenPackIds(selected));
        }
    }

    public void syncPacks(Collection<Pack> available, Collection<Pack> selected) {
        if (!this.locked) {
            this.packIds = new ObjectLinkedOpenHashSet<>(PackUtil.flattenPackIds(selected));
            Set<String> availableIds = new ObjectOpenHashSet<>(PackUtil.flattenPackIds(available));

            this.overrides.entrySet().removeIf(entry -> {
                PackOverride override = entry.getValue();
                String packId = entry.getKey();
                return !override.hasOverride() || (!this.packIds.contains(packId) && !availableIds.contains(packId));
            });
        }
    }

    public void setHidden(boolean hidden, Collection<Pack> packs) {
        for (Pack pack : PackUtil.flattenPacks(packs)) {
            this.setHidden(hidden, pack);
        }
    }

    public void setHidden(boolean hidden, Pack pack) {
        this.applyOrRemoveOverride(pack.getId(), hidden ? true : null, PackOverride::setHidden);
    }

    public void setRequired(@Nullable Boolean required, Collection<Pack> packs) {
        for (Pack pack : PackUtil.flattenPacks(packs)) {
            this.setRequired(required, pack);
        }
    }

    public void setRequired(@Nullable Boolean required, Pack pack) {
        if (!Boolean.FALSE.equals(required) || !PackUtil.isEssential(pack)) {
            this.applyOrRemoveOverride(pack.getId(), required, PackOverride::setRequired);
        }
    }

    public void setPosition(PackOverride.@Nullable Position position, Collection<Pack> packs) {
        for (Pack pack : PackUtil.flattenPacks(packs)) {
            this.setPosition(position, pack);
        }
    }

    public void setPosition(PackOverride.@Nullable Position position, Pack pack) {
        this.applyOrRemoveOverride(pack.getId(), position, PackOverride::setPosition);
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isLocked() {
        return this.locked;
    }

    @Override
    public boolean isHidden(Pack pack) {
        return Boolean.TRUE.equals(mapOrDefault(this.overrides.get(pack.getId()), false, PackOverride::hidden));
    }

    @Override
    public boolean isRequired(Pack pack) {
        return Boolean.TRUE.equals(mapOrDefault(this.overrides.get(pack.getId()), false, PackOverride::required));
    }

    @Override
    public boolean isFixed(Pack pack) {
        if (this.overridesPosition(pack)) {
            return Objects.requireNonNull(this.overrides.get(pack.getId()).position()).fixed();
        }
        return false;
    }

    @Override
    public Pack.@Nullable Position getPosition(Pack pack) {
        if (this.overridesPosition(pack)) {
            return Objects.requireNonNull(this.overrides.get(pack.getId()).position()).get(pack);
        }
        return null;
    }

    public PackOverride.@Nullable Position getPositionOverride(Pack pack) {
        if (this.overridesPosition(pack)) {
            return this.overrides.get(pack.getId()).position();
        }
        return null;
    }

    @Override
    public @Nullable PackSelectionConfig getSelectionConfig(Pack pack) {
        PackOverride packEntry = this.overrides.get(pack.getId());
        if (packEntry != null && (packEntry.required() != null || packEntry.position() != null)) {
            return new PackSelectionConfig(this.isRequired(pack), this.getPosition(pack), this.isFixed(pack));
        }
        return null;
    }

    public boolean overridesRequired(Pack pack) {
        return this.overridesProperty(pack, PackOverride::required);
    }

    public boolean overridesPosition(Pack pack) {
        return this.overridesProperty(pack, PackOverride::position);
    }

    private boolean overridesProperty(Pack pack, Function<PackOverride, @Nullable Object> property) {
        PackOverride entry = this.overrides.get(pack.getId());
        return entry != null && property.apply(entry) != null;
    }

    public boolean hasOverride(Pack pack) {
        PackOverride entry = this.overrides.get(pack.getId());
        return entry != null && entry.hasOverride();
    }

    private <T> void applyOrRemoveOverride(String packId, T property, BiConsumer<PackOverride, T> setter) {
        PackOverride override = this.overrides.computeIfAbsent(packId, id -> new PackOverride());
        setter.accept(override, property);
        if (!override.hasOverride()) this.overrides.remove(packId);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Profile other)) {
            return false;
        }
        return Objects.equals(other.getId(), this.getId());
    }
}
