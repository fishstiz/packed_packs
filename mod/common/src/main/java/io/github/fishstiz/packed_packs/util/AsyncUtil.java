package io.github.fishstiz.packed_packs.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class AsyncUtil {
    private AsyncUtil() {
    }

    public static void submitAndWait(Executor executor, Runnable... runnables) {
        @SuppressWarnings("unchecked")
        CompletableFuture<Void>[] futures = (CompletableFuture<Void>[]) new CompletableFuture<?>[runnables.length];
        for (int i = 0; i < runnables.length; i++) {
            futures[i] = CompletableFuture.runAsync(runnables[i], executor);
        }
        CompletableFuture.allOf(futures).join();
    }
}
