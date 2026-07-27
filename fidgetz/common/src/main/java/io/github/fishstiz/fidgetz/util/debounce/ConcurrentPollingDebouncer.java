package io.github.fishstiz.fidgetz.util.debounce;

import net.minecraft.Util;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ConcurrentPollingDebouncer<T> extends PollingDebouncer<T> {
    private final AtomicReference<T> lastArg = new AtomicReference<>(null);
    private volatile boolean pending = false;
    private volatile long lastCallTime = 0;

    public ConcurrentPollingDebouncer(Consumer<T> task, long delay) {
        super(task, delay);
    }

    public ConcurrentPollingDebouncer(Runnable task, long delay) {
        super(task, delay);
    }

    @Override
    public void accept(T t) {
        this.lastCallTime = Util.getMillis();
        this.pending = true;
        this.lastArg.set(t);
    }

    @Override
    public void abort() {
        this.pending = false;
    }

    @Override
    public void poll() {
        boolean shouldRun = this.pending;
        long lastTime = this.lastCallTime;
        T arg = this.lastArg.get();

        if (shouldRun && Util.getMillis() - lastTime >= this.delay) {
            try {
                this.task.accept(arg);
            } finally {
                if (lastTime == this.lastCallTime && arg == this.lastArg.get()) {
                    this.pending = false;
                }
            }
        }
    }
}
