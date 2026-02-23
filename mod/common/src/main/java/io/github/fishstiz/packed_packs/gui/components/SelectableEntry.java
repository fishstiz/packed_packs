package io.github.fishstiz.packed_packs.gui.components;

// copied from 1.21.11
public interface SelectableEntry {
    default boolean mouseOverIcon(int relX, int relY, int size) {
        return relX >= 0 && relX < size && relY >= 0 && relY < size;
    }

    default boolean mouseOverLeftHalf(int relX, int relY, int size) {
        return relX >= 0 && relX < size / 2 && relY >= 0 && relY < size;
    }

    default boolean mouseOverRightHalf(int relX, int relY, int size) {
        return relX >= size / 2 && relX < size && relY >= 0 && relY < size;
    }

    default boolean mouseOverTopRightQuarter(int relX, int relY, int size) {
        return relX >= size / 2 && relX < size && relY >= 0 && relY < size / 2;
    }

    default boolean mouseOverBottomRightQuarter(int relX, int relY, int size) {
        return relX >= size / 2 && relX < size && relY >= size / 2 && relY < size;
    }

    default boolean mouseOverTopLeftQuarter(int relX, int relY, int size) {
        return relX >= 0 && relX < size / 2 && relY >= 0 && relY < size / 2;
    }

    default boolean mouseOverBottomLeftQuarter(int relX, int relY, int size) {
        return relX >= 0 && relX < size / 2 && relY >= size / 2 && relY < size;
    }
}
