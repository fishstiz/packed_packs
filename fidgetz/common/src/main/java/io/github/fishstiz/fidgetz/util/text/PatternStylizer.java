package io.github.fishstiz.fidgetz.util.text;

import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternStylizer implements TextStylizer {
    private final @Nullable Predicate<String> canStylize;
    private final Matcher matcher;
    private final Style style;

    public PatternStylizer(@Nullable Predicate<String> canStylize, Pattern pattern, Style style) {
        this.canStylize = canStylize;
        this.matcher = pattern.matcher("");
        this.style = style;
    }

    public PatternStylizer(@Nullable Predicate<String> canStylize, Pattern pattern, int color) {
        this(canStylize, pattern, Style.EMPTY.withColor(color));
    }

    @Override
    public boolean canStylize(String input) {
        return this.canStylize == null || this.canStylize.test(input);
    }

    @Override
    public void reset(String input) {
        this.matcher.reset(input);
    }

    @Override
    public boolean find() {
        return this.matcher.find();
    }

    @Override
    public int start() {
        return this.matcher.start();
    }

    @Override
    public int end() {
        return this.matcher.end();
    }

    @Override
    public Style style() {
        return this.matcher.hasMatch() ? this.style : Style.EMPTY;
    }

    @Override
    public TextStylizer copy() {
        return new PatternStylizer(this.canStylize, this.matcher.pattern(), this.style);
    }
}
