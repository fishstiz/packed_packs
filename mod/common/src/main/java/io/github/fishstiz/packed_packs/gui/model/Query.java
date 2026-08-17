package io.github.fishstiz.packed_packs.gui.model;

import io.github.fishstiz.packed_packs.PackedPacks;
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

public record Query(
        boolean hideIncompatible,
        @Nullable SortOption sort,
        @Nullable String search,
        @Nullable String unmodifiedSearch
) implements Predicate<PackEntry> {
    private static final Query EMPTY = new Query(false, null, null, null);

    public Query {
        search = search != null ? search.toLowerCase(Locale.ROOT) : null;
    }

    public static Query empty() {
        return EMPTY;
    }

    public Query withHideIncompatible(boolean hideIncompatible) {
        if (this.hideIncompatible == hideIncompatible) return this;
        return new Query(hideIncompatible, this.sort, this.search, this.unmodifiedSearch);
    }

    public Query withSort(SortOption sort) {
        if (Objects.equals(this.sort, sort)) return this;
        return new Query(this.hideIncompatible, sort, this.search, this.unmodifiedSearch);
    }

    public Query withSearch(String search) {
        String searchLower = search != null ? search.toLowerCase(Locale.ROOT) : null;
        if (Objects.equals(this.search, searchLower)) return this;
        return new Query(this.hideIncompatible, this.sort, searchLower, search);
    }

    @Override
    public boolean test(PackEntry pack) {
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

    public enum SortOption {
        VANILLA("packed_packs.sort.vanilla", "icon/sort_vanilla") {
            @Override
            public Comparator<PackEntry> comparator(SequencedCollection<PackEntry> packs) {
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
            public Comparator<PackEntry> comparator(SequencedCollection<PackEntry> packs) {
                return folderFirst(Comparator.comparing(
                        pack -> normalizeTitle(pack.title().getString()),
                        String.CASE_INSENSITIVE_ORDER
                ));
            }
        },
        Z_A("packed_packs.sort.z_a", "icon/sort_z_a") {
            @Override
            public Comparator<PackEntry> comparator(SequencedCollection<PackEntry> packs) {
                return A_Z.comparator(packs).reversed();
            }
        },
        RECENT("packed_packs.sort.recent", "icon/sort_recent") {
            @Override
            public Comparator<PackEntry> comparator(SequencedCollection<PackEntry> packs) {
                Map<PackEntry, Long> cache = buildTimestampCache(packs);
                return folderFirst(Comparator.<PackEntry, Long>comparing(cache::get).reversed());
            }
        },
        OLDEST("packed_packs.sort.oldest", "icon/sort_oldest") {
            @Override
            public Comparator<PackEntry> comparator(SequencedCollection<PackEntry> packs) {
                return RECENT.comparator(packs).reversed();
            }
        };

        private final String translationKey;
        private final String spritePath;

        SortOption(String key, String icon) {
            this.translationKey = key;
            this.spritePath = icon;
        }

        public abstract Comparator<PackEntry> comparator(SequencedCollection<PackEntry> packs);

        public Identifier icon() {
            return PackedPacks.id(spritePath);
        }

        public @NonNull Component text() {
            return Component.translatable(translationKey);
        }

        static Comparator<PackEntry> folderFirst(Comparator<PackEntry> base) {
            return Comparator.comparing((PackEntry pack) -> !(pack instanceof PackEntry.Parent)).thenComparing(base);
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

        private static Map<PackEntry, Long> buildTimestampCache(SequencedCollection<PackEntry> packs) {
            Map<PackEntry, Long> cache = new Object2LongLinkedOpenHashMap<>(packs.size());
            for (PackEntry pack : packs) cache.put(pack, getLastUpdatedEpochMs(pack));
            return cache;
        }

        private static long getLastUpdatedEpochMs(PackEntry pack) {
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
