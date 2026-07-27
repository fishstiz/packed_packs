package io.github.fishstiz.fidgetz.util.debounce;

import net.minecraft.Util;

import java.util.function.Consumer;

public class ImmediateDebouncer<T> extends Debouncer<T> {
    private long lastCallTime = 0;

    public ImmediateDebouncer(Consumer<T> task, long delay) {
        super(task, delay);
    }

    public ImmediateDebouncer(Runnable task, long delay) {
        super(task, delay);
    }

    @Override
    public void accept(T t) {
        long currentTime = Util.getMillis();
        if (currentTime - this.lastCallTime >= this.delay) {
            this.task.accept(t);
            this.lastCallTime = currentTime;
        }
    }
}
