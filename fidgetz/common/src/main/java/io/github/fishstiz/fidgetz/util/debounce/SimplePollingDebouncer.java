package io.github.fishstiz.fidgetz.util.debounce;

import net.minecraft.Util;

import java.util.function.Consumer;

public class SimplePollingDebouncer<T> extends PollingDebouncer<T> {
    private boolean pending = false;
    private long lastCallTime = 0;
    private T lastArg = null;

    public SimplePollingDebouncer(Consumer<T> task, long delay) {
        super(task, delay);
    }

    public SimplePollingDebouncer(Runnable task, long delay) {
        super(task, delay);
    }

    @Override
    public void accept(T t) {
        this.lastCallTime = Util.getMillis();
        this.pending = true;
        this.lastArg = t;
    }

    @Override
    public void abort() {
        this.pending = false;
    }

    @Override
    public void poll() {
        if (this.pending && Util.getMillis() - this.lastCallTime >= this.delay) {
            try {
                this.task.accept(this.lastArg);
            } finally {
                this.pending = false;
            }
        }
    }
}