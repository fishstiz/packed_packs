package io.github.fishstiz.packed_packs.gui.model;

import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.pack.PackNode;
import io.github.fishstiz.packed_packs.util.PackUtil;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.PackSource;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        return this.search == null || normalizeTitle(pack.title().getString()).toLowerCase(Locale.ROOT).contains(this.search);
    }

    public boolean hasQuery() {
        return this.hideIncompatible || (this.search != null && !this.search.isEmpty()) || this.sort != null;
    }

    private static String normalizeTitle(String title) {
        return title.replaceAll("§.", "").trim();
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


    public enum SortOption {
        VANILLA("packed_packs.sort.vanilla", "icon/sort_vanilla") {
            @Override
            public Comparator<PackNode> comparator(SequencedCollection<PackNode> packs) {
                return folderFirst((first, second) -> {
                    PackSource firstPackSource = first.packSource();
                    PackSource secondPackSource = second.packSource();

                    boolean builtInFirst = PackUtil.isBuiltIn(firstPackSource);
                    boolean builtInSecond = PackUtil.isBuiltIn(secondPackSource);

                    if (builtInFirst != builtInSecond) return builtInFirst ? 1 : -1;

                    boolean featureFirst = PackUtil.isFeature(firstPackSource);
                    boolean featureSecond = PackUtil.isFeature(secondPackSource);

                    if (featureFirst != featureSecond) return featureFirst ? 1 : -1;

                    return first.title().getString().compareTo(second.title().getString());
                });
            }
        },
        A_Z("packed_packs.sort.a_z", "icon/sort_a_z") {
            @Override
            public Comparator<PackNode> comparator(SequencedCollection<PackNode> packs) {
                return folderFirst(Comparator.comparing(
                        pack -> normalizeTitle(pack.title().getString()),
                        String.CASE_INSENSITIVE_ORDER
                ));
            }
        },
        Z_A("packed_packs.sort.z_a", "icon/sort_z_a") {
            @Override
            public Comparator<PackNode> comparator(SequencedCollection<PackNode> packs) {
                return A_Z.comparator(packs).reversed();
            }
        },
        RECENT("packed_packs.sort.recent", "icon/sort_recent") {
            @Override
            public Comparator<PackNode> comparator(SequencedCollection<PackNode> packs) {
                Map<PackNode, Long> cache = buildTimestampCache(packs);
                return folderFirst(Comparator.<PackNode, Long>comparing(cache::get).reversed());
            }
        },
        OLDEST("packed_packs.sort.oldest", "icon/sort_oldest") {
            @Override
            public Comparator<PackNode> comparator(SequencedCollection<PackNode> packs) {
                return RECENT.comparator(packs).reversed();
            }
        };

        private final String translationKey;
        private final String spritePath;

        SortOption(String key, String icon) {
            this.translationKey = key;
            this.spritePath = icon;
        }

        public abstract Comparator<PackNode> comparator(SequencedCollection<PackNode> packs);

        public Identifier icon() {
            return PackedPacks.id(spritePath);
        }

        public @NonNull Component text() {
            return Component.translatable(translationKey);
        }

        static Comparator<PackNode> folderFirst(Comparator<PackNode> base) {
            return Comparator.comparing((PackNode pack) -> !(pack instanceof PackNode.Parent)).thenComparing(base);
        }

        public static SortOption getOrDefault(String name) {
            if (name == null) {
                return VANILLA;
            }

            try {
                return valueOf(name);
            } catch (IllegalArgumentException e) {
                return VANILLA;
            }
        }

        private static Map<PackNode, Long> buildTimestampCache(SequencedCollection<PackNode> packs) {
            Map<PackNode, Long> cache = new Object2LongLinkedOpenHashMap<>(packs.size());
            for (PackNode pack : packs) cache.put(pack, getLastUpdatedEpochMs(pack));
            return cache;
        }

        private static long getLastUpdatedEpochMs(PackNode pack) {
            Path path = pack.path();
            if (path == null) {
                return -1;
            }
            try {
                return Files.getLastModifiedTime(path).toInstant().toEpochMilli();
            } catch (IOException e) {
                PackedPacks.LOGGER.error("[packed_packs] Failed to get age of pack '{}'", pack.id());
                return -1;
            }
        }
    }
}
