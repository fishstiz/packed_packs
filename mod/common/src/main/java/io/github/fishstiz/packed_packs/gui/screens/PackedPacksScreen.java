package io.github.fishstiz.packed_packs.gui.screens;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import io.github.fishstiz.fidgetz.gui.components.*;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenu;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuContainer;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.util.lang.CollectionsUtil;
import io.github.fishstiz.fidgetz.util.lang.ObjectsUtil;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.api.events.ContextMenuEvent;
import io.github.fishstiz.packed_packs.api.events.InitializeLayoutEvent;
import io.github.fishstiz.packed_packs.api.events.ScreenClosingEvent;
import io.github.fishstiz.packed_packs.config.*;
import io.github.fishstiz.packed_packs.gui.FocusTarget;
import io.github.fishstiz.packed_packs.gui.UiEffect;
import io.github.fishstiz.packed_packs.gui.components.PreferenceToggle;
import io.github.fishstiz.packed_packs.gui.components.profile.ProfilesSidebar;
import io.github.fishstiz.packed_packs.gui.states.DragActionRenderer;
import io.github.fishstiz.packed_packs.gui.components.contextmenu.*;
import io.github.fishstiz.packed_packs.gui.components.pack.*;
import io.github.fishstiz.packed_packs.gui.intents.PackListIntent;
import io.github.fishstiz.packed_packs.gui.layouts.OptionsLayout;
import io.github.fishstiz.packed_packs.gui.layouts.PackLayout;
import io.github.fishstiz.packed_packs.gui.metadata.PackSelectionScreenArgs;
import io.github.fishstiz.packed_packs.gui.model.*;
import io.github.fishstiz.packed_packs.gui.states.ActiveAction;
import io.github.fishstiz.packed_packs.impl.PackedPacksApiImpl;
import io.github.fishstiz.packed_packs.impl.context.ScreenContextImpl;
import io.github.fishstiz.packed_packs.impl.events.ContextMenuEventImpl;
import io.github.fishstiz.packed_packs.pack.*;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionModelAccessor;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionScreenAccessor;
import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.NoticeWithLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import static com.mojang.blaze3d.platform.InputConstants.KEY_BACKSPACE;
import static com.mojang.blaze3d.platform.InputConstants.KEY_SPACE;
import static io.github.fishstiz.packed_packs.util.InputUtil.*;
import static io.github.fishstiz.packed_packs.util.PackUtil.joinPackNames;
import static io.github.fishstiz.packed_packs.util.PackUtil.validatePaths;
import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.*;

public class PackedPacksScreen extends Screen implements HoverStateHandler, ToggleableDialogContainer, ContextMenuContainer {
    private static final Component OPEN_FOLDER_TEXT = Component.translatable("pack.openFolder");
    private final Screen previous;
    private final PackSelectionScreenArgs original;
    private final PackedPacksViewModel viewModel;
    private final ScreenContext context;
    private final LayoutWrapper<FlexLayout> layout;
    private final ProfilesSidebar profilesSidebar;
    private final PackLayout availableLayout;
    private final PackLayout enabledLayout;
    private final ContextMenu contextMenu;
    private final Modal<OptionsLayout> optionsModal;
    private final List<ToggleableDialog<?>> dialogs;
    private final DragActionRenderer dragActionRenderer;
    private @Nullable GuiEventListener hoveredElement;
    private boolean refreshOnInit = true;
    private boolean initialized = false;

    static {
        // force load API
        //noinspection ResultOfMethodCallIgnored
        Util.backgroundExecutor().execute(PackedPacksApiImpl::getInstance);
    }

