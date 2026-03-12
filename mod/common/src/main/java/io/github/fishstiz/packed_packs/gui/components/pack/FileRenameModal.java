package io.github.fishstiz.packed_packs.gui.components.pack;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.fishstiz.fidgetz.gui.components.*;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.fidgetz.util.DrawUtil;
import io.github.fishstiz.fidgetz.util.GuiUtil;
import io.github.fishstiz.packed_packs.gui.intents.PackListIntent;
import io.github.fishstiz.packed_packs.gui.model.PackedPacksViewModel;
import io.github.fishstiz.packed_packs.gui.states.ActiveAction;
import io.github.fishstiz.packed_packs.gui.states.PackedPacksState;
import io.github.fishstiz.packed_packs.pack.PackAssetManager;
import io.github.fishstiz.packed_packs.util.PackUtil;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.server.packs.repository.Pack;
import org.apache.commons.io.FilenameUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.regex.Pattern;

import static io.github.fishstiz.packed_packs.util.PackUtil.ZIP_PACK_EXTENSION;
import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.CROSS_SPRITE;
import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.SPACING;

public class FileRenameModal extends Modal<LinearLayout> {
    private static final int MAX_LENGTH = 255;
    private static final int CONTENT_WIDTH = 256;
    private static final int SHADOW_SIZE = 24;
    private static final Pattern ILLEGAL_CHAR_PATTERN = Pattern.compile(".*[<>:\"/\\\\|?*].*");
    private final RenderableRectWidget<Void> sprite;
    private final FidgetzText<Void> title;
    private final ToggleableEditBox<Void> nameEditor;
    private final FidgetzButton<Void> saveButton;
    private final PackedPacksViewModel viewModel;
    private String oldName;

    public <S extends Screen & ToggleableDialogContainer> FileRenameModal(S screen, PackedPacksViewModel viewModel) {
        super(Modal.builder(screen, LinearLayout.vertical().spacing(SPACING)).padding(SPACING).setFocusOnOpen(true));
        this.viewModel = viewModel;

        this.sprite = this.addRenderableOnly(RenderableRectWidget.<Void>builder(PackAssetManager.DEFAULT_ICON)
                .makeSquare()
                .build());
        this.title = this.addRenderableOnly(FidgetzText.<Void>builder()
                .makeSquare()
                .setOffsetY(1)
                .setShadow(true)
                .build());
        FidgetzButton<Void> closeButton = this.addRenderableWidget(FidgetzButton.<Void>builder()
                .makeSquare()
                .setOnPress(this::closeModal)
                .setSprite(CROSS_SPRITE)
                .build());
        this.nameEditor = this.addRenderableWidget(ToggleableEditBox.<Void>builder()
                .setWidth(CONTENT_WIDTH)
                .setEditable(true)
                .addListener(this::handleChange)
                .setMaxLength(MAX_LENGTH)
                .setFilter(this::testInput)
                .build());
        FidgetzButton<Void> cancelButton = this.addRenderableWidget(FidgetzButton.<Void>builder()
                .setOnPress(this::closeModal)
                .setMessage(CommonComponents.GUI_CANCEL)
                .build());
        this.saveButton = this.addRenderableWidget(FidgetzButton.<Void>builder()
                .setOnPress(this::saveName)
                .setMessage(CommonComponents.GUI_DONE)
                .build());

        FlexLayout titleLayout = FlexLayout.horizontal(this::getContentWidth).spacing(SPACING);
        titleLayout.addChild(this.sprite);
        titleLayout.addFlexChild(this.title);
        titleLayout.addChild(closeButton);

        FlexLayout buttonLayout = FlexLayout.horizontal(this::getContentWidth).spacing(SPACING);
        buttonLayout.addFlexChild(cancelButton);
        buttonLayout.addFlexChild(this.saveButton);

        this.root().layout().addChild(titleLayout);
        this.root().layout().addChild(this.nameEditor);
        this.root().layout().addChild(buttonLayout);

        this.viewModel.subscribe(PackedPacksState::renamingPack, this::refresh);
    }

