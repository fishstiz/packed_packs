package io.github.fishstiz.packed_packs.impl;

import io.github.fishstiz.fidgetz.v0.utils.CollectionUtils;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.api.Event;
import io.github.fishstiz.packed_packs.api.EventBus;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public final class EventBusImpl implements EventBus {
    private Delegate delegate = new Collector();

    EventBusImpl() {
    }

    @Override
    public <T extends Event> void register(Class<T> eventClass, ResourceLocation id, Consumer<T> listener) {
        this.delegate.register(eventClass, id, listener);
    }

    @Override
    public <T extends Event> void register(Class<T> eventClass, ResourceLocation id, ResourceLocation loadAfter, Consumer<T> listener) {
        this.delegate.register(eventClass, id, loadAfter, listener);
    }

    @Override
    public <T extends Event> void register(Class<T> eventClass, ResourceLocation id, List<ResourceLocation> loadAfter, Consumer<T> listener) {
        this.delegate.register(eventClass, id, loadAfter, listener);
    }

    @Override
    public <T extends Event> T post(T event) {
        return this.delegate.post(event);
    }

    void freeze() {
        this.delegate = new Dispatcher(this.delegate.getEventListeners());
    }

    interface Delegate extends EventBus {
        Map<Class<? extends Event>, Consumer<Event>[]> getEventListeners();
    }

    private static class Collector implements Delegate {
        private final Map<Class<? extends Event>, Set<Listener<Event>>> eventListeners = new Reference2ReferenceOpenHashMap<>();

        Collector() {
        }

        @SuppressWarnings("unchecked")
        private void register(Class<? extends Event> eventClass, Listener<? extends Event> listener) {
            Set<Listener<Event>> listeners = this.eventListeners.computeIfAbsent(eventClass, e -> new ObjectOpenHashSet<>());
            if (!listeners.add((Listener<Event>) listener)) {
                PackedPacks.LOGGER.warn(
                        "[packed_packs] Skipping duplicate event listener with id '{}' found for event '{}'",
                        listener.id, eventClass.getName()
                );
            }
        }

        @Override
        public <T extends Event> void register(Class<T> eventClass, ResourceLocation id, Consumer<T> listener) {
            this.register(eventClass, new Listener<>(id, listener));
        }

        @Override
        public <T extends Event> void register(Class<T> eventClass, ResourceLocation id, ResourceLocation loadAfter, Consumer<T> listener) {
            this.register(eventClass, new Listener<>(id, listener, loadAfter));
        }

        @Override
        public <T extends Event> void register(Class<T> eventClass, ResourceLocation id, List<ResourceLocation> loadAfter, Consumer<T> listener) {
            this.register(eventClass, new Listener<>(id, listener, loadAfter.toArray(ResourceLocation[]::new)));
        }

        @Override
        public <T extends Event> T post(T event) {
            throw new IllegalStateException("Cannot post event while initializing EventBus.");
        }

        @Override
        public Map<Class<? extends Event>, Consumer<Event>[]> getEventListeners() {
            if (this.eventListeners.isEmpty()) {
                return Collections.emptyMap();
            }

            Reference2ReferenceOpenHashMap<Class<? extends Event>, Consumer<Event>[]> bakedMap =
                    new Reference2ReferenceOpenHashMap<>(this.eventListeners.size(), 0.99f);

            for (var entry : this.eventListeners.entrySet()) {
                Class<? extends Event> eventClass = entry.getKey();
                Set<Listener<Event>> raw = entry.getValue();
                List<Listener<Event>> sorted = (raw.size() > 1)
                        ? topoSort(raw, Listener::id, Listener::dependencies)
                        : List.copyOf(raw);

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

        public static <T, K extends Comparable<? super K>> List<T> topoSort(
                Collection<T> collection,
                Function<T, K> keyFn,
                Function<T, K[]> predecessorKeysFn
        ) {
            Map<K, T> nodes = CollectionUtils.toMap(collection, keyFn);
            Map<K, Set<K>> successorMap = new Object2ObjectOpenHashMap<>(collection.size());
            Map<K, Integer> inDegree = new Object2IntOpenHashMap<>(collection.size());

            for (K key : nodes.keySet()) {
                successorMap.put(key, new ObjectOpenHashSet<>());
                inDegree.put(key, 0);
            }

            for (T node : collection) {
                K currentKey = keyFn.apply(node);
                K[] predecessorKeys = predecessorKeysFn.apply(node);
                for (K predecessorKey : predecessorKeys) {
                    if (nodes.containsKey(predecessorKey)) {
                        successorMap.get(predecessorKey).add(currentKey);
                        inDegree.merge(currentKey, 1, Integer::sum);
                    }
                }
            }

            Queue<K> queue = new PriorityQueue<>();
            for (Map.Entry<K, Integer> entry : inDegree.entrySet()) {
                if (entry.getValue() == 0) {
                    queue.add(entry.getKey());
                }
            }

            List<T> orderedList = new ObjectArrayList<>(collection.size());
            while (!queue.isEmpty()) {
                K currentKey = queue.poll();
                orderedList.add(nodes.get(currentKey));

                for (K dependentKey : successorMap.getOrDefault(currentKey, Collections.emptySet())) {
                    int newInDegree = inDegree.get(dependentKey) - 1;
                    inDegree.put(dependentKey, newInDegree);

                    if (newInDegree == 0) {
                        queue.add(dependentKey);
                    }
                }
            }

            if (orderedList.size() != collection.size()) {
                Set<K> seen = CollectionUtils.map(orderedList, keyFn, ObjectOpenHashSet::new);
                List<T> cyclicNodes = CollectionUtils.filter(collection, node -> !seen.contains(keyFn.apply(node)));
                cyclicNodes.sort(Comparator.comparing(keyFn));
                orderedList.addAll(cyclicNodes);
            }

            return orderedList;
        }


        record Listener<T extends Event>(ResourceLocation id, Consumer<T> consumer, ResourceLocation... dependencies) {
            @Override
            public boolean equals(Object obj) {
                if (obj == this) return true;
                return obj instanceof Listener<?> that && this.id.equals(that.id);
            }

            @Override
            public int hashCode() {
                return this.id.hashCode();
            }
        }
    }

    private static class Dispatcher implements Delegate {
        private final Map<Class<? extends Event>, Consumer<Event>[]> eventListeners;

        Dispatcher(Map<Class<? extends Event>, Consumer<Event>[]> eventListeners) {
            this.eventListeners = eventListeners;
        }

        private static void throwFrozenError() {
            throw new IllegalStateException("EventBus is already frozen. Cannot register listener.");
        }

        @Override
        public <T extends Event> void register(Class<T> eventClass, ResourceLocation id, Consumer<T> listener) {
            throwFrozenError();
        }

        @Override
        public <T extends Event> void register(Class<T> eventClass, ResourceLocation id, ResourceLocation loadAfter, Consumer<T> listener) {
            throwFrozenError();
        }

        @Override
        public <T extends Event> void register(Class<T> eventClass, ResourceLocation id, List<ResourceLocation> loadAfter, Consumer<T> listener) {
            throwFrozenError();
        }

        @Override
        public <T extends Event> T post(T event) {
            Consumer<Event>[] listeners = this.eventListeners.get(event.getClass());
            if (listeners != null) {
                for (Consumer<Event> listener : listeners) {
                    listener.accept(event);
                }
            }
            return event;
        }

        @Override
        public Map<Class<? extends Event>, Consumer<Event>[]> getEventListeners() {
            return this.eventListeners;
        }
    }
}
