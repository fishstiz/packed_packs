package io.github.fishstiz.packed_packs.gui;

import java.util.Optional;

public class HistoryManager<S> {
    private static final int DEFAULT_CAPACITY = 25;
    private final int capacity;
    private final S[] buffer;
    private int start = 0;
    private int size = 0;
    private int cursor = -1;

    @SuppressWarnings("unchecked")
    public HistoryManager(S initialState, int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than 0");
        }
        this.capacity = capacity;
        this.buffer = (S[]) new Object[capacity];
        this.push(initialState);
    }

    public HistoryManager(S initialState) {
        this(initialState, DEFAULT_CAPACITY);
    }

    private S currentState() {
        return cursor == -1 ? null : buffer[cursor];
    }

    public void push(S state) {
        if (state == null) {
            return;
        }

        S current = currentState();
        if (current != null && current.equals(state)) {
            return;
        }

        if (cursor != -1) {
            int currentOffset = (cursor - start + capacity) % capacity;
            size = currentOffset + 1;
        }

        if (size == capacity) {
            start = (start + 1) % capacity;
            cursor = (start + size - 1) % capacity;
        } else {
            cursor = (start + size) % capacity;
            size++;
        }

        buffer[cursor] = state;
    }

    public Optional<S> undo() {
        if (cursor == start || size <= 1) {
            return Optional.empty();
        }
        cursor = (cursor - 1 + capacity) % capacity;
        return Optional.of(buffer[cursor]);
    }

    public Optional<S> redo() {
        int tail = (start + size - 1) % capacity;
        if (cursor == tail || size == 0) {
            return Optional.empty();
        }
        cursor = (cursor + 1) % capacity;
        return Optional.of(buffer[cursor]);
    }

    public void reset(S initialState) {
        for (int i = 0; i < capacity; i++) {
            buffer[i] = null;
        }
        start = 0;
        size = 0;
        cursor = -1;
        push(initialState);
    }
}
