package io.github.fishstiz.packed_packs.impl;

import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.api.PreferenceRegistry;
import io.github.fishstiz.packed_packs.config.Preferences;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class PreferenceRegistryImpl implements PreferenceRegistry {
    private final Map<Identifier, Preferences.Spec<?>> preferences = new Object2ObjectOpenHashMap<>();
    private @Nullable Set<Key<?>> incorrectKeys;
    private boolean frozen;

    PreferenceRegistryImpl() {
    }

    @Override
    public <T> Key<T> register(Identifier id, Class<T> type, T defaultValue, Function<String, T> deserializer) {
        if (this.frozen) {
            throw new IllegalStateException("Cannot register preference as registry has been frozen.");
        }

        if (this.preferences.containsKey(id)) {
            PackedPacks.LOGGER.error("Preference already exists for key: {}", id);
            return new KeyImpl<>(id, type);
        }

        String keyString = id.getNamespace().equals(PackedPacks.MOD_ID) ? id.getPath() : id.toString();
        this.preferences.put(id, Preferences.Spec.create(keyString, type, defaultValue, deserializer));

        return new KeyImpl<>(id, type);
    }

    @SuppressWarnings("unchecked")
    private <T> Preferences.@Nullable Spec<T> getAndValidate(Key<T> key) {
        Preferences.Spec<?> spec = this.preferences.get(key.id());
        if (spec == null) {
            return null;
        }
        if (spec.type() == key.type()) {
            return (Preferences.Spec<T>) spec;
        }
        this.addIncorrectKey(key);
        return null;
    }

    @Override
    public <T> void set(Key<T> key, T value) {
        this.checkFrozen();

        Preferences.Spec<T> spec = this.getAndValidate(key);
        if (spec == null) {
            throw new NullPointerException("Cannot set value of unregistered preference " + key.id());
        }

        Preferences.INSTANCE.getOrThrow(spec).set(value);
    }

    @Override
    public @Nullable <T> T get(Key<T> key) {
        this.checkFrozen();

        Preferences.Spec<T> spec = this.getAndValidate(key);
        if (spec == null) {
            return null;
        }

        return Preferences.INSTANCE.getOrThrow(spec).get();
    }

    @Override
    public void setUnsafe(Identifier id, Object value) {
        this.checkFrozen();

        @SuppressWarnings("unchecked")
        Preferences.Spec<Object> spec = (Preferences.Spec<Object>) this.preferences.get(id);
        if (spec != null) {
            Preferences.INSTANCE.getOrThrow(spec).set(value);
        } else {
            throw new NullPointerException("Cannot set preference with key " + id);
        }
    }

    @Override
    public @Nullable Object getUnsafe(Identifier key) {
        this.checkFrozen();

        Preferences.Spec<?> spec = this.preferences.get(key);
        if (spec != null) {
            return Preferences.INSTANCE.getOrThrow(spec).get();
        }

        return null;
    }

    private void addIncorrectKey(Key<?> key) {
        if (this.incorrectKeys == null) {
            this.incorrectKeys = new ReferenceOpenHashSet<>();
        }

        if (this.incorrectKeys.add(key)) {
            PackedPacks.LOGGER.warn("[packed_packs] Unexpected type found for preference key '{}'", key.id());
        }
    }

    public Collection<Preferences.Spec<?>> getPreferences() {
        return this.preferences.values();
    }

    @SuppressWarnings("unchecked")
    public <T> Preferences.Spec<T> getSpec(Key<T> key) {
        return (Preferences.Spec<T>) this.preferences.get(key.id());
    }

    private void checkFrozen() {
        if (!this.frozen) {
            throw new IllegalStateException("Cannot set or get a preference during initialization.");
        }
    }

    void freeze() {
        this.frozen = true;
    }

    private record KeyImpl<T>(Identifier id, Class<T> type) implements Key<T> {
    }
}