    private PackedPacksScreen(Screen previous, PackSelectionScreenArgs original, InitMode initMode) {
        super(ResourceUtil.getModName());

        this.previous = previous;
        this.original = original;
        this.viewModel = new PackedPacksViewModel(this.minecraft, original, initMode);
        this.context = new ScreenContextImpl(previous, this, this.viewModel, original, Config.get().isDevMode());
        this.viewModel.startWatcher(this.context);

        Components components = Components.create(this, this.context, this.viewModel);
        this.profilesSidebar = components.profilesSidebar();
        this.availableLayout = components.availableLayout();
        this.enabledLayout = components.enabledLayout();
        this.contextMenu = components.contextMenu();
        this.optionsModal = components.optionsModal();
        this.dialogs = components.dialogs();

        this.dragActionRenderer = new DragActionRenderer(this.minecraft.font);
        this.layout = new LayoutWrapper<>(FlexLayout.vertical(this::getMaxHeight).spacing(SPACING));
        this.layout.setPadding(SPACING);

        this.viewModel.addEffectListener(this::onUiEffect);
    }

    public PackedPacksScreen(Screen previous, PackSelectionScreenArgs original) {
        this(previous, original, new InitMode.Default());
    }

    public PackedPacksScreen(Screen previous, PackSelectionScreenArgs original, Profile profile) {
        this(previous, original, new InitMode.WithProfile(profile));
    }

    public PackedPacksScreen(Screen previous, PackSelectionScreenArgs original, PackGroup packs) {
        this(previous, original, new InitMode.WithPacks(packs));
    }

    @Override
    public void added() {
        if (this.initialized) {
            this.viewModel.onMounted();
            this.viewModel.refreshRepository();
            this.viewModel.startWatcher(this.context);
        }
    }

    @Override
    public void removed() {
        this.viewModel.stopWatcher();
        this.viewModel.onUnmounted();
    }

    @Override
    protected void init() {
        if (this.initialized) return;

        InitializeLayoutEvent event = PackedPacksApiImpl.getInstance().eventBus().post(new InitializeLayoutEvent(this.context));
        this.layout.layout().addChild(this.createHeader(event));
        this.layout.layout().addFlexChild(this.createContents());
        this.layout.layout().addChild(this.createFooter(event));

        this.dialogs.forEach(this::addWidget);
        this.layout.visitWidgets(this::addRenderableWidget);
        CollectionsUtil.forEachReverse(this.dialogs, this::addRenderableOnly);
        this.repositionElements();

        this.viewModel.onMounted();
        if (this.refreshOnInit) this.viewModel.refreshRepository();

        this.initialized = true;
    }

    private void addExtensions(FlexLayout layout, InitializeLayoutEvent.Pos pos, InitializeLayoutEvent extensions) {
        extensions.getPendingWidgets(pos).forEach(layout::addChild);
    }

    private FlexLayout createHeader(InitializeLayoutEvent extensions) {
        FlexLayout header = FlexLayout.horizontal(this::getMaxWidth).spacing(SPACING);

        header.addChild(FidgetzButton.builder()
                .makeSquare()
                .setMessage(ProfilesViewModel.TITLE_TEXT)
                .setTooltip(Tooltip.create(ProfilesViewModel.TITLE_TEXT))
                .setSprite(HAMBURGER_SPRITE)
                .setOnPress(this.profilesSidebar::toggle)
                .build());

        header.addChild(PreferenceToggle.wrap(Preferences.ACTION_BAR_WIDGET, FidgetzButton.<Void>builder()
                .makeSquare()
                .setTooltip(Tooltip.create(ResourceUtil.getText("toggle_actionbar.info")))
                .setSprite(Sprite.of16(ResourceUtil.getIcon("filter")))
                .setOnPress(this::toggleActionBar)
                .build()));

        header.addFlexChild(this.profilesSidebar.createProfileHeader());

        this.addExtensions(header, InitializeLayoutEvent.Pos.AFTER_TITLE, extensions);

        header.addChild(PreferenceToggle.wrap(Preferences.OPTIONS_WIDGET, FidgetzButton.<Void>builder()
                .makeSquare()
                .setMessage(OPTIONS_TEXT)
                .setTooltip(Tooltip.create(OPTIONS_TEXT.copy().append(CommonComponents.ELLIPSIS)))
                .setSprite(Sprite.of16(ResourceUtil.getIcon("gear")))
                .setOnPress(this.optionsModal::toggle)
                .build()));

        header.addChild(PreferenceToggle.wrap(Preferences.ORIGINAL_SCREEN_WIDGET, FidgetzButton.<Void>builder()
                .makeSquare()
                .setTooltip(Tooltip.create(ResourceUtil.getText("original_screen.info").append(CommonComponents.ELLIPSIS)))
                .setSprite(Sprite.of16(ResourceUtil.getIcon("exit")))
                .setOnPress(() -> {
                    if (this.previous instanceof PackSelectionScreen) {
                        this.onClose();
                    } else {
                        this.minecraft.setScreen(this.original.createScreen(this.previous));
                    }
                })
                .build()));

        return header;
    }

