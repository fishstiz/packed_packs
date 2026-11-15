package io.github.fishstiz.packed_packs.util;

public class ScheduledAction {
    private final long delayNs;
    private long startNs;
    private Runnable task;

    public ScheduledAction(long delayMs, Runnable task) {
        this.delayNs = delayMs * 1_000_000L;
        this.task = task;
    }

    public float progressValue() {
        return Math.min(1.0f, (float) (System.nanoTime() - this.startNs) / this.delayNs);
    }

    public boolean poll() {
        if (task == null) {
            return true;
        }

        if (startNs == 0) {
            this.startNs = System.nanoTime();
            return false;
        }

        if (progressValue() >= 1f) {
            if (task != null) {
                task.run();
                task = null;
            }
            return true;
        }

        return false;
    }

    public void cancel() {
        task = null;
    }

    public boolean isDone() {
        return task == null || progressValue() >= 1f;
    }
}
