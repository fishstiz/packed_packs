package io.github.fishstiz.packed_packs.impl;

import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.api.Preference;
import io.github.fishstiz.packed_packs.api.PreferenceRegistry;
import io.github.fishstiz.packed_packs.config.Preferences;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class PreferenceRegistryImpl implements PreferenceRegistry {
    private final Map<Identifier, PreferenceImpl<?>> options = new Object2ObjectOpenHashMap<>();

    PreferenceRegistryImpl() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Preference<T> register(
            Identifier id,
            Class<T> type,
            T defaultValue,
            @Nullable Function<String, T> deserializer,
            @Nullable Function<T, String> serializer
    ) {
        Preference<?> existing = this.options.get(id);
        if (existing != null) {
            if (existing.type().isAssignableFrom(type)) {
                PackedPacks.LOGGER.warn("[packed_packs] Preference already exists for id '{}' with type '{}'", id, type);
                return (Preference<T>) existing;
            }
            PackedPacks.LOGGER.warn(
                    "[packed_packs] Preference already exists for id '{}' with mismatched type '{}'!='{}', returning preference will be transient.",
                    id, existing.type(), type
            );
            return new TransientPreference<>(id, type, defaultValue);
        }

        String key = id.getNamespace().equals(PackedPacks.MOD_ID) ? id.getPath() : id.toLanguageKey();

        Preferences.Option<T> option = deserializer == null || serializer == null
                ? Preferences.register(key, type, defaultValue)
                : Preferences.register(key, type, defaultValue, deserializer, serializer);

        PreferenceImpl<T> preference = new PreferenceImpl<>(id, option);
        this.options.put(id, preference);
        return preference;
    }

    @Override
    public <T> Preference<T> register(Identifier id, Class<T> type, T defaultValue) {
        return this.register(id, type, defaultValue, null, null);
    }

    @Override
    public Optional<Preference<?>> find(Identifier id) {
        return Optional.ofNullable(this.options.get(id));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<Preference<T>> find(Identifier id, Class<T> type) {
        Preference<?> preference = this.options.get(id);
        if (preference == null || !type.isAssignableFrom(preference.type())) {
            return Optional.empty();
        }
        return Optional.of((Preference<T>) preference);
    }

    public <T> Preferences.@Nullable Option<T> getInternal(Preference<T> preference) {
        return preference instanceof PreferenceImpl<T> impl ? impl.delegate : null;
    }

    private record PreferenceImpl<T>(Identifier id, Preferences.Option<T> delegate) implements Preference<T> {
        @Override
        public Class<T> type() {
            return delegate.type();
        }

        @Override
        public T get() {
            return this.delegate.get();
        }

        @Override
        public void set(T value) {
            this.delegate.set(value);
        }

        @Override
        public T getDefault() {
            return this.delegate.getDefault();
        }

        @Override
        public void reset() {
            this.delegate.reset();
        }
    }

    private static final class TransientPreference<T> implements Preference<T> {
        private final Identifier id;
        private final Class<T> type;
        private final T defaultValue;
        private T value;

        TransientPreference(Identifier id, Class<T> type, T defaultValue) {
            this.id = id;
            this.type = type;
            this.defaultValue = defaultValue;
            this.value = defaultValue;
        }

        @Override
        public Identifier id() {
            return this.id;
        }

        @Override
        public Class<T> type() {
            return this.type;
        }

        @Override
        public T get() {
            return this.value;
        }

        @Override
        public void set(T value) {
            this.value = value;
        }

        @Override
        public T getDefault() {
            return this.defaultValue;
        }

        @Override
        public void reset() {
            this.value = this.defaultValue;
        }
    }
}
