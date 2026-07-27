package io.github.fishstiz.fidgetz.util.lang;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class FunctionsUtil {
    private static final Runnable NO_OP = () -> {
    };
    private static final Consumer<Object> NO_OP_CONSUMER = o -> {
    };
    private static final Supplier<Object> NULL_SUPPLIER = () -> null;

    public static Runnable nop() {
        return NO_OP;
    }

    @SuppressWarnings("unchecked")
    public static <T> Consumer<T> nopConsumer() {
        return (Consumer<T>) NO_OP_CONSUMER;
    }

    @SuppressWarnings("unchecked")
    public static <T> Supplier<@Nullable T> nullSupplier() {
        return (Supplier<T>) NULL_SUPPLIER;
    }
}
