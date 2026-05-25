package io.github.fishstiz.packed_packs.util;

import net.minecraft.util.Util;

import java.util.Iterator;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class Utils {
    private Utils() {
    }

    public static <T, R> R mapOrElse(T obj, R defaultValue, Function<T, R> mapper) {
        return obj != null ? mapper.apply(obj) : defaultValue;
    }

    public static <E> boolean orderEquals(SequencedCollection<E> a, SequencedCollection<E> b) {
        if (a.size() != b.size()) return false;
        Iterator<E> itA = a.iterator();
        Iterator<E> itB = b.iterator();
        while (itA.hasNext()) {
            if (!Objects.equals(itA.next(), itB.next())) return false;
        }
        return true;
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
