package io.github.fishstiz.packed_packs.gui.text;

import io.github.fishstiz.fidgetz.v0.gui.text.TextStyleMatcher;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class GroupCloseStyleMatcher implements TextStyleMatcher {
    private final @Nullable Predicate<String> styleable;
    private final char open;
    private final char close;
    private final Style style;
    private String text = "";
    private int depth = 0;
    private int pos = 0;
    private int start = -1;
    private int end = -1;

    public GroupCloseStyleMatcher(char open, char close, Style style, @Nullable Predicate<String> styleable) {
        this.styleable = styleable;
        this.open = open;
        this.close = close;
        this.style = style;
    }

    @Override
    public boolean styleable(String input) {
        return styleable == null || styleable.test(input);
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

            if (c == open) this.depth++;
            else if (c == close && this.depth > 0) {
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
        return start >= 0 ? style : Style.EMPTY;
    }

    @Override
    public GroupCloseStyleMatcher copy() {
        return new GroupCloseStyleMatcher(open, close, style, styleable);
    }
}
