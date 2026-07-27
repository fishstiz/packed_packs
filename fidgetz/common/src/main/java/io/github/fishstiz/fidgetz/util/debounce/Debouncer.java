package io.github.fishstiz.fidgetz.util.debounce;

import java.util.function.Consumer;

public abstract class Debouncer<T> implements Consumer<T>, Runnable {
    protected final Consumer<T> task;
    protected final long delay;

    protected Debouncer(Consumer<T> task, long delay) {
        this.task = task;
        this.delay = delay;
    }

    protected Debouncer(Runnable task, long delay) {
        this(arg -> task.run(), delay);
    }

    @Override
    public final void run() {
        this.accept(null);
    }
}
