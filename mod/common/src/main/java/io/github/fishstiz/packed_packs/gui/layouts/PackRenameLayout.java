package io.github.fishstiz.packed_packs.gui.layouts;

import io.github.fishstiz.fidgetz.v0.gui.components.*;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZFlexLayout;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZLayout;
import io.github.fishstiz.fidgetz.v0.gui.renderables.Renderables;
import io.github.fishstiz.fidgetz.v0.gui.state.FZMutableRef;
import io.github.fishstiz.fidgetz.v0.utils.FunctionUtils;
import io.github.fishstiz.packed_packs.gui.model.PackListKey;
import io.github.fishstiz.packed_packs.gui.states.ActiveAction;
import io.github.fishstiz.packed_packs.gui.Store;
import io.github.fishstiz.packed_packs.gui.actions.intents.PackListIntent;
import io.github.fishstiz.packed_packs.gui.services.PackResourcesService;
import io.github.fishstiz.packed_packs.pack.PackEntry;
import io.github.fishstiz.packed_packs.util.PackUtil;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
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

    public static PackRenameLayout create(Store store) {
        ActiveAction.RenamingPack renamingPack = store.value().renamingPack();
        if (renamingPack == null) {
            return new PackRenameLayout(error(Component.literal("renamingPack is null")));
        }

        final PackResourcesService resources = store.getPackResourcesService();
        final PackEntry pack = renamingPack.pack();
        final PackListKey target = renamingPack.target();
        final String previousName = sanitizeNameForEdit(pack);
        final FZMutableRef<String> nameRef = new FZMutableRef<>(previousName);
        final String suggestion = PackUtil.isZipPath(pack.path()) ? ZIP_PACK_EXTENSION : null;
        final Runnable closeHandler = () -> {
            ActiveAction.RenamingPack renaming = store.value().renamingPack();
            if (renaming == null || !renaming.loading()) {
                store.dispatch(new PackListIntent.CloseRenameModal(target, pack));
            }
        };

        return new PackRenameLayout(closeHandler, FZFlexLayout.vertical().spacing(SPACING).maxWidth(WIDTH).also(root -> {
            root.child(FZFlexLayout.horizontal(), root.flexChildHorizontalSettings()).also(header -> {
                header.maxWidth(WIDTH).spacing(SPACING).defaultChildSettings().alignVerticallyMiddle();

                header.child(FZIcon.builder(Renderables.texture(resources.getIcon(pack), 32, 32))
                        .size(16, 16)
                        .build());
                header.child(FZText.builder(pack.title())
                        .build(), header.flexChildHorizontalSettings());
                header.child(FZIconButton.builder()
                        .square()
                        .icon(new WidgetElements(CROSS_SPRITE, 16, 16))
                        .onPress(closeHandler)
                        .build());
            });

            FZButton closeButton = FZButton.builder().message(CommonComponents.GUI_CANCEL)
                    .onPress(closeHandler)
                    .focusOnInteraction(false)
                    .build();

            FZButton saveButton = FZButton.bind("SaveButton", nameRef.map(value -> FZButton.builder()
                    .message(CommonComponents.GUI_DONE)
                    .active(canSave(previousName, value))
                    .focusOnInteraction(false)
                    .onPress(() -> {
                        if (canSave(previousName, value)) {
                            store.dispatch(new PackListIntent.Rename(target, pack, sanitizeNameForSave(pack, value)));
                        }
                    })
                    .toProps()));

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
                            store.dispatch(new PackListIntent.Rename(target, pack, sanitizeNameForSave(pack, newValue)));
                            ActiveAction.RenamingPack result = store.value().renamingPack();
                            if (result != null && result.loading()) {
                                e.target().active = false;
                                closeButton.active = false;
                                saveButton.active = false;
                            }
                            e.confirm();
                        }
                    })
                    .toProps())));

            root.child(FZFlexLayout.horizontal(), root.flexChildHorizontalSettings()).also(footer -> {
                footer.maxWidth(WIDTH).spacing(SPACING).defaultChildSettings().flexMain();
                footer.child(closeButton);
                footer.child(saveButton);
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

    private static String sanitizeNameForEdit(PackEntry pack) {
        String name = pack.title().getString();
        return PackUtil.isZipPath(pack.path()) ? name.replaceFirst(Pattern.quote(ZIP_PACK_EXTENSION) + "$", "") : name;
    }

    private static String sanitizeNameForSave(PackEntry pack, String newName) {
        newName = FilenameUtils.getName(newName).trim();
        return PackUtil.isZipPath(pack.path()) ? newName + ZIP_PACK_EXTENSION : newName;
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
