package io.github.fishstiz.packed_packs.gui.components;

import io.github.fishstiz.packed_packs.pack.PackNode;
import io.github.fishstiz.packed_packs.util.GuiUtils;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.SequencedCollection;

public interface SortOption {
    String name();

    ResourceLocation icon();

    Component text();

    Comparator<PackNode> comparator(SequencedCollection<PackNode> packs);

    record Locked(@Nullable SortOption sort) implements SortOption {
        @Override
        public String name() {
            return "LOCKED";
        }

        @Override
        public ResourceLocation icon() {
            return GuiUtils.LOCK_SPRITE_SMALL;
        }

        @Override
        public Component text() {
            return CommonComponents.EMPTY;
        }

        @Override
        public Comparator<PackNode> comparator(SequencedCollection<PackNode> packs) {
            return (ignoredA, ignoredB) -> 0;
        }
    }
}
