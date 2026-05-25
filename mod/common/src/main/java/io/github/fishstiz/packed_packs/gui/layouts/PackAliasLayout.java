package io.github.fishstiz.packed_packs.gui.layouts;

import io.github.fishstiz.fidgetz.v0.gui.components.*;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZFlexLayout;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZLayout;
import io.github.fishstiz.fidgetz.v0.gui.renderables.Renderables;
import io.github.fishstiz.fidgetz.v0.gui.state.FZMutableRef;
import io.github.fishstiz.fidgetz.v0.gui.text.TextStyleMatcher;
import io.github.fishstiz.fidgetz.v0.gui.text.TextStyleRegexMatcher;
import io.github.fishstiz.fidgetz.v0.utils.CollectionUtils;
import io.github.fishstiz.fidgetz.v0.utils.FunctionUtils;
import io.github.fishstiz.packed_packs.config.DevConfig;
import io.github.fishstiz.packed_packs.gui.intents.PackListIntent;
import io.github.fishstiz.packed_packs.gui.model.PackListKey;
import io.github.fishstiz.packed_packs.gui.model.PackedPacksStore;
import io.github.fishstiz.packed_packs.gui.states.ActiveAction;
import io.github.fishstiz.packed_packs.api.context.PackContext;
import io.github.fishstiz.packed_packs.util.Colors;
import io.github.fishstiz.packed_packs.gui.text.GroupCloseStyleMatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.github.fishstiz.packed_packs.util.GuiUtils.CROSS_SPRITE;
import static io.github.fishstiz.packed_packs.util.GuiUtils.SPACING;

public class PackAliasLayout extends WrappedLayout {
    private static final int MAX_LENGTH = 255;
    private static final int WIDTH = 296;
    private static final int LIST_HEIGHT = 128;
    private final Runnable closeHandler;

    private PackAliasLayout(FZLayout layout) {
        super(layout);
        this.closeHandler = FunctionUtils.nop();
    }

    private PackAliasLayout(Runnable closeHandler, FZLayout layout) {
        super(layout);
        this.closeHandler = closeHandler;
    }

    public void onClose() {
        closeHandler.run();
    }

    public static PackAliasLayout create(PackedPacksStore store, List<TextStyleMatcher> styleMatchers) {
        ActiveAction.EditingAliases editingAliases = store.value().editingAliases();
        if (editingAliases == null) {
            return new PackAliasLayout(error(Component.literal("editingAliases is null")));
        }

        final PackListKey key = editingAliases.target();
        final PackContext ctx = editingAliases.ctx();
        final FZMutableRef<List<MutableObject<String>>> aliasesRef = new FZMutableRef<>(editingAliases
                .aliases()
                .stream()
                .map(MutableObject::new)
                .collect(Collectors.toCollection(ArrayList::new)));
        final Runnable closeHandler = () -> store.dispatch(new PackListIntent.CloseAliases(key, ctx, aliasesRef
                .value()
                .stream()
                .map(MutableObject::getValue)
                .filter(s -> !s.isEmpty())
                .toList()));

        return new PackAliasLayout(closeHandler, FZFlexLayout.vertical().also(root -> {
            root.maxWidth(WIDTH).spacing(SPACING);

            root.child(FZFlexLayout.horizontal(), root.flexChildHorizontalSettings()).also(header -> {
                header.maxWidth(WIDTH).spacing(SPACING).defaultChildSettings().alignVerticallyMiddle();

                header.child(FZIcon.builder(Renderables.texture(ctx.icon(), 32, 32)).build());
                header.child(FZText.builder(ctx.pack().getTitle())
                        .build(), header.flexChildHorizontalSettings());
                header.child(FZButton.builder()
                        .square()
                        .message(Component.literal("+"))
                        .onPress(() -> aliasesRef.set(prev -> CollectionUtils.addLast(prev, new MutableObject<>(""))))
                        .build());
                header.child(FZIconButton.builder()
                        .square()
                        .icon(new WidgetElements(CROSS_SPRITE, 16, 16))
                        .onPress(closeHandler)
                        .build());
            });
            root.child(FZLayoutList.bind("AliasList", aliasesRef.map(aliases -> FZLayoutList.builder()
                    .size(WIDTH, LIST_HEIGHT)
                    .maxContentWidth(0)
                    .onRefresh(refreshEvent -> {
                        FZFlexLayout row = refreshEvent.layout();
                        aliases.forEach(alias -> {
                            FZFlexLayout entry = row.child(FZFlexLayout.horizontal().maxWidth(WIDTH), row.flexChildHorizontalSettings());
                            entry.child(FZTextField.builder()
                                    .text(alias.getValue())
                                    .onChange(e -> alias.setValue(e.value()))
                                    .allowSectionSign()
                                    .maxLength(MAX_LENGTH)
                                    .styleMatchers(styleMatchers)
                                    .build(), entry.flexChildHorizontalSettings());
                            entry.child(FZButton.builder()
                                    .square()
                                    .message(Component.literal("-"))
                                    .onPress(() -> aliasesRef.set(prev -> CollectionUtils.remove(prev, alias)))
                                    .build());
                        });
                    })
                    .toProps())));
        }));
    }

