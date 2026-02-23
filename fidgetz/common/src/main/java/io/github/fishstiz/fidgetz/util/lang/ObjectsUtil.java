package io.github.fishstiz.fidgetz.util.lang;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class ObjectsUtil {
    private ObjectsUtil() {
    }

    public static <E> E pick(boolean condition, E ifTrue, E ifFalse) {
        return condition ? ifTrue : ifFalse;
    }

    public static <E> @Nullable E pick(E first, E second, Predicate<E> predicate) {
        if (predicate.test(first)) {
            return first;
        } else if (predicate.test(second)) {
            return second;
        }
        return null;
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <E> @Nullable E firstNonNull(E... args) {
        for (E arg : args) {
            if (arg != null) {
                return arg;
            }
        }
        return null;
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <E> @NonNull E firstNonNullOrDefault(@NonNull E defaultValue, E... args) {
        E value = firstNonNull(args);
        return value != null ? value : Objects.requireNonNull(defaultValue);
    }

    public static <E> boolean testNullable(@Nullable E obj, Predicate<@NonNull E> predicate) {
        return obj != null && predicate.test(obj);
    }

    public static <T, R> R mapOrDefault(T obj, R defaultValue, Function<@NonNull T, R> mapper) {
        return obj != null ? mapper.apply(obj) : defaultValue;
    }

    public static <T, R> @Nullable R mapOrNull(T obj, Function<@NonNull T, R> mapper) {
        return mapOrDefault(obj, null, mapper);
    }

    public static <T> void ifPresent(T obj, Consumer<@NonNull T> consumer) {
        if (obj != null) consumer.accept(obj);
    }

    public static <T> T getOrDefault(@Nullable T obj, T defaultValue) {
        return obj != null ? obj : defaultValue;
    }

    public static <T> T peek(T value, Consumer<? super T> action) {
        action.accept(value);
        return value;
    }
}
