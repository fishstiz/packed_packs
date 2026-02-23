package io.github.fishstiz.packed_packs.gui;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

public class HistoryManager<S> {
    private static final int DEFAULT_CAPACITY = 25;
    private final int capacity;
    private final Deque<S> history;
    private final Deque<S> undone;

    public HistoryManager(S initialState, int capacity) {
        this.capacity = capacity;
        this.history = new ArrayDeque<>(capacity);
        this.undone = new ArrayDeque<>(capacity);
        this.push(initialState);
    }

    public HistoryManager(S initialState) {
        this(initialState, DEFAULT_CAPACITY);
    }

    public void push(S state) {
        if (state == null) {
            return;
        }
        if (!this.history.isEmpty() && this.history.peekLast().equals(state)) {
            return;
        }
        while (this.history.size() >= this.capacity) {
            this.history.removeFirst();
        }
        this.undone.clear();
        this.history.addLast(state);
    }

    public Optional<S> undo() {
        if (this.history.size() > 1) {
            this.undone.addLast(this.history.removeLast());
            return Optional.of(this.history.getLast());
        }
        return Optional.empty();
    }

    public Optional<S> redo() {
        if (!this.undone.isEmpty()) {
            S state = this.undone.removeLast();
            this.history.addLast(state);
            return Optional.of(state);
        }
        return Optional.empty();
    }

    public void reset(S initialState) {
        this.history.clear();
        this.undone.clear();
        this.push(initialState);
    }

    public List<S> getStack() {
        List<S> stack = new ObjectArrayList<>(this.history.size() + this.undone.size());
        stack.addAll(this.history);
        stack.addAll(this.undone);
        return stack;
    }

    public int stackIndex() {
        if (this.history.isEmpty()) {
            return -1;
        }
        return this.history.size() - 1;
    }

    public Optional<S> getState(int index) {
        List<S> stack = this.getStack();
        int totalSize = stack.size();

        if (index < 0 || index >= totalSize) {
            return Optional.empty();
        }

        this.history.clear();
        this.undone.clear();

        for (int i = 0; i <= index; i++) {
            this.history.addLast(stack.get(i));
        }
        for (int i = index + 1; i < totalSize; i++) {
            this.undone.addLast(stack.get(i));
        }
        if (!this.history.isEmpty()) {
            return Optional.of(this.history.getLast());
        }

        return Optional.empty();
    }
}