    private FlexLayout createContents() {
        FlexLayout contents = FlexLayout.horizontal(this::getMaxWidth).spacing(SPACING);
        contents.addFlexChild(this.availableLayout, true);
        contents.addFlexChild(this.enabledLayout, true);
        return contents;
    }

    private FlexLayout createFooter(InitializeLayoutEvent extensions) {
        FlexLayout footer = FlexLayout.horizontal(this::getMaxWidth).spacing(SPACING);
        FlexLayout firstColumn = FlexLayout.horizontal().spacing(SPACING);
        FlexLayout secondColumn = FlexLayout.horizontal().spacing(SPACING);

        this.addExtensions(firstColumn, InitializeLayoutEvent.Pos.BEFORE_FOOTER, extensions);

        firstColumn.addFlexChild(FidgetzButton.builder()
                .setMessage(OPEN_FOLDER_TEXT)
                .setTooltip(Tooltip.create(Component.translatable("pack.folderInfo")))
                .setOnPress(this.viewModel::openBaseDir)
                .build());

        this.addExtensions(firstColumn, InitializeLayoutEvent.Pos.AFTER_LEFT_FOOTER, extensions);

        if (this.context.isClientResources()) {
            secondColumn.addFlexChild(FidgetzButton.builder().setMessage(ResourceUtil.getText("apply")).setOnPress(this.viewModel::commit).build());
        }

        this.addExtensions(secondColumn, InitializeLayoutEvent.Pos.BEFORE_RIGHT_FOOTER, extensions);

        secondColumn.addFlexChild(FidgetzButton.builder().setMessage(CommonComponents.GUI_DONE).setOnPress(this::onClose).build());

        this.addExtensions(secondColumn, InitializeLayoutEvent.Pos.AFTER_FOOTER, extensions);

        footer.addFlexChild(firstColumn);
        footer.addFlexChild(secondColumn);

        return footer;
    }

    public int getMaxHeight() {
        return this.height - SPACING * 2;
    }

    public int getMaxWidth() {
        return this.width - SPACING * 2;
    }

    private void toggleActionBar() {
        Config.get().setShowActionBar(!Config.get().isShowActionBar());
        this.repositionLists();
    }

    private void repositionLists() {
        this.availableLayout.setHeaderVisibility(Config.get().isShowActionBar());
        this.enabledLayout.setHeaderVisibility(Config.get().isShowActionBar());
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        this.layout.setPosition(0, 0);
        this.dialogs.forEach(ToggleableDialog::repositionElements);
        this.contextMenu.setOpen(false);
        this.repositionLists();
    }

    @Override
    protected void rebuildWidgets() {
        PackedPacksScreen screen;
        Profile profile = this.viewModel.getSelectedProfile();
        if (profile != null) {
            screen = new PackedPacksScreen(this.previous, this.original, profile);
        } else {
            PackGroup packs = new PackGroup(this.viewModel.getEnabledPacks(), this.viewModel.getAvailablePacks());
            screen = new PackedPacksScreen(this.previous, this.original, packs);
        }
        screen.refreshOnInit = false;
        this.minecraft.setScreen(screen);
    }