    private int getContentWidth() {
        return CONTENT_WIDTH;
    }

    private void clearReferences() {
        this.oldName = null;
        this.sprite.setRenderableRect(PackAssetManager.DEFAULT_ICON);
        this.title.setMessage(CommonComponents.EMPTY);
        this.nameEditor.setValue("");
    }

    @Override
    public void setOpen(boolean open) {
        var renameContext = this.viewModel.state().renamingPack();
        if (!open && renameContext != null) {
            this.viewModel.dispatch(new PackListIntent.CloseRename(renameContext.target(), renameContext.ctx()));
        }
    }

    private void refresh(ActiveAction.@Nullable RenamingPack renameContext) {
        if (renameContext == null) {
            super.setOpen(false);
            this.setFocused(null);
            this.clearReferences();
            return;
        }

        this.setFocused(this.nameEditor);
        this.sprite.setRenderableRect(renameContext.ctx().sprite());
        this.title.setMessage(renameContext.ctx().pack().getTitle());

        this.oldName = sanitizeNameForEdit(renameContext.ctx().pack());
        this.nameEditor.setValue(this.oldName);
        this.nameEditor.setSuggestion(PackUtil.isZipPack(renameContext.ctx().pack()) ? ZIP_PACK_EXTENSION : null);
        this.saveButton.active = false;

        this.repositionElements();
        super.setOpen(true);
    }

    private boolean testInput(String input) {
        if (input == null || (!input.isEmpty() && input.isBlank())) {
            return false;
        }
        return testIllegalChars(input);
    }

    private boolean canSave(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        var renameContext = this.viewModel.state().renamingPack();
        if (renameContext == null || PackUtil.validatePackPath(renameContext.ctx().pack()) == null) {
            return false;
        }
        String trimmed = input.trim();
        if (Objects.equals(this.oldName, trimmed)) {
            return false;
        }
        return testIllegalChars(input);
    }

    private void handleChange(String name) {
        this.saveButton.active = this.canSave(name);
    }

    private boolean saveName() {
        String newName = this.nameEditor.getValue();
        if (!this.canSave(newName)) return false;

        var renameContext = this.viewModel.state().renamingPack();
        if (renameContext != null) {
            String sanitizedName = sanitizeNameForSave(renameContext.ctx().pack(), newName);
            this.viewModel.dispatch(new PackListIntent.Rename(renameContext.target(), renameContext.ctx(), sanitizedName));
            return true;
        }

        return false;
    }

    private static String sanitizeNameForEdit(Pack pack) {
        String name = pack.getTitle().getString();
        return PackUtil.isZipPack(pack) ? name.replaceFirst(Pattern.quote(ZIP_PACK_EXTENSION) + "$", "") : name;
    }

    private static String sanitizeNameForSave(Pack pack, String newName) {
        newName = FilenameUtils.getName(newName).trim();
        return PackUtil.isZipPack(pack) ? newName + ZIP_PACK_EXTENSION : newName;
    }

    private static boolean testIllegalChars(@NonNull String input) {
        input = input.trim();
        if (!input.equals(FilenameUtils.getName(input))) {
            return false;
        }
        return !ILLEGAL_CHAR_PATTERN.matcher(input).matches();
    }

    @Override
    protected void renderBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, float partialTick) {
        DrawUtil.renderDropShadow(guiGraphics, x, y, width, height, SHADOW_SIZE);
        super.renderBackground(guiGraphics, x, y, width, height, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent keyEvent) {
        boolean keyPressed = super.keyPressed(keyEvent);
        if (!keyPressed && keyEvent.key() == InputConstants.KEY_RETURN && this.saveName()) {
            GuiUtil.playClickSound();
            return this.viewModel.state().renamingPack() != null;
        }
        return keyPressed;
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(@NonNull FocusNavigationEvent event) {
        if (this.viewModel.state().renamingPack() == null) {
            return null;
        }
        if (event instanceof FocusNavigationEvent.InitialFocus) {
            return ComponentPath.path(this.nameEditor, this);
        }
        return super.nextFocusPath(event);
    }
}
