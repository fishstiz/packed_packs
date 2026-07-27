package io.github.fishstiz.packed_packs.util.text;

import io.github.fishstiz.fidgetz.util.text.TextStylizer;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class GroupCloseStylizer implements TextStylizer {
    private final @Nullable Predicate<String> canStylize;
    private final char open;
    private final char close;
    private final Style style;
    private String text = "";
    private int depth = 0;
    private int pos = 0;
    private int start = -1;
    private int end = -1;

    public GroupCloseStylizer(@Nullable Predicate<String> canStylize, char open, char close, Style style) {
        this.canStylize = canStylize;
        this.open = open;
        this.close = close;
        this.style = style;
    }

    public GroupCloseStylizer(@Nullable Predicate<String> canStylize, char open, char close, int color) {
        this(canStylize, open, close, Style.EMPTY.withColor(color));
    }

    @Override
    public boolean canStylize(String input) {
        return this.canStylize == null || this.canStylize.test(input);
    }

    @Override
    public void reset(String input) {
        this.text = input;
        this.depth = 0;
        this.pos = 0;
        this.start = -1;
        this.end = -1;
    }

    @Override
    public boolean find() {
        while (this.pos < this.text.length()) {
            char c = this.text.charAt(pos);
            if (c == '\\' && this.pos + 1 < this.text.length()) {
                this.pos += 2;
                continue;
            }

            if (c == this.open) this.depth++;
            else if (c == this.close && this.depth > 0) {
                this.start = this.pos;
                this.end = this.pos + 1;
                this.pos++;
                this.depth--;
                return true;
            }

            this.pos++;
        }
        return false;
    }

    @Override
    public int start() {
        return this.start;
    }

    @Override
    public int end() {
        return this.end;
    }

    @Override
    public Style style() {
        return start >= 0 ? this.style : Style.EMPTY;
    }

    @Override
    public TextStylizer copy() {
        return new GroupCloseStylizer(this.canStylize, this.open, this.close, this.style);
    }
}
