package io.github.fishstiz.packed_packs;

import com.mojang.blaze3d.Blaze3D;
import io.github.fishstiz.packed_packs.platform.Services;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class PackedPacks {
    public static final String MOD_ID = "packed_packs";
    public static final String MOD_NAME = "Packed Packs";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final boolean DEBUG = System.getProperty("packed_packs.debug") != null;

    private PackedPacks() {
    }

    public static Path getConfigDir() {
        return Services.PLATFORM.getConfigDir().resolve(MOD_ID);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void openPath(Path path) {
        Blaze3D.openPath(path);
    }

    public static long duration(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    public static <T, R> R mapOrElse(T obj, R defaultValue, Function<T, R> mapper) {
        return obj != null ? mapper.apply(obj) : defaultValue;
    }

    public static void runInParallel(Runnable... tasks) {
        @SuppressWarnings("unchecked")
        CompletableFuture<Void>[] futures = (CompletableFuture<Void>[]) new CompletableFuture<?>[tasks.length];
        for (int i = 0; i < tasks.length; i++) {
            futures[i] = CompletableFuture.runAsync(tasks[i], Util.backgroundExecutor());
        }
        CompletableFuture.allOf(futures).join();
    }
}
