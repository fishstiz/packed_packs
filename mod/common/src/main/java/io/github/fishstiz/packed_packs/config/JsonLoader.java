package io.github.fishstiz.packed_packs.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.fishstiz.packed_packs.PackedPacks;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Supplier;

public final class JsonLoader {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(PackOverride.class, new PackOverride.Adapter())
            .create();

    private JsonLoader() {
    }

    public static <T> T loadOrDefault(InputStream inputStream, Class<T> clazz, Supplier<T> defaultFactory) {
        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            return GSON.fromJson(reader, clazz);
        } catch (Exception e) {
            PackedPacks.LOGGER.error("[packed_packs] Failed to read object with type '{}'", clazz.getName(), e);
            return defaultFactory.get();
        }
    }

    public static <T> T loadOrDefault(Path path, Class<T> clazz, Supplier<T> defaultFactory) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            return GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), clazz);
        } catch (NoSuchFileException ignored) {
        } catch (Exception e) {
            PackedPacks.LOGGER.error("[packed_packs] Failed to load file at '{}'. ", path, e);
        }
        return defaultFactory.get();
    }

    public static <T> T loadOrCreate(Path path, Class<T> clazz, Supplier<T> defaultFactory) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            String json = new String(bytes, StandardCharsets.UTF_8);
            return GSON.fromJson(json, clazz);
        } catch (NoSuchFileException e) {
            T serializable = defaultFactory.get();
            PackedPacks.LOGGER.info("[packed_packs] Creating file at '{}'.", path);
            saveJson(serializable, path);
            return serializable;
        } catch (Exception e) {
            PackedPacks.LOGGER.error("[packed_packs] Failed to load file at '{}'. ", path, e);
            return defaultFactory.get();
        }
    }

    public static <T> boolean saveJson(T serializable, Path path) {
        try {
            String json = GSON.toJson(serializable);
            Files.createDirectories(path.getParent());
            Files.writeString(path, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return true;
        } catch (Exception e) {
            PackedPacks.LOGGER.info("[packed_packs] Failed to save file at '{}'.", path, e);
            return false;
        }
    }
}
