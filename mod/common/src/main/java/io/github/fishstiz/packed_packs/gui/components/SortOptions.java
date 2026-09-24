package io.github.fishstiz.packed_packs.gui.components;

import io.github.fishstiz.fidgetz.v0.gui.components.FZPopoverMenuItem;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.pack.PackNode;
import io.github.fishstiz.packed_packs.util.PackUtil;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.PackSource;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

import static io.github.fishstiz.packed_packs.util.GuiUtils.padded16Sprite;

public enum SortOptions implements SortOption {
    NONE("packed_packs.sort.none", "icon/cross") { // todo create icon
        @Override
        public boolean canSort() {
            return false;
        }
    },
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
            //noinspection NullableProblems
            return folderFirst(Comparator.comparing(
                    pack -> ChatFormatting.stripFormatting(pack.title().getString()),
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

    SortOptions(String key, String icon) {
        this.translationKey = key;
        this.spritePath = icon;
    }

    public Identifier icon() {
        return PackedPacks.id(spritePath);
    }

    public Component text() {
        return Component.translatable(translationKey);
    }

    static Comparator<PackNode> folderFirst(Comparator<PackNode> base) {
        return Comparator.comparing((PackNode pack) -> !(pack instanceof PackNode.Parent)).thenComparing(base);
    }

    public static SortOption getOrDefault(@Nullable String name) {
        if (name == null || name.equals(NONE.name())) {
            return VANILLA;
        }

        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return VANILLA;
        }
    }

    public static List<FZPopoverMenuItem> menuItems(boolean includeNone, Consumer<SortOption> clickHandler) {
        SortOption[] options = SortOptions.values();
        List<FZPopoverMenuItem> items = new ArrayList<>(options.length);
        for (int i = includeNone ? 0 : 1; i < options.length; i++) {
            SortOption option = options[i];
            items.add(FZPopoverMenuItem.builder()
                    .message(option.text())
                    .icon(padded16Sprite(option.icon()))
                    .onPress(() -> clickHandler.accept(option))
                    .build());
        }
        return items;
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