    @Override
    public void onFilesDrop(@NonNull List<Path> files) {
        this.minecraft.setScreen(new ConfirmScreen(
                confirmed -> {
                    if (!confirmed) {
                        this.minecraft.setScreen(this);
                        return;
                    }
                    PackUtil.PathValidationResults results = validatePaths(files);
                    if (!results.symlinkWarnings().isEmpty()) {
                        this.minecraft.setScreen(NoticeWithLinkScreen.createPackSymlinkWarningScreen(() -> this.minecraft.setScreen(this)));
                        return;
                    }
                    if (!results.valid().isEmpty()) {
                        PackSelectionScreen.copyPacks(this.minecraft, results.valid(), this.viewModel.getBaseDir());
                        this.viewModel.refreshRepository();
                    }
                    if (!results.rejected().isEmpty()) {
                        String rejectedNames = joinPackNames(results.rejected());
                        this.minecraft.setScreen(new AlertScreen(
                                () -> this.minecraft.setScreen(this),
                                Component.translatable("pack.dropRejected.title"),
                                Component.translatable("pack.dropRejected.message", rejectedNames)
                        ));
                        return;
                    }
                    this.minecraft.setScreen(this);
                },
                Component.translatable("pack.dropConfirm"),
                Component.literal(joinPackNames(files))
        ));
    }

    @Override
    public void onClose() {
        var closingEvent = PackedPacksApiImpl.getInstance().eventBus().post(new ScreenClosingEvent(this.context));
        if (closingEvent.isCommitted() || this.viewModel.shouldCommitOnClose()) {
            this.viewModel.commit();
        }
        if (this.context.isServerData() && !(this.previous instanceof PackSelectionScreen)) {
            return;
        }
        if (this.previous instanceof PackSelectionScreenAccessor packScreen) {
            ((PackSelectionModelAccessor) packScreen.getModel()).packed_packs$reset();
            packScreen.invokeReload();
        }
        this.minecraft.setScreen(this.previous);
    }

    @Override
    public void tick() {
        this.viewModel.pollWatcher();
    }

    private PackLayout getPackLayout(PackListType type) {
        return switch (type) {
            case AVAILABLE -> this.availableLayout;
            case ENABLED -> this.enabledLayout;
        };
    }

    private void visitDeepestList(PackListType type, Consumer<PackList> visitor) {
        this.getPackLayout(type).container().visitDeepestList(visitor);
    }

    private void applyFocusTarget(PackListType type, FocusTarget target) {
        this.clearFocus();
        PackListContainer listContainer = this.getPackLayout(type).container();
        this.setFocused(listContainer);
        ObjectsUtil.ifPresent(listContainer.getFocusPath(target), path -> path.applyFocus(true));
    }

    private void onUiEffect(UiEffect effect) {
        switch (effect) {
            case UiEffect.ScrollToTop(PackListType type) -> this.visitDeepestList(type, PackList::scrollToTop);
            case UiEffect.ScrollToLastSelected(PackListType type) ->
                    this.visitDeepestList(type, PackList::scrollToLastSelected);
            case UiEffect.FocusList(PackListType type) -> this.setFocused(this.getPackLayout(type).container());
            case UiEffect.Focus(PackListType type, String packId, boolean scroll) when packId == null ->
                    this.applyFocusTarget(type, new FocusTarget.LastSelected(scroll));
            case UiEffect.Focus(PackListType type, String packId, boolean scroll) ->
                    this.applyFocusTarget(type, new FocusTarget.PackEntry(packId, scroll));
        }
    }

    private @Nullable PackLayout getFocusedOrHoveredLayout() {
        return ObjectsUtil.firstNonNull(
                ObjectsUtil.pick(this.availableLayout, this.enabledLayout, pl -> pl.container() == this.getFocused()),
                ObjectsUtil.pick(this.availableLayout, this.enabledLayout, pl -> pl.container().isHovered()),
                ObjectsUtil.pick(this.availableLayout, this.enabledLayout, pl -> pl.container().isFocused())
        );
    }

    private @Nullable PackLayout getFocusedLayout() {
        return ObjectsUtil.firstNonNull(
                ObjectsUtil.pick(this.availableLayout, this.enabledLayout, pl -> pl.container() == this.getFocused()),
                ObjectsUtil.pick(this.availableLayout, this.enabledLayout, pl -> pl.container().isFocused())
        );
    }