    private static TextStyleMatcher createRegexMatcher(Pattern pattern, int color) {
        return new TextStyleRegexMatcher(pattern, Style.EMPTY.withColor(color), DevConfig.Packs::isRegexPrefixed);
    }

    private static TextStyleMatcher createGroupMatcher(char open, char close, int color) {
        return new GroupCloseStyleMatcher(open, close, Style.EMPTY.withColor(color), DevConfig.Packs::isRegexPrefixed);
    }

    public static List<TextStyleMatcher> styleMatchers() {
        return List.of(
                // unescaped forward slash
                createRegexMatcher(Pattern.compile("(?<!\\\\)/"), Colors.RED_700),
                // unclosed parenthesis
                createRegexMatcher(Pattern.compile("(?<!\\\\)\\((?=[^)]*$)"), Colors.RED_700),
                // unclosed bracket
                createRegexMatcher(Pattern.compile("(?<!\\\\)\\[(?![^]\\[]*])"), Colors.RED_700),
                // dangling backslash
                createRegexMatcher(Pattern.compile("(?<=(?<!\\\\)(\\\\\\\\){0,128})\\\\$"), Colors.RED_700),
                // any char
                createRegexMatcher(
                        Pattern.compile("\\\\[wWdDsS]|(?<!(?<!\\\\)(\\\\\\\\){0,128}\\[[^]]{0,255})(?<=(?<!\\\\)(\\\\\\\\){0,128})(?<=(?<!\\\\)(\\\\\\\\){0,128})\\."),
                        Colors.ORANGE_500
                ),
                // escaped char
                createRegexMatcher(
                        Pattern.compile("(\\\\u[0-9a-fA-F]{4})|(\\\\([0-3][0-7]{2}|[0-7]{1,2}))|(\\\\[^wWdDsS])"),
                        Colors.MAGENTA_500
                ),
                // boundary
                createRegexMatcher(
                        Pattern.compile("(?<!(?<!\\\\)(\\\\\\\\){0,128}\\[[^]]{0,255})(?<=(?<!\\\\)(\\\\\\\\){0,128})(?<=(?<!\\\\)(\\\\\\\\){0,128})[$^]|(\\\\[bB])"),
                        Colors.BROWN_500
                ),
                // quantifier
                createRegexMatcher(
                        Pattern.compile("(?<!(?<!\\\\)(\\\\\\\\){0,128}\\[[^]]{0,255})(?<=(?<!\\\\)(\\\\\\\\){0,128})(?<=(?<!\\\\)(\\\\\\\\){0,128})(\\{\\d+,?\\d*}|[+*?])"),
                        Colors.BLUE_500
                ),
                // open char set
                createRegexMatcher(
                        Pattern.compile("(?<=(?<!\\\\)(\\\\\\\\){0,128})\\[\\^?(?=[^]]*(?<=(?<!\\\\)(\\\\\\\\){0,128})])"),
                        Colors.YELLOW_500
                ),
                // closing char set
                createGroupMatcher('[', ']', Colors.YELLOW_500),
                // open capture group
                createRegexMatcher(
                        Pattern.compile("(?<=(?<!\\\\)(\\\\\\\\){0,128})\\((\\?(<\\w+>|:|!|=|<!|<=))?(?=.*(?<=(?<!\\\\)(\\\\\\\\){0,128})\\))"),
                        Colors.GREEN_500
                ),
                // closing capture group
                createGroupMatcher('(', ')', Colors.GREEN_500),
                // alternation
                createRegexMatcher(
                        Pattern.compile("(?<!(?<!\\\\)(\\\\\\\\){0,128}\\[[^]]{0,255})(?<=(?<!\\\\)(\\\\\\\\){0,128})\\|"),
                        Colors.GREEN_500
                ),
                createRegexMatcher(DevConfig.Packs.getRegexPrefixPattern(), Colors.GRAY_500)
        );
    }
}
