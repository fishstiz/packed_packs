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
        this.preferences.put(id, Preferences.Spec.create(keyString, defaultValue, deserializer));

        return new KeyImpl<>(id, type);
    }

    @Override
    public <T> void set(Key<T> key, T value) {
        if (isCorrectType(key, value)) {
            this.setUnsafe(key.id(), value);
        } else {
            if (this.addIncorrectKey(key)) {
                PackedPacks.LOGGER.warn("[packed_packs] Unexpected type found for preference '{}', unable to set value to '{}'", key.id(), value);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T> T get(Key<T> key) {
        Object value = this.getUnsafe(key.id());

        if (!isCorrectType(key, value)) {
            if (this.addIncorrectKey(key)) {
                PackedPacks.LOGGER.warn("[packed_packs] Unexpected type found for preference '{}', unable to get value", key.id());
            }
            return null;
        }

        return (T) value;
    }

    @Override
    public void setUnsafe(Identifier id, Object value) {
        if (!this.frozen) {
            throw new IllegalStateException("Cannot set preference during initialization.");
        }

        @SuppressWarnings("unchecked")
        Preferences.Spec<Object> spec = (Preferences.Spec<Object>) this.preferences.get(id);
        if (spec != null) {
            Preferences.INSTANCE.get(spec).ifPresent(pref -> pref.set(value));
        } else {
            throw new NullPointerException("Cannot set preference with key " + id);
        }
    }

    @Override
    public @Nullable Object getUnsafe(Identifier key) {
        if (!this.frozen) {
            throw new IllegalStateException("Cannot get preference during initialization.");
        }

        Preferences.Spec<?> spec = this.preferences.get(key);
        if (spec != null) {
            return Preferences.INSTANCE.getOrThrow(spec).get();
        }

        return null;
    }

    private boolean addIncorrectKey(Key<?> key) {
        if (this.incorrectKeys == null) {
            this.incorrectKeys = new ReferenceOpenHashSet<>();
        }

        return this.incorrectKeys.add(key);
    }

    public Collection<Preferences.Spec<?>> getPreferences() {
        return this.preferences.values();
    }

    @SuppressWarnings("unchecked")
    public <T> Preferences.Spec<T> getSpec(Key<T> key) {
        return (Preferences.Spec<T>) this.preferences.get(key.id());
    }

    static <T> boolean isCorrectType(Key<T> key, @Nullable Object value) {
        return value == null || key.type().isAssignableFrom(value.getClass());
    }

    void freeze() {
        this.frozen = true;
    }

    private record KeyImpl<T>(Identifier id, Class<T> type) implements Key<T> {
    }
}
