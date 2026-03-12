package io.github.fishstiz.packed_packs.gui.components.pack;

import io.github.fishstiz.fidgetz.gui.components.CyclicButton;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.ButtonSprites;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.gui.shapes.Size;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import io.github.fishstiz.packed_packs.util.PackUtil;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public record Query(
        boolean hideIncompatible,
        @Nullable SortOption sort,
        @Nullable String search,
        @Nullable String unmodifiedSearch
) implements Predicate<Pack> {
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
    public boolean test(Pack pack) {
        if (pack == null) {
            return false;
        }
        if (this.hideIncompatible && !pack.getCompatibility().isCompatible()) {
            return false;
        }
        return this.search == null || normalizeTitle(pack.getTitle().getString()).toLowerCase(Locale.ROOT).contains(this.search);
    }

    public boolean hasQuery() {
        return this.hideIncompatible || (this.search != null && !this.search.isEmpty()) || this.sort != null;
    }

    private static String normalizeTitle(String title) {
        return title.replaceAll("§.", "").trim();
    }

    public enum SortOption implements CyclicButton.SpriteOption {
        VANILLA("sort.vanilla", "sort_vanilla") {
            @Override
            public Comparator<Pack> comparator(SequencedCollection<Pack> packs) {
                return folderFirst((first, second) -> {
                    boolean builtInFirst = PackUtil.isBuiltIn(first);
                    boolean builtInSecond = PackUtil.isBuiltIn(second);

                    if (builtInFirst != builtInSecond) return builtInFirst ? 1 : -1;

                    boolean featureFirst = PackUtil.isFeature(first);
                    boolean featureSecond = PackUtil.isFeature(second);

                    if (featureFirst != featureSecond) return featureFirst ? 1 : -1;

                    return first.getTitle().getString().compareTo(second.getTitle().getString());
                });
            }
        },
        A_Z("sort.a_z", "sort_a_z") {
            @Override
            public Comparator<Pack> comparator(SequencedCollection<Pack> packs) {
                return folderFirst(Comparator.comparing(
                        pack -> normalizeTitle(pack.getTitle().getString()),
                        String.CASE_INSENSITIVE_ORDER
                ));
            }
        },
        Z_A("sort.z_a", "sort_z_a") {
            @Override
            public Comparator<Pack> comparator(SequencedCollection<Pack> packs) {
                return A_Z.comparator(packs).reversed();
            }
        },
        RECENT("sort.recent", "sort_recent") {
            @Override
            public Comparator<Pack> comparator(SequencedCollection<Pack> packs) {
                Map<Pack, Long> cache = buildTimestampCache(packs);
                return folderFirst(Comparator.<Pack, Long>comparing(cache::get).reversed());
            }
        },
        OLDEST("sort.oldest", "sort_oldest") {
            @Override
            public Comparator<Pack> comparator(SequencedCollection<Pack> packs) {
                return RECENT.comparator(packs).reversed();
            }
        };

        private final Component component;
        private final Tooltip tooltip;
        private final ButtonSprites sprites;

        SortOption(String key, String icon) {
            this.component = ResourceUtil.getText(key);
            this.tooltip = Tooltip.create(this.component);
            this.sprites = ButtonSprites.of(new Sprite(ResourceUtil.getIcon(icon), Size.of16()));
        }

        public abstract Comparator<Pack> comparator(SequencedCollection<Pack> packs);

        @Override
        public @NonNull Component text() {
            return this.component;
        }

        @Override
        public @Nullable Tooltip tooltip() {
            return this.tooltip;
        }

        @Override
        public @Nullable ButtonSprites sprites() {
            return this.sprites;
        }

        static Comparator<Pack> folderFirst(Comparator<Pack> base) {
            return Comparator.comparing((Pack pack) -> !(pack instanceof FolderPack)).thenComparing(base);
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

        private static Map<Pack, Long> buildTimestampCache(SequencedCollection<Pack> packs) {
            Map<Pack, Long> cache = new Object2LongLinkedOpenHashMap<>(packs.size());
            for (Pack pack : packs) cache.put(pack, PackUtil.getLastUpdatedEpochMs(pack));
            return cache;
        }
    }
}
