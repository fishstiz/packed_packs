package io.github.fishstiz.packed_packs.config;

import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.util.PackUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class Profile {
    private boolean locked = false;
    private String name;
    private Map<String, PackOverride> overrides = new Object2ObjectOpenHashMap<>();
    private Set<String> packIds = new ObjectLinkedOpenHashSet<>();
    transient String id;
    transient boolean temp = false;
    transient int overrideGen;

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

    public void setPacks(Collection<String> selected) {
        if (!this.locked) {
            this.packIds = new ObjectLinkedOpenHashSet<>(selected);
        }
    }

    public void syncPacks(Set<String> available, SequencedSet<String> enabled) {
        if (!this.locked) {
            this.packIds = enabled;
            this.overrides.entrySet().removeIf(entry -> {
                PackOverride override = entry.getValue();
                String packId = entry.getKey();
                return !override.hasOverride() || (!this.packIds.contains(packId) && !available.contains(packId));
            });
        }
    }

    public Profile withHiddenOverride(boolean hidden, String packId) {
        applyOrRemoveOverride(packId, hidden ? true : null, PackOverride::setHidden);
        return this;
    }

    public Profile withRequiredOverride(@Nullable Boolean required, String packId) {
        if (!Boolean.FALSE.equals(required) || !PackUtil.isEssential(packId)) {
            applyOrRemoveOverride(packId, required, PackOverride::setRequired);
        }
        return this;
    }

    public Profile withPositionOverride(PackOverride.@Nullable Position position, String packId) {
        applyOrRemoveOverride(packId, position, PackOverride::setPosition);
        return this;
    }

    public Profile withLocked(boolean locked) {
        this.locked = locked;
        return this;
    }

    public Profile withName(String name) {
        this.name = name;
        return this;
    }

    public boolean isLocked() {
        return this.locked;
    }

    public @Nullable PackOverride getOverrides(String packId) {
        return this.overrides.get(packId);
    }

    public boolean isHidden(String packId) {
        return Boolean.TRUE.equals(PackedPacks.mapOrElse(this.overrides.get(packId), false, PackOverride::hidden));
    }

    public boolean isRequired(String packId) {
        return Boolean.TRUE.equals(PackedPacks.mapOrElse(this.overrides.get(packId), false, PackOverride::required));
    }

    public boolean isFixed(String packId) {
        if (this.overridesPosition(packId)) {
            return Objects.requireNonNull(this.overrides.get(packId).position()).fixed();
        }
        return false;
    }

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

    public boolean hasOverride(String packId) {
        PackOverride entry = this.overrides.get(packId);
        return entry != null && entry.hasOverride();
    }

    private <T> void applyOrRemoveOverride(String packId, T property, BiConsumer<PackOverride, T> setter) {
        PackOverride override = this.overrides.computeIfAbsent(packId, id -> new PackOverride());
        setter.accept(override, property);
        if (!override.hasOverride()) this.overrides.remove(packId);
        this.overrideGen++;
    }

    public int overridesGen() {
        return overrideGen;
    }

    // immutability is too much of a hassle. just override equals and hashcode for state
    // and never push profile mutations to history

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Profile profile)) return false;
        return locked == profile.locked
               && Objects.equals(name, profile.name)
               && Objects.equals(overrides, profile.overrides)
               && Objects.equals(packIds, profile.packIds)
               && Objects.equals(id, profile.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(locked, name, overrides, packIds, id);
    }
}
