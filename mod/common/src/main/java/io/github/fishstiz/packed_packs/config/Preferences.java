package io.github.fishstiz.packed_packs.config;

import com.google.gson.Gson;
import io.github.fishstiz.packed_packs.PackedPacks;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import java.io.*;
import java.lang.reflect.Type;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

import static io.github.fishstiz.packed_packs.PackedPacks.LOGGER;

public final class Preferences {
    private static final Gson GSON = new Gson();
    private static final Preferences INSTANCE = new Preferences();
    public static final Option<Boolean> ORIGINAL_SCREEN_WIDGET = register("original_screen", boolean.class, true);
    public static final Option<Boolean> OPTIONS_WIDGET = register("options", boolean.class, true);
    public static final Option<Boolean> ACTION_BAR_WIDGET = register("action_bar", boolean.class, true);
    public static final Option<Boolean> INCOMPATIBLE_TOGGLE_WIDGET = register("toggle_incompatible", boolean.class, true);
    public static final Option<Boolean> FOLDER_PACK_WIDGET = register("folder_pack", boolean.class, true);
    private final Set<Option<?>> options = new ReferenceOpenHashSet<>();

    private static File getFile() {
        return PackedPacks.getConfigDir().resolve("preferences.properties").toFile();
    }

    public static <T> Option<T> register(String key, Class<T> type, T defaultValue, Function<String, T> deserializer, Function<T, String> serializer) {
        return INSTANCE.newOption(key, type, defaultValue, serializer, deserializer);
    }

    @SuppressWarnings("unchecked")
    public static <T> Option<T> register(String key, Class<T> type, T defaultValue) {
        return type == String.class
                ? (Option<T>) register(key, String.class, (String) defaultValue, Function.identity(), Function.identity())
                : register(key, type, defaultValue, json -> GSON.fromJson(json, type), GSON::toJson);
    }

    public static void reset() {
        INSTANCE.options.forEach(Option::reset);
    }

    public static void save() {
        Properties props = new Properties();

        for (Option<?> option : INSTANCE.options) {
            try {
                props.setProperty(option.getKey(), option.serialize());
            } catch (Exception e) {
                LOGGER.error("[packed_packs] Failed to serialize preference '{}' with value '{}'. ", option.getKey(), option.get(), e);
            }
        }

        try (FileOutputStream fos = new FileOutputStream(getFile())) {
            props.store(fos, "Preferences");
        } catch (IOException e) {
            LOGGER.error("[packed_packs] Failed to save preferences. ", e);
        }
    }

    private <T> Option<T> newOption(String key, Class<T> type, T defaultValue, Function<T, String> serializer, Function<String, T> deserializer) {
        return new Option<>(key, type, defaultValue, serializer, deserializer);
    }

    private Preferences() {
    }

    public class Option<T> {
        private static final Properties CACHE;
        private final String key;
        private final Type type;
        private final T defaultValue;
        private final Function<T, String> serializer;
        private T value;

        static {
            Properties props = new Properties();
            File file = getFile();
            if (file.exists()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    props.load(fis);
                } catch (Exception e) {
                    LOGGER.error("[packed_packs] Failed to load preferences. ", e);
                }
            }
            CACHE = props;
        }

        private Option(String key, Class<T> type, T defaultValue, Function<T, String> serializer, Function<String, T> deserializer) {
            this.key = key;
            this.type = type;
            this.defaultValue = defaultValue;
            this.serializer = serializer;
            this.value = resolveFromCache(key, defaultValue, deserializer);
            Preferences.this.options.add(this);
        }

        private static <T> T resolveFromCache(String key, T fallback, Function<String, T> deserializer) {
            String value = CACHE.getProperty(key);
            if (value == null) return fallback;

            try {
                return deserializer.apply(value);
            } catch (Exception e) {
                LOGGER.error("[packed_packs] Failed to deserialize preference '{}' with value '{}'. ", key, value, e);
                return fallback;
            }
        }

        public void set(T value) {
            this.value = value;
        }

        public T get() {
            return this.value;
        }

        public String getKey() {
            return this.key;
        }

        public T getDefault() {
            return this.defaultValue;
        }

        public void reset() {
            this.set(this.getDefault());
        }

        public Type type() {
            return this.type;
        }

        String serialize() {
            return this.serializer.apply(this.value);
        }
    }
}
