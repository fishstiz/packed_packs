package io.github.fishstiz.fidgetz.gui.components.contextmenu;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.*;

public class ContextMenuItemBuilder {
    final List<MenuItem> items;

    private ContextMenuItemBuilder(List<MenuItem> items) {
        this.items = items;
    }

    public ContextMenuItemBuilder() {
        this(new ArrayList<>());
    }

    protected ContextMenuItemBuilder self() {
        return this;
    }

    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    public ContextMenuItemBuilder add(MenuItem option) {
        this.items.add(option);
        return this.self();
    }

    public ContextMenuItemBuilder addAll(Collection<? extends MenuItem> options) {
        this.items.addAll(options);
        return this.self();
    }

    public ContextMenuItemBuilder separator() {
        return this.isEmpty() || this.items.getLast() != MenuItem.SEPARATOR ? this.add(MenuItem.SEPARATOR) : this.self();
    }

    public ContextMenuItemBuilder separatorIfNonEmpty() {
        return this.isEmpty() ? this.self() : this.separator();
    }

    public ContextMenuItemBuilder simpleItem(Component text, BooleanSupplier active, Runnable action) {
        return this.add(MenuItem.builder(text).activeWhen(active).action(action).build());
    }

    public ContextMenuItemBuilder simpleItem(Component text, Runnable action) {
        return this.add(MenuItem.builder(text).action(action).build());
    }

    public ContextMenuItemBuilder parent(Function<List<MenuItem>, MenuItem> parentItemFactory, Consumer<ContextMenuItemBuilder> builderAction) {
        ContextMenuItemBuilder builder = new ContextMenuItemBuilder();
        builderAction.accept(builder);
        return this.add(parentItemFactory.apply(builder.build()));
    }

    public ContextMenuItemBuilder parent(Component text, Consumer<ContextMenuItemBuilder> builderAction) {
        return this.parent(children -> MenuItem.builder(text).addChildren(children).build(), builderAction);
    }

    public ConditionalChain when(boolean condition) {
        return new ConditionalChain(this.self(), condition);
    }

    public <T> PredicateChain<T> when(T t, Predicate<T> predicate) {
        return new PredicateChain<>(this.self(), predicate.test(t), t);
    }

    public <T> NonNullPredicateChain<T> whenNonNull(T t) {
        return new NonNullPredicateChain<>(this.self(), t != null, t);
    }

    public <E> IterableChain<E> iterate(Iterable<E> iterable) {
        return new IterableChain<>(this, iterable);
    }

    public ContextMenuItemBuilder peek(Consumer<List<MenuItem>> action) {
        action.accept(Collections.unmodifiableList(this.items));
        return this.self();
    }

    public ContextMenuItemBuilder then(Consumer<ContextMenuItemBuilder> builderAction) {
        builderAction.accept(this.self());
        return this.self();
    }

    public List<MenuItem> build() {
        return new ArrayList<>(this.items);
    }

    public static class IterableChain<E> {
        protected final ContextMenuItemBuilder builder;
        protected final Iterable<E> iterable;

        IterableChain(ContextMenuItemBuilder builder, Iterable<E> iterable) {
            this.builder = builder;
            this.iterable = iterable;
        }

        public ContextMenuItemBuilder map(Function<E, MenuItem> itemFactory) {
            for (E e : this.iterable) {
                this.builder.add(itemFactory.apply(e));
            }
            return this.builder;
        }

        public ContextMenuItemBuilder forEach(BiConsumer<E, ContextMenuItemBuilder> action) {
            for (E e : this.iterable) {
                action.accept(e, this.builder);
            }
            return this.builder;
        }
    }

    public static class ElseChain extends ContextMenuItemBuilder {
        protected final ContextMenuItemBuilder builder;
        protected final boolean conditionMatched;

        protected ElseChain(ContextMenuItemBuilder builder, boolean conditionMatched) {
            super(builder.items);
            this.builder = builder;
            this.conditionMatched = conditionMatched;
        }

        @Override
        protected ContextMenuItemBuilder self() {
            return this.builder;
        }

        public ContextMenuItemBuilder orElse(Consumer<ContextMenuItemBuilder> builderAction) {
            if (!this.conditionMatched) builderAction.accept(this.self());
            return this.self();
        }
    }

    public static class PredicateElseChain<T> extends ElseChain {
        private final T t;

        protected PredicateElseChain(ContextMenuItemBuilder builder, boolean conditionMatched, T t) {
            super(builder, conditionMatched);
            this.t = t;
        }

        public ContextMenuItemBuilder orElse(BiConsumer<T, ContextMenuItemBuilder> builderAction) {
            if (!this.conditionMatched) builderAction.accept(this.t, this.builder);
            return this.builder;
        }
    }

    public static class ConditionalChain {
        protected final ContextMenuItemBuilder builder;
        protected final boolean condition;

        ConditionalChain(ContextMenuItemBuilder builder, boolean condition) {
            this.builder = builder;
            this.condition = condition;
        }

        public ElseChain ifTrue(Consumer<ContextMenuItemBuilder> builderAction) {
            if (this.condition) builderAction.accept(this.builder);
            return new ElseChain(this.builder, this.condition);
        }

        public ElseChain ifFalse(Consumer<ContextMenuItemBuilder> builderAction) {
            if (!this.condition) builderAction.accept(this.builder);
            return new ElseChain(this.builder, !this.condition);
        }
    }

    public static class PredicateChain<T> extends ConditionalChain {
        protected final T t;

        PredicateChain(ContextMenuItemBuilder builder, boolean condition, T t) {
            super(builder, condition);
            this.t = t;
        }

        @Override
        public PredicateElseChain<T> ifTrue(Consumer<ContextMenuItemBuilder> builderAction) {
            super.ifTrue(builderAction);
            return new PredicateElseChain<>(this.builder, this.condition, this.t);
        }

        @Override
        public PredicateElseChain<T> ifFalse(Consumer<ContextMenuItemBuilder> builderAction) {
            super.ifFalse(builderAction);
            return new PredicateElseChain<>(this.builder, !this.condition, this.t);
        }

        public PredicateElseChain<T> ifTrue(BiConsumer<T, ContextMenuItemBuilder> builderAction) {
            if (this.condition) builderAction.accept(this.t, this.builder);
            return new PredicateElseChain<>(this.builder, this.condition, this.t);
        }

        public PredicateElseChain<T> ifFalse(BiConsumer<T, ContextMenuItemBuilder> builderAction) {
            if (!this.condition) builderAction.accept(this.t, this.builder);
            return new PredicateElseChain<>(this.builder, !this.condition, this.t);
        }
    }

    public static class NonNullPredicateChain<T> extends PredicateChain<T> {
        NonNullPredicateChain(ContextMenuItemBuilder builder, boolean condition, T t) {
            super(builder, condition, t);
        }

        @Override
        public PredicateElseChain<T> ifTrue(BiConsumer<@NotNull T, ContextMenuItemBuilder> builderAction) {
            if (this.condition) builderAction.accept(Objects.requireNonNull(this.t), this.builder);
            return new PredicateElseChain<>(this.builder, this.condition, this.t);
        }
    }
}