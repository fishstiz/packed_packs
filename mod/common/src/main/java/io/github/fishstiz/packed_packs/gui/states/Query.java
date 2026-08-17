package io.github.fishstiz.packed_packs.gui.states;

import io.github.fishstiz.packed_packs.gui.components.SortOption;
import io.github.fishstiz.packed_packs.pack.PackNode;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public final class Query implements Predicate<PackNode> {
    private static final Query EMPTY = new Query(false, null, null, null);
    private final boolean hideIncompatible;
    private final @Nullable SortOption sort;
    private final @Nullable String search;
    private final @Nullable String searchLower;

    private Query(boolean hideIncompatible, @Nullable SortOption sort, @Nullable String search, @Nullable String searchLower) {
        this.hideIncompatible = hideIncompatible;
        this.sort = sort;
        this.search = search;
        this.searchLower = searchLower;
    }

    public Query(boolean hideIncompatible, @Nullable SortOption sort, @Nullable String search) {
        this.hideIncompatible = hideIncompatible;
        this.sort = sort;
        this.search = search;
        this.searchLower = search == null ? null : search.toLowerCase(Locale.ROOT);
    }

    public static Query empty() {
        return EMPTY;
    }

    public Query withHideIncompatible(boolean hideIncompatible) {
        if (this.hideIncompatible == hideIncompatible) return this;
        return new Query(hideIncompatible, this.sort, this.search, this.searchLower);
    }

    public Query withSort(SortOption sort) {
        if (Objects.equals(this.sort, sort)) return this;
        return new Query(this.hideIncompatible, sort, this.search, this.searchLower);
    }

    public Query withSearch(String search) {
        String searchLower = search != null ? search.toLowerCase(Locale.ROOT) : null;
        if (Objects.equals(this.search, searchLower)) return this;
        return new Query(this.hideIncompatible, this.sort, search, searchLower);
    }

    @Override
    public boolean test(PackNode pack) {
        if (pack == null) {
            return false;
        }
        if (this.hideIncompatible && !pack.compatibility().isCompatible()) {
            return false;
        }
        if (searchLower == null) {
            return true;
        }
        return pack.stream().anyMatch(node ->
                ChatFormatting.stripFormatting(node.title().getString()).toLowerCase(Locale.ROOT).contains(searchLower)
        );
    }

    public boolean hasQuery() {
        return this.hideIncompatible
               || (this.search != null && !this.search.isEmpty())
               || (this.sort != null && !(this.sort instanceof SortOption.Locked));
    }

    public boolean hideIncompatible() {
        return hideIncompatible;
    }

    public @Nullable SortOption sort() {
        return sort;
    }

    public @Nullable String search() {
        return search;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Query) obj;
        return this.hideIncompatible == that.hideIncompatible &&
               Objects.equals(this.sort, that.sort) &&
               Objects.equals(this.search, that.search);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hideIncompatible, sort, search);
    }

    @Override
    public String toString() {
        return "Query[" +
               "hideIncompatible=" + hideIncompatible + ", " +
               "sort=" + sort + ", " +
               "search=" + search + ']';
    }
}
