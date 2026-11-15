package io.github.fishstiz.packed_packs.util;

import org.jspecify.annotations.Nullable;

public class TimedProgressCache {
    private final long durationNanos;
    private @Nullable Object key;
    private long startTimeNanos;

    public TimedProgressCache(long durationMs) {
        this.durationNanos = durationMs * 1_000_000L;
    }

    public void reset() {
        this.key = null;
        this.startTimeNanos = 0;
    }

    public float getValue(Object key) {
        long now = System.nanoTime();

        if (key != this.key) {
            this.key = key;
            this.startTimeNanos = now;
            return 0;
        } else {
            long elapsed = now - this.startTimeNanos;
            return Math.min(1.0f, (float) elapsed / this.durationNanos);
        }
    }
}
