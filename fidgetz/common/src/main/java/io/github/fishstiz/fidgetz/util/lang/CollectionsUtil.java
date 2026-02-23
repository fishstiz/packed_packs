package io.github.fishstiz.fidgetz.util.lang;

import it.unimi.dsi.fastutil.objects.*;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.*;

public class CollectionsUtil {
    private CollectionsUtil() {
    }

    public static <K, V> List<V> lookup(Collection<K> keys, Map<K, V> source) {
        List<V> result = new ObjectArrayList<>();
        for (K key : keys) {
            V v = source.get(key);
            if (v != null) result.add(v);
        }
        return result;
    }

    public static <K, V> List<K> reverseLookup(V value, Map<K, V> map) {
        List<K> keys = new ObjectArrayList<>();
        for (var entry : map.entrySet()) {
            if (Objects.equals(entry.getValue(), value)) {
                keys.add(entry.getKey());
            }
        }
        return keys;
    }

    public static <K, V> void updateReverseMapping(Map<K, V> map, V value, Collection<K> newKeys) {
        map.values().removeIf(v -> Objects.equals(v, value));
        for (K key : newKeys) {
            map.put(key, value);
        }
    }

    public static <T, K> Map<K, T> toMap(Collection<T> collection, Function<T, K> keyFn) {
        Map<K, T> map = new Object2ObjectOpenHashMap<>(collection.size());
        for (T item : collection) {
            map.put(keyFn.apply(item), item);
        }
        return map;
    }

    public static <T, R> List<R> extractNonNull(Collection<T> collection, Function<T, R> mapper) {
        List<R> result = new ObjectArrayList<>(collection.size());
        for (T item : collection) {
            if (item != null) {
                R value = mapper.apply(item);
                if (value != null) {
                    result.add(value);
                }
            }
        }
        return result;
    }

    public static <E> void forEach(E[] arr, Consumer<E> action) {
        for (E e : arr) {
            action.accept(e);
        }
    }

    public static <T> void forEachReverse(List<T> list, Consumer<T> action) {
        for (int i = list.size() - 1; i >= 0; i--) {
            action.accept(list.get(i));
        }
    }

    public static <T> void forEachDistinct(Collection<T> collection, Consumer<T> action) {
        Set<T> seen = new ObjectOpenHashSet<>(collection.size());
        for (T entry : collection) {
            if (entry != null && seen.add(entry)) {
                action.accept(entry);
            }
        }
    }

    public static <T> List<T> deduplicate(Collection<T> list) {
        List<T> deduplicated = new ObjectArrayList<>();
        forEachDistinct(list, deduplicated::add);
        return deduplicated;
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> List<T> addAll(Collection<T>... collections) {
        List<T> list = new ObjectArrayList<>();
        for (Collection<T> collection : collections) {
            list.addAll(collection);
        }
        return list;
    }

    public static <T extends Collection<E>, E> T addIf(T out, Collection<E> add, Predicate<E> predicate) {
        for (E e : add) {
            if (predicate.test(e)) out.add(e);
        }
        return out;
    }

    public static <E> boolean equalsOrdered(Collection<E> a, Collection<E> b) {
        if (a == b) return true;
        if (a == null || b == null || a.size() != b.size()) return false;
        Iterator<E> itA = a.iterator(), itB = b.iterator();
        while (itA.hasNext()) {
            if (!Objects.equals(itA.next(), itB.next())) return false;
        }
        return true;
    }

    public static <E, R, T extends Collection<R>> T map(Collection<E> collection, Function<E, R> mapper, IntFunction<T> collectionFactory) {
        T mapped = collectionFactory.apply(collection.size());
        for (E e : collection) {
            mapped.add(mapper.apply(e));
        }
        return mapped;
    }

    public static <E, R> R[] mapToArray(List<E> list, Function<E, R> mapper, IntFunction<R[]> arrayFactory) {
        R[] array = arrayFactory.apply(list.size());
        for (int i = 0; i < list.size(); i++) {
            array[i] = mapper.apply(list.get(i));
        }
        return array;
    }

    public static <E, R, T extends Collection<R>> T mapIf(Collection<E> collection, Predicate<E> filter, Function<E, R> mapper, Supplier<T> collectionFactory) {
        T result = collectionFactory.get();
        for (E element : collection) {
            if (filter.test(element)) {
                result.add(mapper.apply(element));
            }
        }
        return result;
    }

    public static <E, T extends Collection<E>> T filter(Collection<E> collection, Predicate<E> filter, Supplier<T> collectionFactory) {
        T result = collectionFactory.get();
        for (E e : collection) {
            if (filter.test(e)) result.add(e);
        }
        return result;
    }

    public static <E> boolean anyMatch(Collection<E> collection, Predicate<E> predicate) {
        for (E e : collection) {
            if (predicate.test(e)) return true;
        }
        return false;
    }

    public static <E, T> @Nullable E firstMatch(Collection<E> collection, T value, Function<E, T> mapper) {
        for (E e : collection) {
            if (Objects.equals(mapper.apply(e), value)) {
                return e;
            }
        }
        return null;
    }

    public static <T, K extends Comparable<? super K>> List<T> topoSort(
            Collection<T> collection,
            Function<T, K> keyFn,
            Function<T, @Nullable K[]> predecessorKeysFn
    ) {
        Map<K, T> nodes = toMap(collection, keyFn);
        Map<K, Set<K>> successorMap = new Object2ObjectOpenHashMap<>(collection.size());
        Map<K, Integer> inDegree = new Object2IntOpenHashMap<>(collection.size());

        for (K key : nodes.keySet()) {
            successorMap.put(key, new ObjectOpenHashSet<>());
            inDegree.put(key, 0);
        }

        for (T node : collection) {
            K currentKey = keyFn.apply(node);
            K[] predecessorKeys = predecessorKeysFn.apply(node);
            if (predecessorKeys != null) {
                for (K predecessorKey : predecessorKeys) {
                    if (predecessorKey != null && nodes.containsKey(predecessorKey)) {
                        successorMap.get(predecessorKey).add(currentKey);
                        inDegree.merge(currentKey, 1, Integer::sum);
                    }
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
            Set<K> seen = map(orderedList, keyFn, ObjectOpenHashSet::new);
            List<T> cyclicNodes = filter(collection, node -> !seen.contains(keyFn.apply(node)), ObjectArrayList::new);
            cyclicNodes.sort(Comparator.comparing(keyFn));
            orderedList.addAll(cyclicNodes);
        }

        return orderedList;
    }
}
