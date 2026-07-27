package io.github.fishstiz.fidgetz.util.text;

import io.github.fishstiz.fidgetz.util.lang.CollectionsUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.Util;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public class AsyncStylizerFormatter {
    private final TextStylizer[] textStylizers;
    private final Supplier<String> textSupplier;
    private final Executor executor;
    private final StylizedSequence readSequence = new StylizedSequence();
    private final StylizedSequence writeSequence = new StylizedSequence();
    private volatile StylizedSequence currentSequence = readSequence;
    private volatile String lastText = "";

    public AsyncStylizerFormatter(List<TextStylizer> textStylizers, Supplier<String> textSupplier, Executor executor) {
        this.textStylizers = CollectionsUtil.mapToArray(textStylizers, TextStylizer::copy, TextStylizer[]::new);
        this.textSupplier = textSupplier;
        this.executor = executor;
    }

    public AsyncStylizerFormatter(List<TextStylizer> textStylizers, Supplier<String> textSupplier) {
        this(textStylizers, textSupplier, Util.backgroundExecutor());
    }

    public @Nullable FormattedCharSequence format(String displayText, int displayPos) {
        String currentText = this.textSupplier.get();
        if (currentText.isBlank()) return null;

        StylizedSequence current = this.currentSequence;
        current.text = currentText;
        current.displayPos = displayPos;
        current.displayEnd = displayPos + displayText.length();

        if (!currentText.equals(this.lastText)) {
            this.lastText = currentText;
            this.executor.execute(this::computeStyles);
        }

        return current;
    }

    private void computeStyles() {
        StylizedSequence write = (this.currentSequence == this.readSequence) ? this.writeSequence : this.readSequence;
        String text = this.lastText;

        write.text = text;
        write.charStyles.size(text.length());
        Arrays.fill(write.charStyles.elements(), 0, write.charStyles.size(), null);

        for (TextStylizer stylizer : this.textStylizers) {
            if (!stylizer.canStylize(text)) continue;
            stylizer.reset(text);
            while (stylizer.find()) {
                for (int i = stylizer.start(); i < stylizer.end(); i++) {
                    write.charStyles.set(i, stylizer.style());
                }
            }
        }

        this.currentSequence = write;
    }

    private static class StylizedSequence implements FormattedCharSequence {
        final ObjectArrayList<Style> charStyles = new ObjectArrayList<>();
        volatile String text = "";
        int displayPos;
        int displayEnd;

        @Override
        public boolean accept(FormattedCharSink sink) {
            String currentText = this.text;
            int start = Math.max(0, displayPos);
            int end = Math.min(Math.min(currentText.length(), displayEnd), Math.max(this.charStyles.size(), currentText.length()));

            for (int i = start; i < end; i++) {
                Style style = i < this.charStyles.size() ? this.charStyles.get(i) : null;
                if (!sink.accept(i - displayPos, Objects.requireNonNullElse(style, Style.EMPTY), currentText.charAt(i))) {
                    return false;
                }
            }

            return true;
        }
    }
}
