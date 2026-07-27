package io.github.fishstiz.fidgetz.util.text;

import net.minecraft.network.chat.Style;

public interface TextStylizer {
    boolean canStylize(String input);

    void reset(String input);

    boolean find();

    int start();

    int end();

    Style style();

    TextStylizer copy();
}
