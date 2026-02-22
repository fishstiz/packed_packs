package io.github.fishstiz.packed_packs.config;

import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.impl.PackedPacksApiImpl;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.jspecify.annotations.Nullable;

import java.io.*;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;

import static io.github.fishstiz.packed_packs.PackedPacks.LOGGER;

public class Preferences {
    public static final Preferences INSTANCE = load();
    private final Map<Spec<?>, Option<?>> options = new Reference2ObjectOpenHashMap<>();
    public final Option<Boolean> originalScreenWidget = new Option<>("original_screen", boolean.class, true);
    public final Option<Boolean> optionsWidget = new Option<>("options", boolean.class, true);
    public final Option<Boolean> actionBarWidget = new Option<>("action_bar", boolean.class, true);
    public final Option<Boolean> toggleIncompatibleWidget = new Option<>("toggle_incompatible", boolean.class, true);
    public final Option<Boolean> folderPackWidget = new Option<>("folder_pack", boolean.class, true);

    private Preferences() {
    }

    private void appendExtensions() {
        PackedPacksApiImpl.getInstance().preferences().getPreferences().forEach(Option::new);
    }

    public interface Spec<T> {
        String key();

        Class<T> type();

        T defaultValue();

        T deserialize(String value);

        static <T> Spec<T> create(String key, Class<T> type, T defaultValue, Function<String, T> deserializer) {
            return new Spec<>() {
                @Override
                public String key() {
                    return key;
                }

                @Override
                public Class<T> type() {
                    return type;
                }

                @Override
                public T defaultValue() {
                    return defaultValue;
                }

                @Override
                public T deserialize(String value) {
                    return deserializer.apply(value);
                }
            };
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<Option<T>> get(Spec<T> spec) {
        return Optional.ofNullable((Option<T>) this.options.get(spec));
    }

    @SuppressWarnings("unchecked")
    public <T> Option<T> getOrThrow(Spec<T> spec) {
        return Objects.requireNonNull((Option<T>) this.options.get(spec), spec.key());
    }

    private static File getFile() {
        return PackedPacks.getConfigDir().resolve("preferences.properties").toFile();
    }

    private static Preferences load() {
        Preferences prefs = new Preferences();
        prefs.appendExtensions();

        File file = getFile();
        if (!file.exists()) {
            return prefs;
        }

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            props.load(fis);

            for (Option<?> entry : prefs.options.values()) {
                String key = entry.getKey();
                String value = props.getProperty(key);
                entry.deserializeAndSet(value);
            }
        } catch (IOException | NumberFormatException e) {
            LOGGER.error("[packed_packs] Failed to load preferences. ", e);
        }

        return prefs;
    }

    public void reset() {
        this.options.values().forEach(Option::reset);
    }

    public void save() {
        Properties props = new Properties();

        for (Option<?> entry : this.options.values()) {
            props.setProperty(entry.getKey(), entry.get().toString());
        }

        try (FileOutputStream fos = new FileOutputStream(getFile())) {
            props.store(fos, "Preferences");
        } catch (IOException e) {
            LOGGER.error("[packed_packs] Failed to save preferences. ", e);
        }
    }

    public class Option<T> {
        private final Spec<T> spec;
        private T value;

        private Option(Spec<T> spec) {
            this.spec = spec;
            this.value = spec.defaultValue();
            Preferences.this.options.put(spec, this);
        }

        private Option(String key, Class<T> type, T defaultValue, @Nullable Function<String, T> deserializer) {
            this(Spec.create(key, type, defaultValue, deserializer == null ? getDefaultDeserializer(type) : deserializer));
        }

        private Option(String key, Class<T> type, T defaultValue) {
            this(key, type, defaultValue, null);
        }

        public void set(T value) {
            this.value = value;
        }

        public T get() {
            return this.value;
        }

        public String getKey() {
            return this.spec.key();
        }

        public T getDefault() {
            return this.spec.defaultValue();
        }

        public void reset() {
            this.value = this.getDefault();
        }

        void deserializeAndSet(@Nullable String value) {
            if (value == null) {
                this.value = this.getDefault();
                return;
            }

            try {
                this.value = this.spec.deserialize(value);
            } catch (Exception e) {
                this.value = this.getDefault();
                LOGGER.error("[packed_packs] Failed to read preference '{}' with value '{}'. ", this.spec.key(), value, e);
            }
        }

        @SuppressWarnings("unchecked")
        static <T> Function<String, T> getDefaultDeserializer(Class<T> type) {
            if (type == boolean.class || type == Boolean.class) {
                return value -> (T) Boolean.valueOf(Boolean.parseBoolean(value));
            } else if (type == int.class || type == Integer.class) {
                return value -> (T) Integer.valueOf(Integer.parseInt(value));
            } else if (type == String.class) {
                return value -> (T) value;
            }
            throw new UnsupportedOperationException("No default deserializer for " + type);
        }
    }
}
