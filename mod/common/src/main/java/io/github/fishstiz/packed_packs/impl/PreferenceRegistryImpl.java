package io.github.fishstiz.packed_packs.impl;

import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.api.PreferenceRegistry;
import io.github.fishstiz.packed_packs.config.Preferences;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class PreferenceRegistryImpl implements PreferenceRegistry {
    private final Map<Identifier, Preferences.Option<?>> options = new Object2ObjectOpenHashMap<>();
    private @Nullable Set<Key<?>> incorrectKeys;

    PreferenceRegistryImpl() {
    }

    @Override
    public <T> Key<T> register(
            Identifier id,
            Class<T> type,
            T defaultValue,
            @Nullable Function<String, T> deserializer,
            @Nullable Function<T, String> serializer
    ) {
        Preferences.Option<?> option = this.options.get(id);
        if (option != null) {
            PackedPacks.LOGGER.error("Preference already exists for key: {}", id);
            return new KeyImpl<>(id, type);
        }

        String key = id.getNamespace().equals(PackedPacks.MOD_ID) ? id.getPath() : id.toString();
        this.options.put(id, deserializer == null || serializer == null
                ? Preferences.register(key, type, defaultValue)
                : Preferences.register(key, type, defaultValue, deserializer, serializer));

        return new KeyImpl<>(id, type);
    }

    @Override
    public <T> Key<T> register(Identifier id, Class<T> type, T defaultValue) {
        return this.register(id, type, defaultValue, null, null);
    }

    @SuppressWarnings("unchecked")
    private <T> Preferences.@Nullable Option<T> getAndValidate(Key<T> key) {
        Preferences.Option<?> option = this.options.get(key.id());
        if (option == null) {
            return null;
        }
        if (option.type() == key.type()) {
            return (Preferences.Option<T>) option;
        }
        if (this.incorrectKeys == null) {
            this.incorrectKeys = new ReferenceOpenHashSet<>();
        }
        if (this.incorrectKeys.add(key)) {
            PackedPacks.LOGGER.warn("[packed_packs] Unexpected type found for preference key '{}'", key.id());
        }
        return null;
    }

    private <T> void setOrLog(Identifier id, Preferences.@Nullable Option<T> option, T value) {
        if (option == null) {
            PackedPacks.LOGGER.error("[packed_packs] Cannot set value of unregistered preference '{}'", id);
        } else {
            option.set(value);
        }
    }

    @Override
    public <T> void set(Key<T> key, T value) {
        this.setOrLog(key.id(), this.getAndValidate(key), value);
    }

    @Override
    public @Nullable <T> T get(Key<T> key) {
        Preferences.Option<T> option = this.getAndValidate(key);
        return option == null ? null : option.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setUnsafe(Identifier id, Object value) {
        this.setOrLog(id, (Preferences.Option<Object>) this.options.get(id), value);
    }

    @Override
    public @Nullable Object getUnsafe(Identifier key) {
        Preferences.Option<?> option = this.options.get(key);
        return option == null ? null : option.get();
    }

    public <T> Preferences.@Nullable Option<T> getOption(Key<T> key) {
        return this.getAndValidate(key);
    }

    private record KeyImpl<T>(Identifier id, Class<T> type) implements Key<T> {
    }
}
