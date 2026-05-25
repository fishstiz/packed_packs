package io.github.fishstiz.packed_packs.gui.layouts;

import io.github.fishstiz.fidgetz.v0.gui.components.*;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZFlexLayout;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZLayout;
import io.github.fishstiz.fidgetz.v0.gui.renderables.Renderables;
import io.github.fishstiz.fidgetz.v0.gui.state.FZMutableRef;
import io.github.fishstiz.fidgetz.v0.utils.FunctionUtils;
import io.github.fishstiz.packed_packs.gui.intents.PackListIntent;
import io.github.fishstiz.packed_packs.gui.model.PackListKey;
import io.github.fishstiz.packed_packs.gui.model.PackedPacksStore;
import io.github.fishstiz.packed_packs.gui.states.ActiveAction;
import io.github.fishstiz.packed_packs.api.context.PackContext;
import io.github.fishstiz.packed_packs.util.PackUtil;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import org.apache.commons.io.FilenameUtils;

import java.util.Objects;
import java.util.regex.Pattern;

import static io.github.fishstiz.packed_packs.util.PackUtil.ZIP_PACK_EXTENSION;
import static io.github.fishstiz.packed_packs.util.GuiUtils.CROSS_SPRITE;
import static io.github.fishstiz.packed_packs.util.GuiUtils.SPACING;

public class PackRenameLayout extends WrappedLayout {
    private static final int MAX_LENGTH = 255;
    private static final int WIDTH = 256;
    private static final Pattern ILLEGAL_CHAR_PATTERN = Pattern.compile(".*[<>:\"/\\\\|?*].*");
    private final Runnable closeHandler;

    private PackRenameLayout(FZLayout layout) {
        super(layout);
        this.closeHandler = FunctionUtils.nop();
    }

    private PackRenameLayout(Runnable closeHandler, FZLayout layout) {
        super(layout);
        this.closeHandler = closeHandler;
    }

    public void onClose() {
        closeHandler.run();
    }

    public static PackRenameLayout create(PackedPacksStore store) {
        ActiveAction.RenamingPack renamingPack = store.value().renamingPack();
        if (renamingPack == null) {
            return new PackRenameLayout(error(Component.literal("renamingPack is null")));
        }

        final PackListKey target = renamingPack.target();
        final PackContext ctx = renamingPack.ctx();
        final String previousName = sanitizeNameForEdit(ctx.pack());
        final FZMutableRef<String> nameRef = new FZMutableRef<>(previousName);
        final String suggestion = PackUtil.isZipPack(ctx.pack()) ? ZIP_PACK_EXTENSION : null;
        final Runnable closeHandler = () -> store.dispatch(new PackListIntent.CloseRename(target, ctx));

        return new PackRenameLayout(closeHandler, FZFlexLayout.vertical().spacing(SPACING).maxWidth(WIDTH).also(root -> {
            root.child(FZFlexLayout.horizontal(), root.flexChildHorizontalSettings()).also(header -> {
                header.maxWidth(WIDTH).spacing(SPACING).defaultChildSettings().alignVerticallyMiddle();

                header.child(FZIcon.builder(Renderables.texture(ctx.icon(), 32, 32))
                        .size(16, 16)
                        .build());
                header.child(FZText.builder(ctx.pack().getTitle())
                        .build(), header.flexChildHorizontalSettings());
                header.child(FZIconButton.builder()
                        .square()
                        .icon(new WidgetElements(CROSS_SPRITE, 16, 16))
                        .onPress(closeHandler)
                        .build());
            });

            root.child(FZTextField.bind("NameField", nameRef.map(value -> FZTextField.builder()
                    .width(WIDTH)
                    .text(value)
                    .suggestion(suggestion)
                    .filter(PackRenameLayout::testInput)
                    .allowSectionSign()
                    .maxLength(MAX_LENGTH)
                    .tabOrderGroup(-1)
                    .onChange(e -> nameRef.set(e.value()))
                    .onConfirm(e -> {
                        String newValue = e.target().getValue();
                        if (canSave(previousName, newValue)) {
                            store.dispatch(new PackListIntent.Rename(target, ctx, sanitizeNameForSave(ctx.pack(), newValue)));
                            return true;
                        }
                        return false;
                    })
                    .toProps())));

            root.child(FZFlexLayout.horizontal(), root.flexChildHorizontalSettings()).also(footer -> {
                footer.maxWidth(WIDTH).spacing(SPACING).defaultChildSettings().flexMain();

                footer.child(FZButton.builder().message(CommonComponents.GUI_CANCEL)
                        .onPress(closeHandler)
                        .build());
                footer.child(FZButton.bind("SaveButton", nameRef.map(value -> FZButton.builder()
                        .message(CommonComponents.GUI_DONE)
                        .active(canSave(previousName, value))
                        .onPress(() -> {
                            if (canSave(previousName, value)) {
                                store.dispatch(new PackListIntent.Rename(target, ctx, sanitizeNameForSave(ctx.pack(), value)));
                            }
                        })
                        .toProps())));
            });

            root.arrangeElements();
        }));
    }

    private static boolean canSave(String previousName, String newName) {
        if (newName.isBlank()) {
            return false;
        }
        String trimmed = newName.trim();
        if (Objects.equals(previousName, trimmed)) {
            return false;
        }
        return testIllegalChars(newName);
    }

    private static String sanitizeNameForEdit(Pack pack) {
        String name = pack.getTitle().getString();
        return PackUtil.isZipPack(pack) ? name.replaceFirst(Pattern.quote(ZIP_PACK_EXTENSION) + "$", "") : name;
    }

    private static String sanitizeNameForSave(Pack pack, String newName) {
        newName = FilenameUtils.getName(newName).trim();
        return PackUtil.isZipPack(pack) ? newName + ZIP_PACK_EXTENSION : newName;
    }

    private static boolean testInput(String input) {
        if (!input.isEmpty() && input.isBlank()) {
            return false;
        }
        return testIllegalChars(input);
    }

    private static boolean testIllegalChars(String input) {
        input = input.trim();
        if (!input.equals(FilenameUtils.getName(input))) {
            return false;
        }
        return !ILLEGAL_CHAR_PATTERN.matcher(input).matches();
    }
}