    private ToggleableEditBox<Void> focusSearchField(PackLayout packLayout) {
        if (!Config.get().isShowActionBar()) this.toggleActionBar();
        ToggleableEditBox<Void> searchField = packLayout.getSearchField();
        this.clearFocus();
        this.setFocused(searchField);
        return searchField;
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent charEvent) {
        if (this.viewModel.isDragging()) {
            return true;
        }
        if (super.charTyped(charEvent)) {
            return true;
        }
        if (CollectionsUtil.anyMatch(this.dialogs, ToggleableDialog::isOpen)) {
            return false;
        }
        if (charEvent.codepoint() != KEY_SPACE && noModifiers(charEvent.modifiers())) {
            PackLayout packLayout = this.getFocusedOrHoveredLayout();
            if (packLayout != null && !packLayout.getSearchField().isFocused()) {
                return this.focusSearchField(packLayout).charTyped(charEvent);
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent keyEvent) {
        if (this.viewModel.isDragging()) {
            return true;
        }
        this.contextMenu.setOpen(false);
        if (keyEvent.isEscape()) {
            PackLayout packLayout = this.getFocusedLayout();
            if (packLayout != null) {
                PackListType type = packLayout.key().type();
                if (this.viewModel.closeFolder(type) || this.viewModel.closeFolder(type.other())) {
                    return true;
                }
            } else if (this.viewModel.closeFolder()) {
                return true;
            }
        }
        if (isDeveloperMode(keyEvent)) {
            this.viewModel.toggleDevMode();
            this.rebuildWidgets();
            return true;
        }
        if (isSwitchDefaultProfile(keyEvent)) {
            this.viewModel.switchDefaultProfile();
            return true;
        }
        if (isRefresh(keyEvent) && this.viewModel.canRefresh()) {
            this.viewModel.refreshRepository();
            return true;
        }
        if (isOpenProfiles(keyEvent)) {
            this.profilesSidebar.toggle();
            return true;
        }
        if (super.keyPressed(keyEvent)) {
            return true;
        }
        if (isRedo(keyEvent)) {
            this.viewModel.redo();
            return true;
        }
        if (isUndo(keyEvent)) {
            this.viewModel.undo();
            return true;
        }
        if (keyEvent.key() == KEY_BACKSPACE) {
            PackLayout packLayout = this.getFocusedOrHoveredLayout();
            if (packLayout != null) {
                ToggleableEditBox<Void> searchField = packLayout.getSearchField();
                if (!searchField.isFocused() && !searchField.getValue().isEmpty()) {
                    return this.focusSearchField(packLayout).keyPressed(keyEvent);
                }
            }
        }
        return false;
    }

    private void openContextMenu(int mouseX, int mouseY) {
        if (this.contextMenu.isMouseOver(mouseX, mouseY)) return;

        var extensions = ContextMenuEventImpl.postScreen(this.context);
        var prefExtensions = ContextMenuEventImpl.postPreferences(this.context);

        this.buildItems(mouseX, mouseY)
                .whenNonNull(extensions.getItems(ContextMenuEvent.Screen.Pos.TOP))
                .ifTrue((items, b) -> b.addAll(items))
                .when(Config.get().isDevMode())
                .ifTrue(dev -> dev.separatorIfNonEmpty()
                        .whenNonNull(this.viewModel.getSelectedProfile())
                        .ifTrue(b -> b.
                                add(devItem(ResourceUtil.getText("profile.save"))
                                        .action(this.viewModel::saveSelectedProfile)
                                        .build())
                                .separator())
                        .parent(children -> devItem(ResourceUtil.getText("preferences"))
                                .addChildren(children)
                                .build(), builder -> builder
                                .whenNonNull(prefExtensions.getItems(ContextMenuEvent.Preferences.Pos.TOP))
                                .ifTrue((items, b) -> b.addAll(items))
                                .addAll(PreferenceToggle.standardOptions())
                                .whenNonNull(prefExtensions.getItems(ContextMenuEvent.Preferences.Pos.BOTTOM))
                                .ifTrue((items, b) -> b.addAll(items))
                                .add(devItem(ResourceUtil.getText("preferences.reset"))
                                        .action(Preferences::reset)
                                        .build()))
                )
                .separatorIfNonEmpty()
                .simpleItem(ResourceUtil.getText("reset_enabled"), this.viewModel::isUnlocked, this.viewModel::resetChanges)
                .simpleItem(ResourceUtil.getText("refresh"), this.viewModel::canRefresh, this.viewModel::refreshRepository)
                .when(this.viewModel.getAdditionalFolders(), List::isEmpty)
                .ifTrue(b -> b.simpleItem(OPEN_FOLDER_TEXT, this.viewModel::openBaseDir))
                .orElse((dirs, b) -> b
                        .parent(OPEN_FOLDER_TEXT, p -> p
                                .add(new DirectoryMenuItem(this.viewModel.getBaseDir()))
                                .separator()
                                .iterate(dirs)
                                .map(DirectoryMenuItem::new)))
                .whenNonNull(extensions.getItems(ContextMenuEvent.Screen.Pos.BOTTOM))
                .ifTrue((items, b) -> b.addAll(items))
                .peek(items -> {
                    boolean hasHeader = !items.isEmpty() && items.getFirst() instanceof PackMenuHeader;
                    int yOffset = hasHeader ? this.contextMenu.getItemHeight() : 0;
                    this.contextMenu.open(mouseX, mouseY - yOffset, items);
                });
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent mouseEvent, boolean doubleClicked) {
        if (this.viewModel.isDragging()) {
            return true;
        }
        if (isClickForward(mouseEvent)) {
            this.viewModel.redo();
            return true;
        }
        if (isClickBack(mouseEvent)) {
            this.viewModel.undo();
            return true;
        }

        boolean clicked = ToggleableDialogContainer.super.mouseClicked(mouseEvent, doubleClicked);

        if (isRightClick(mouseEvent) && !this.optionsModal.isMouseOver(mouseEvent.x(), mouseEvent.y())) {
            this.openContextMenu((int) mouseEvent.x(), (int) mouseEvent.y());
        } else if (clicked && !this.contextMenu.isMouseOver(mouseEvent.x(), mouseEvent.y())) {
            this.contextMenu.setOpen(false);
        }

        if (!clicked) {
            ObjectsUtil.ifPresent(this.getCurrentFocusPath(), path -> path.applyFocus(false));
        }

        return clicked;
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
        return this.viewModel.isDragging() || super.mouseDragged(mouseButtonEvent, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent mouseButtonEvent) {
        ActiveAction.Dragging dragged = this.viewModel.state().dragging();
        if (isLeftClick(mouseButtonEvent) && dragged != null) {
            if (this.availableLayout.container().isHovered()) {
                this.availableLayout.container().onDrop(dragged, (int) mouseButtonEvent.x(), (int) mouseButtonEvent.y());
            } else if (this.enabledLayout.container().isHovered()) {
                this.enabledLayout.container().onDrop(dragged, (int) mouseButtonEvent.x(), (int) mouseButtonEvent.y());
            } else {
                this.viewModel.dispatch(new PackListIntent.Drop(dragged.target(), dragged.ctx(), dragged.payload(), null, 0));
            }
            return true;
        }

        return super.mouseReleased(mouseButtonEvent);
    }

    @Override
    public List<ToggleableDialog<?>> getDialogs() {
        return this.dialogs;
    }

    @Override
    public @Nullable GuiEventListener getHovered() {
        return this.hoveredElement;
    }

    @Override
    public void render(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.hoveredElement = this.findHovered(mouseX, mouseY);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (this.viewModel.isDragging()) {
            if (this.viewModel.isUnlocked()) {
                this.dragActionRenderer.render(
                        this.availableLayout.container(),
                        this.enabledLayout.container(),
                        this.viewModel.state().dragging(),
                        guiGraphics,
                        mouseX,
                        mouseY,
                        partialTick
                );
            } else {
                guiGraphics.requestCursor(CursorTypes.NOT_ALLOWED);
            }
        }

        if (this.context.devMode()) {
            float scale = 0.5f;
            int y = (int) ((height - this.font.lineHeight * scale) / scale);

            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().scale(scale);
            guiGraphics.drawString(this.font, ResourceUtil.getText("dev_mode", DEV_MODE_SHORTCUT), 0, y, Theme.WHITE.getARGB());
            guiGraphics.pose().popMatrix();
        }
    }
}
