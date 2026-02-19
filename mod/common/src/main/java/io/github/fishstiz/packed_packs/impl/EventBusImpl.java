package io.github.fishstiz.packed_packs.impl;


import io.github.fishstiz.fidgetz.util.lang.CollectionsUtil;
import io.github.fishstiz.packed_packs.api.Event;
import io.github.fishstiz.packed_packs.api.EventBus;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.resources.Identifier;

import java.util.*;
import java.util.function.Consumer;

public final class EventBusImpl implements EventBus {
    private Delegate delegate = new Collector();

    EventBusImpl() {
    }

    @Override
    public <T extends Event> void register(Class<T> eventClass, Identifier id, Consumer<T> listener) {
        this.delegate.register(eventClass, id, listener);
    }

    @Override
    public <T extends Event> void register(Class<T> eventClass, Identifier id, Identifier loadAfter, Consumer<T> listener) {
        this.delegate.register(eventClass, id, loadAfter, listener);
    }

    @Override
    public <T extends Event> void register(Class<T> eventClass, Identifier id, List<Identifier> loadAfter, Consumer<T> listener) {
        this.delegate.register(eventClass, id, loadAfter, listener);
    }

    @Override
    public void post(Event event) {
        this.delegate.post(event);
    }

    void freeze() {
        this.delegate = new Dispatcher(this.delegate.getEventListeners());
    }

    interface Delegate extends EventBus {
        Map<Class<? extends Event>, Consumer<Event>[]> getEventListeners();
    }

    private static class Collector implements Delegate {
        private final Map<Class<? extends Event>, List<Listener<Event>>> eventListeners = new Reference2ReferenceOpenHashMap<>();

        Collector() {
        }

        @SuppressWarnings("unchecked")
        private void register(Class<? extends Event> eventClass, Listener<? extends Event> listeners) {
            this.eventListeners.computeIfAbsent(eventClass, e -> new ObjectArrayList<>()).add((Listener<Event>) listeners);
        }

        @Override
        public <T extends Event> void register(Class<T> eventClass, Identifier id, Consumer<T> listener) {
            this.register(eventClass, new Listener<>(id, listener));
        }

        @Override
        public <T extends Event> void register(Class<T> eventClass, Identifier id, Identifier loadAfter, Consumer<T> listener) {
            this.register(eventClass, new Listener<>(id, listener, loadAfter));
        }

        @Override
        public <T extends Event> void register(Class<T> eventClass, Identifier id, List<Identifier> loadAfter, Consumer<T> listener) {
            this.register(eventClass, new Listener<>(id, listener, loadAfter.toArray(Identifier[]::new)));
        }

        @Override
        public void post(Event event) {
            throw new IllegalStateException("Cannot post event while initializing EventBus.");
        }

        @Override
        public Map<Class<? extends Event>, Consumer<Event>[]> getEventListeners() {
            Reference2ReferenceOpenHashMap<Class<? extends Event>, Consumer<Event>[]> bakedMap =
                    new Reference2ReferenceOpenHashMap<>(this.eventListeners.size());

            for (var entry : this.eventListeners.entrySet()) {
                Class<? extends Event> eventClass = entry.getKey();
                List<Listener<Event>> raw = entry.getValue();
                List<Listener<Event>> sorted = (raw.size() > 1)
                        ? CollectionsUtil.topoSort(raw, Listener::id, Listener::dependencies)
                        : raw;

                @SuppressWarnings("unchecked")
                Consumer<Event>[] bakedArray = new Consumer[sorted.size()];
                for (int i = 0; i < sorted.size(); i++) {
                    bakedArray[i] = sorted.get(i).consumer();
                }

                bakedMap.put(eventClass, bakedArray);
            }

            bakedMap.trim();

            return bakedMap;
        }

        record Listener<T extends Event>(Identifier id, Consumer<T> consumer, Identifier... dependencies) {
        }
    }

    private static class Dispatcher implements Delegate {
        private final Map<Class<? extends Event>, Consumer<Event>[]> eventListeners;

        Dispatcher(Map<Class<? extends Event>, Consumer<Event>[]> eventListeners) {
            this.eventListeners = eventListeners;
        }

        private static void throwFrozenError() {
            throw new IllegalArgumentException("EventBus is already frozen. Cannot register listener.");
        }

        @Override
        public <T extends Event> void register(Class<T> eventClass, Identifier id, Consumer<T> listener) {
            throwFrozenError();
        }

        @Override
        public <T extends Event> void register(Class<T> eventClass, Identifier id, Identifier loadAfter, Consumer<T> listener) {
            throwFrozenError();
        }

        @Override
        public <T extends Event> void register(Class<T> eventClass, Identifier id, List<Identifier> loadAfter, Consumer<T> listener) {
            throwFrozenError();
        }

        @Override
        public void post(Event event) {
            Consumer<Event>[] listeners = this.eventListeners.get(event.getClass());
            if (listeners != null) {
                for (Consumer<Event> listener : listeners) {
                    listener.accept(event);
                }
            }
        }

        @Override
        public Map<Class<? extends Event>, Consumer<Event>[]> getEventListeners() {
            return this.eventListeners;
        }
    }
}
