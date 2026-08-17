package io.github.fishstiz.packed_packs.config;

import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.packed_packs.util.Utils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

// todo remove references to packs, use pack ids only, or not?! move pack options to entry context but not implement pack options mk
public final class Profile implements PackOptions {
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

    public boolean includes(String packId) {
        return this.packIds.contains(packId);
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
            this.setHidden(hidden, pack.getId());
        }
    }

    public void setHidden(boolean hidden, String pack) {
        this.applyOrRemoveOverride(pack, hidden ? true : null, PackOverride::setHidden);
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

    public @Nullable PackOverride getOverrides(String packId) {
        return this.overrides.get(packId);
    }

    @Override
    public boolean isHidden(String packId) {
        return Boolean.TRUE.equals(Utils.mapOrElse(this.overrides.get(packId), false, PackOverride::hidden));
    }

    @Override
    public boolean isRequired(String packId) {
        return Boolean.TRUE.equals(Utils.mapOrElse(this.overrides.get(packId), false, PackOverride::required));
    }

    @Override
    public boolean isFixed(String packId) {
        if (this.overridesPosition(packId)) {
            return Objects.requireNonNull(this.overrides.get(packId).position()).fixed();
        }
        return false;
    }

    @Override
    public Pack.@Nullable Position getPosition(String packId) {
        PackOverride override = this.overrides.get(packId);
        if (override != null) {
            PackOverride.Position position = override.position();
            if (position != null) {
                return position.override();
            }
        }
        return null;
    }

    public PackOverride.@Nullable Position getPositionOverride(String packId) {
        if (this.overridesPosition(packId)) {
            return this.overrides.get(packId).position();
        }
        return null;
    }

    @Override
    public @Nullable PackSelectionConfig getSelectionConfig(String packId) {
        PackOverride override = this.overrides.get(packId);
        if (override != null) {
            Boolean required = override.required();
            PackOverride.Position position = override.position();
            if (required != null || position != null) {
                // todo this probably shouldnt be in profile
            }


            return new PackSelectionConfig(this.isRequired(packId), this.getPosition(packId), this.isFixed(packId));
        }
        return null;
    }

    public boolean overridesRequired(String packId) {
        return this.overridesProperty(packId, PackOverride::required);
    }

    public boolean overridesPosition(String packId) {
        return this.overridesProperty(packId, PackOverride::position);
    }

    private boolean overridesProperty(String packId, Function<PackOverride, @Nullable Object> property) {
        PackOverride entry = this.overrides.get(packId);
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
