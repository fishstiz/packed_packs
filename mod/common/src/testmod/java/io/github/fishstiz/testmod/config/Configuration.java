package io.github.fishstiz.testmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.fishstiz.testmod.TestMod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Configuration {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Configuration INSTANCE = loadOrCreate(getPath());
    private boolean shouldAddTestPacks = true;

    public Configuration() {
    }

    public static Configuration getInstance() {
        return INSTANCE;
    }

    private static Path getPath() {
        return Paths.get("config").resolve("testmod.json");
    }

    public void save() {
        save(INSTANCE, getPath());
    }

    public void setShouldAddTestPacks(boolean shouldAddTestPacks) {
        this.shouldAddTestPacks = shouldAddTestPacks;
    }

    public boolean shouldAddTestPacks() {
        return shouldAddTestPacks;
    }

    private static Configuration loadOrCreate(Path path) {
        try {
            if (Files.notExists(path)) {
                Configuration configuration = new Configuration();
                TestMod.LOGGER.info("Creating config file at '{}'.", path);
                save(configuration, path);
                return configuration;
            }

            byte[] bytes = Files.readAllBytes(path);
            String json = new String(bytes, StandardCharsets.UTF_8);
            return GSON.fromJson(json, Configuration.class);
        } catch (IOException e) {
            TestMod.LOGGER.error("Failed to load config file at '{}'. ", path, e);
            return new Configuration();
        }
    }

    private static void save(Configuration configuration, Path path) {
        try {
            String json = GSON.toJson(configuration);
            Files.createDirectories(path.getParent());
            Files.writeString(path, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            TestMod.LOGGER.info("Failed to save config file at '{}'.", path, e);
        }
    }
}
