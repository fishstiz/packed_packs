package io.github.fishstiz.packed_packs.gui.components;

import io.github.fishstiz.packed_packs.pack.PackNode;
import io.github.fishstiz.packed_packs.util.GuiUtils;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.SequencedCollection;

public interface SortOption {
    String name();

    Identifier icon();

    Component text();

    default @Nullable Comparator<PackNode> comparator(SequencedCollection<PackNode> packs) {
        return null;
    }

    default boolean canSort() {
        return true;
    }

    record Locked(@Nullable SortOption sort) implements SortOption {
        @Override
        public String name() {
            return "LOCKED";
        }

        @Override
        public Identifier icon() {
            return GuiUtils.LOCK_SPRITE_SMALL;
        }

        @Override
        public Component text() {
            return CommonComponents.EMPTY;
        }

        @Override
        public boolean canSort() {
            return false;
        }
    }
}
