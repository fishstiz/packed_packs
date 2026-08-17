package io.github.fishstiz.packed_packs.gui.screens;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.fishstiz.fidgetz.v0.gui.components.*;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZComposedLayout;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZFlexLayout;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZLayout;
import io.github.fishstiz.fidgetz.v0.gui.screens.FZScreen;
import io.github.fishstiz.fidgetz.v0.gui.state.FZMutableRef;
import io.github.fishstiz.fidgetz.v0.gui.state.FZRef;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.api.Event;
import io.github.fishstiz.packed_packs.api.Preference;
import io.github.fishstiz.packed_packs.api.events.ClosingEvent;
import io.github.fishstiz.packed_packs.api.events.ContextMenuEvent;
import io.github.fishstiz.packed_packs.api.events.InitializeEvent;
import io.github.fishstiz.packed_packs.api.events.InitializeLayoutEvent;
import io.github.fishstiz.packed_packs.compat.minecraftcursor.MinecraftCursor;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.config.Preferences;
import io.github.fishstiz.packed_packs.gui.FocusTarget;
import io.github.fishstiz.packed_packs.gui.UiEffect;
import io.github.fishstiz.packed_packs.gui.components.*;
import io.github.fishstiz.packed_packs.gui.layouts.*;
import io.github.fishstiz.packed_packs.gui.states.*;
import io.github.fishstiz.packed_packs.gui.actions.intents.Intent;
import io.github.fishstiz.packed_packs.gui.actions.intents.PackListIntent;
import io.github.fishstiz.packed_packs.gui.actions.intents.ProfileIntent;
import io.github.fishstiz.packed_packs.impl.PackedPacksApiImpl;
import io.github.fishstiz.packed_packs.impl.events.ContextMenuEventImpl;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionModelAccessor;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionScreenAccessor;
import io.github.fishstiz.packed_packs.util.Colors;
import io.github.fishstiz.packed_packs.util.PackUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.NoticeWithLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE;
import static com.mojang.blaze3d.platform.InputConstants.KEY_SPACE;
import static io.github.fishstiz.packed_packs.util.InputUtil.*;
import static io.github.fishstiz.packed_packs.util.GuiUtils.*;

public class PackedPacksScreen extends FZScreen {
    private static final Component DEV_MODE_TEXT = Component.translatable("packed_packs.dev_mode", DEV_MODE_SHORTCUT);
    private static final Component SEARCH_TEXT = Component.translatable("packed_packs.search")
            .append(CommonComponents.ELLIPSIS)
            .withStyle(Style.EMPTY.applyFormats(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    private final @Nullable Screen parent;
    private final PackSelectionScreenArgs original;
    private final PackedPacksContext context;
    private final FZMutableRef<Boolean> ribbonOpen = new FZMutableRef<>(Config.get().isShowActionBar());
    private final FZMutableRef<Boolean> sidebarOpen = new FZMutableRef<>(false);
    private final PackListContainer availableList;
    private final PackListContainer enabledList;
    private final DragActionRenderer dragActionRenderer;
    private @Nullable FZLayout layout;
    private @Nullable FZTextField availableSearch;
    private @Nullable FZTextField enabledSearch;
    private boolean initialized = false;
    private boolean preloading;

    public PackedPacksScreen(Minecraft minecraft, @Nullable Screen parent, PackSelectionScreenArgs args) {
        super(Component.literal(PackedPacks.MOD_NAME));
        this.minecraft = minecraft;
        this.parent = parent;
        this.original = args;

        this.context = new PackedPacksContext(minecraft, parent, this, args);

        this.availableList = PackListContainer.createHead(context, PackListType.AVAILABLE);
        this.enabledList = PackListContainer.createHead(context, PackListType.ENABLED);
        this.dragActionRenderer = new DragActionRenderer(context.iconCache(), 100.0f, minecraft.font, availableList, enabledList);

        context.setEffectHandler(this::onUiEffect);
        context.state().subscribe("RenameModal", PackedPacksState::renamingPack, this::initRenameModal);
        context.state().subscribe("AliasModal", PackedPacksState::editingAliases, this::initAliasModal);
        sidebarOpen.subscribe("ProfilesSidebar", this::initSidebar);
    }

    // In v2.1.2, all components used to be initialized in the ctor parallelly due to slow loading.
    // Dialogs are now loaded lazily, but the initial screen load time got ~100% slower with the updated fidgetz components,
    // so now we preload the components and avoid initializing the API.
    static void preload(Minecraft minecraft) {
        PackedPacksScreen screen = new PackedPacksScreen(minecraft, null, PackSelectionScreenArgs.dummy(minecraft));
        screen.context.stopWatcher();
        screen.preloading = true;
        screen.init();
    }

    private <T extends Event> T postEvent(T event) {
        if (!this.preloading) {
            PackedPacksApiImpl.getInstance().eventBus().post(event);
        }
        return event;
    }

    @Override
    public void added() {
        if (this.initialized) {
            context.initializeState();
            context.reload();
            context.startWatcher();
        }
    }

    @Override
    public void removed() {
        Config.get().setShowActionBar(ribbonOpen.value());
        context.cancelReload();
        context.stopWatcher();
        context.saveState();
    }

    @Override
    protected void init() {
        if (this.initialized) return;

        List<Consumer<InitializeEvent.Post>> postActions = new ArrayList<>();
        postEvent(new InitializeEvent.Pre(context, postActions::add));

        super.init();
        context.initializeState();

        this.initialized = true;

        InitializeEvent.Post postInit = new InitializeEvent.Post(context);
        postActions.forEach(listener -> listener.accept(postInit));
        postEvent(postInit);

        context.startWatcher();
    }

    private FZRef<PackedPacksState> state() {
        return context.state();
    }

    private FZTextField createSearchField(PackListType type) {
        return FZTextField.bind("SearchField@" + type, state()
                .map(s -> s.getTailList(type).query().search())
                .map(value -> FZTextField.builder()
                        .text(value == null ? "" : value)
                        .onChange(e -> context.dispatch(new PackListIntent.Search(
                                state().value().getTailKey(type),
                                e.value()
                        )))
                        .hint(SEARCH_TEXT)
                        .toProps()
                )
        );
    }

    private FZButton createTransferAllButton(PackListType type) {
        PackListContainer container = type.available() ? availableList : enabledList;
        Component message = Component.literal(type.available() ? ">>" : "<<");

        return FZButton.bind("TransferAllButton@" + type, state()
                .map(s -> s.profiles().isLocked()
                          || s.getTailList(PackListType.AVAILABLE).module()
                          || s.getTailList(PackListType.ENABLED).module()
                )
                .map(locked -> FZButton.builder()
                        .message(message)
                        .tooltip(Component.translatable("packed_packs.transfer_all.info"))
                        .onPress(() -> container.visitLeafList(PackList::transferAll))
                        .square()
                        .active(!locked)
                        .toProps()
                )
        );
    }

    @Override
    protected void collectChildren(GuiComponentCollector collector) {
        InitializeLayoutEvent event = postEvent(new InitializeLayoutEvent(context));

        FZFlexLayout root = FZFlexLayout.vertical(this).spacing(SPACING);
        {
            FZFlexLayout header = root.child(horizontal(), root.flexChildHorizontalSettings());

            header.child(FZIconButton.builder()
                    .square()
                    .tooltip(PROFILE_TITLE_TEXT)
                    .focusOnInteraction(false)
                    .icon(new WidgetElements(HAMBURGER_RECT, 16, 16))
                    .onPress(() -> sidebarOpen.set(prev -> !prev))
                    .build());

            PreferenceHelper.wrap(Preferences.ACTION_BAR_WIDGET, FZIconButton.builder()
                    .square()
                    .tooltip(Component.translatable("packed_packs.toggle_actionbar.info"))
                    .icon(new WidgetElements(PackedPacks.id("icon/filter"), 16, 16))
                    .onPress(() -> ribbonOpen.set(prev -> !prev))
                    .build()).ifPresent(header::child);

            header.child(
                    ProfileTitleLayout.create(state().map(PackedPacksState::profiles), context::dispatch),
                    header.flexChildHorizontalSettings()
            );

            event.getPendingWidgets(InitializeLayoutEvent.Pos.AFTER_TITLE).forEach(header::child);

            PreferenceHelper.wrap(Preferences.OPTIONS_WIDGET, FZIconButton.builder()
                    .square()
                    .message(OPTIONS_TEXT)
                    .tooltip(OPTIONS_TEXT)
                    .icon(new WidgetElements(PackedPacks.id("icon/gear"), 16, 16))
                    .onPress(this::openOptions)
                    .build()).ifPresent(header::child);

            PreferenceHelper.wrap(Preferences.ORIGINAL_SCREEN_WIDGET, FZIconButton.builder()
                    .square()
                    .tooltip(Component.translatable("packed_packs.original_screen.info"))
                    .icon(new WidgetElements(PackedPacks.id("icon/exit"), 16, 16))
                    .onPress(() -> {
                        if (minecraft != null) {
                            if (parent instanceof PackSelectionScreen screen) {
                                minecraft.setScreen(screen);
                            } else {
                                minecraft.setScreen(original.createScreen(parent));
                            }
                        }
                    })
                    .build()).ifPresent(header::child);
        }

        {
            FZFlexLayout body = root.child(vertical(), root.flexChildSettings());
            {
                FZFlexLayout ribbon = body.child(horizontal(), body.flexChildHorizontalSettings()).visible(ribbonOpen.value());
                {
                    FZFlexLayout leftRibbon = ribbon.child(horizontal(), ribbon.flexChildHorizontalSettings());

                    this.availableSearch = leftRibbon.child(
                            createSearchField(PackListType.AVAILABLE),
                            leftRibbon.flexChildHorizontalSettings()
                    );

                    leftRibbon.child(FZDropdown.bind("AvailableSortDropdown", state()
                            .map(s -> s.getTailList(PackListType.AVAILABLE).query().sort())
                            .map(sort -> {
                                boolean active = sort != null && !(sort instanceof SortOption.Locked);
                                Component valueText = sort != null && active ? sort.text() : CommonComponents.EMPTY;
                                Component sortText = Component.translatable("packed_packs.sort");

                                return FZDropdown.builder(this)
                                        .width(40)
                                        .minContainerWidth(175)
                                        .hideMessage(true)
                                        .message(sortText)
                                        .active(active)
                                        .tooltip(active ? CommonComponents.optionNameValue(sortText, valueText) : sortText)
                                        .leftIcon(sort == null ? null : padded16Sprite(sort.icon()))
                                        .entryDivider(null)
                                        .entries(SortOptions.menuItems(option -> context.dispatch(new PackListIntent.Sort(
                                                state().value().getTailKey(PackListType.AVAILABLE),
                                                option
                                        ))))
                                        .toProps();
                            })));

                    if (Preferences.INCOMPATIBLE_TOGGLE_WIDGET.get() || Config.get().isDevMode()) {
                        leftRibbon.child(PreferenceHelper.wrapNonNull(
                                Preferences.INCOMPATIBLE_TOGGLE_WIDGET,
                                FZIconButton.bind("AvailableIncompatibleButton", state()
                                        .map(s -> s.getTailList(PackListType.AVAILABLE).query().hideIncompatible())
                                        .map(value -> {
                                            ResourceLocation icon = value
                                                    ? PackedPacks.id("icon/incompatible_hidden")
                                                    : PackedPacks.id("icon/incompatible");

                                            return FZIconButton.builder()
                                                    .square()
                                                    .message(Component.translatable("packed_packs.hide_incompatible"))
                                                    .tooltip(Component.translatable("packed_packs.hide_incompatible.info"))
                                                    .icon(new WidgetElements(icon, 16, 16))
                                                    .onPress(() -> context.dispatch(new PackListIntent.HideIncompatible(
                                                            state().value().getTailKey(PackListType.AVAILABLE),
                                                            !value
                                                    )))
                                                    .toProps();
                                        })))
                        );
                    }

                    leftRibbon.child(createTransferAllButton(PackListType.AVAILABLE));
                }

                {
                    FZFlexLayout rightRibbon = ribbon.child(horizontal(), ribbon.flexChildHorizontalSettings());

                    rightRibbon.child(createTransferAllButton(PackListType.ENABLED));

                    this.enabledSearch = rightRibbon.child(
                            createSearchField(PackListType.ENABLED),
                            rightRibbon.flexChildHorizontalSettings()
                    );
                }

                ribbon.visitWidgets(widget -> widget.visible = ribbonOpen.value());

                ribbonOpen.subscribe("Ribbon", value -> {
                    ribbon.visible(value);
                    ribbon.visitWidgets(widgets -> widgets.visible = value);
                    body.arrangeElements();
                });
            }
            {
                FZFlexLayout content = body.child(horizontal(), body.flexChildSettings());
                content.child(availableList, content.flexChildSettings());
                content.child(enabledList, content.flexChildSettings());
            }
        }
        {
            FZFlexLayout footer = root.child(horizontal(), root.flexChildHorizontalSettings());
            {
                FZFlexLayout leftFooter = footer.child(horizontal(), footer.flexChildHorizontalSettings());

                event.getPendingWidgets(InitializeLayoutEvent.Pos.BEFORE_FOOTER).forEach(leftFooter::child);

                {
                    FZFlexLayout folders = leftFooter.child(FZFlexLayout.horizontal(), leftFooter.flexChildHorizontalSettings());

                    folders.child(FZButton.builder()
                            .message(Component.translatable("pack.openFolder"))
                            .tooltip(Component.translatable("pack.folderInfo"))
                            .onPress(() -> PackedPacks.openPath(context.packDir()))
                            .build(), folders.flexChildHorizontalSettings());

                    List<Path> paths = context.otherDirectories();
                    if (!paths.isEmpty()) {
                        FZDropdown.Builder dropdown = FZDropdown.builder(this)
                                .hideMessage()
                                .size(20, 20)
                                .minContainerWidth(150, HorizontalDirection.LEFT)
                                .entryDivider(null);

                        for (Path path : paths) {
                            dropdown.entry(Component.literal(path.getFileName().toString()), () -> PackedPacks.openPath(path));
                        }

                        folders.child(dropdown.build());
                    }
                }

                event.getPendingWidgets(InitializeLayoutEvent.Pos.AFTER_LEFT_FOOTER).forEach(leftFooter::child);
            }
            {
                FZFlexLayout rightFooter = footer.child(horizontal(), footer.flexChildHorizontalSettings());

                event.getPendingWidgets(InitializeLayoutEvent.Pos.BEFORE_RIGHT_FOOTER).forEach(rightFooter::child);

                if (context.isClientResources()) {
                    rightFooter.child(FZButton.builder()
                            .message(Component.translatable("packed_packs.apply"))
                            .onPress(context::commit)
                            .build(), rightFooter.flexChildHorizontalSettings());
                }

                event.getPendingWidgets(InitializeLayoutEvent.Pos.BETWEEN_RIGHT_FOOTER).forEach(rightFooter::child);

                rightFooter.child(FZButton.builder()
                        .message(CommonComponents.GUI_DONE)
                        .onPress(this::onClose)
                        .build(), rightFooter.flexChildHorizontalSettings());

                event.getPendingWidgets(InitializeLayoutEvent.Pos.AFTER_FOOTER).forEach(rightFooter::child);
            }
        }

        layout = FZComposedLayout.contain(this, root)
                .padding(SPACING)
                .center()
                .clamp()
                .get();

        if (!this.preloading) {
            layout.arrangeElements();
            layout.visitWidgets(collector::renderableWidget);
        }
    }

    @Override
    protected void repositionElements() {
        if (layout == null) {
            super.repositionElements();
        } else {
            layout.fidgetz$setSize(width, height);
        }

        dialogManager.remove(GLOBAL_CONTEXT_MENU_ID);
        fidgetz$Dialogs().forEach(FZDialog::repositionElements);
    }

    private void openOptions() {
        dialogManager.put(FZModal.builder(this, OptionsLayout.create(this, Config.packs(original.packType())))
                .id("OptionsModal")
                .popoverOrder(0)
                .padding(SPACING, SPACING + 2, SPACING, SPACING)
                .margin(SPACING)
                .centered()
                .buildAndOpen());
    }

    @Override
    protected void openContextMenu(double x, double y, boolean focus) {
        if (fidgetz$Dialogs().stream().noneMatch(dialog -> dialog.isOpen() && dialog.fidgetz$popoverOrder() < 1)) {
            dialogManager.put(FZContextMenu.builder(this)
                    .id(GLOBAL_CONTEXT_MENU_ID)
                    .maxHeight((int) Math.max((height * .8), 240))
                    .popoverOrder(1)
                    .sectionDivider(FZPopoverMenuItem.createDivider(PackedPacks.id("widget/contextmenu_section_divider"), 1))
                    .noEntryDivider()
                    .rowSpacing(0)
                    .padding(1)
                    .focusOnOpen(focus)
                    .buildAndOpen(x, y, fidgetz$collectContextEntries(x, y)));
        }
    }

    private void initRenameModal() {
        FZRef<ActiveAction.@Nullable RenamingPack> ref = state().map(PackedPacksState::renamingPack);

        dialogManager.put(FZModal.bind("RenameModal", ref.map(rename -> {
            PackRenameLayout layout = PackRenameLayout.create(ref, context::dispatch, context.iconCache());
            return FZModal.builder(this, layout)
                    .id("RenameModal")
                    .popoverOrder(2)
                    .padding(SPACING)
                    .centered()
                    .captureClick()
                    .captureFocus()
                    .closeAfterClickOutOfBounds()
                    .open(rename != null)
                    .onClose(layout::onClose)
                    .toProps();
        })));
    }

    private void initAliasModal() {
        FZRef<ActiveAction.@Nullable EditingAliases> ref = state().map(PackedPacksState::editingAliases);
        dialogManager.put(FZModal.bind("AliasModal", ref.map(editingAliases -> {
            PackAliasLayout layout = PackAliasLayout.create(ref, context::dispatch, context.iconCache());
            return FZModal.builder(this, layout)
                    .id("AliasModal")
                    .popoverOrder(2)
                    .backdrop(null)
                    .padding(SPACING)
                    .centered()
                    .captureClick(false)
                    .open(editingAliases != null)
                    .onClose(layout::onClose)
                    .toProps();
        })));
    }

    private void initSidebar() {
        SidebarLayout sidebar = SidebarLayout.create(state(), context::dispatch, context.packType(), () -> sidebarOpen.set(false));
        dialogManager.put(FZModal.bind("ProfilesSidebar", sidebarOpen.map(open -> FZModal.builder(this, sidebar)
                .id("ProfilesSidebar")
                .popoverOrder(5)
                .alignTopLeft()
                .flexHeight()
                .padding(SPACING)
                .backdrop(null)
                .captureClick(false)
                .open(open)
                .onClose(() -> sidebarOpen.set(false))
                .toProps())));
    }

    @Override
    public void rebuildWidgets() {
        if (!this.initialized) return;
        context.cancelReload();
        clearWidgets();
        context.saveState();
        this.initialized = false;
        init();
        availableList.rebuildEntries();
        enabledList.rebuildEntries();
        dialogManager.refreshDialogs();
    }

    @Override
    public void onFilesDrop(List<Path> files) {
        if (minecraft == null) return;
        minecraft.setScreen(new ConfirmScreen(
                confirmed -> {
                    if (!confirmed) {
                        minecraft.setScreen(this);
                        return;
                    }
                    PackUtil.PathValidationResults results = PackUtil.validatePaths(files);
                    if (!results.symlinkWarnings().isEmpty()) {
                        minecraft.setScreen(NoticeWithLinkScreen.createPackSymlinkWarningScreen(() -> minecraft.setScreen(this)));
                        return;
                    }
                    if (!results.valid().isEmpty()) {
                        PackSelectionScreenAccessor.packed_packs$copyPacks(minecraft, results.valid(), context.packDir());
                        context.rebuild(); // fix reload
                    }
                    if (!results.rejected().isEmpty()) {
                        String rejectedNames = PackUtil.joinPackNames(results.rejected());
                        minecraft.setScreen(new AlertScreen(
                                () -> minecraft.setScreen(this),
                                Component.translatable("pack.dropRejected.title"),
                                Component.translatable("pack.dropRejected.message", rejectedNames)
                        ));
                        return;
                    }
                    minecraft.setScreen(this);
                },
                Component.translatable("pack.dropConfirm"),
                Component.literal(PackUtil.joinPackNames(files))
        ));
    }

    @Override
    public void onClose() {
        ClosingEvent closingEvent = postEvent(new ClosingEvent(context));
        if (closingEvent.isCommitted() || !(context.configs().user() instanceof Config.ResourcePacks config) || config.isApplyOnClose()) {
            context.commit();
        }
        if (context.isServerData() && !(parent instanceof PackSelectionScreen)) {
            return;
        }
        if (parent instanceof PackSelectionScreenAccessor packScreen) {
            ((PackSelectionModelAccessor) packScreen.packed_packs$model()).packed_packs$reset();
            packScreen.packed_packs$reload();
        }
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void tick() {
        context.pollWatcher();
    }

    private PackListContainer getPackList(PackListType type) {
        return switch (type) {
            case AVAILABLE -> availableList;
            case ENABLED -> enabledList;
        };
    }

    private void visitDeepestList(PackListType type, Consumer<PackList> visitor) {
        this.getPackList(type).visitLeafList(visitor);
    }

    private void applyFocusTarget(PackListType type, FocusTarget target) {
        clearFocus();
        PackListContainer list = getPackList(type);
        setFocused(list);
        ComponentPath targetPath = list.getFocusPath(target);
        if (targetPath != null) targetPath.applyFocus(true);
    }

    private void onUiEffect(UiEffect effect) {
        switch (effect) {
            case UiEffect.ScrollToTop(PackListType type) -> visitDeepestList(type, PackList::scrollToTop);
            case UiEffect.ScrollToLastSelected(PackListType type) ->
                    visitDeepestList(type, PackList::scrollToLastSelected);
            case UiEffect.FocusList(PackListType type) -> setFocused(getPackList(type));
            case UiEffect.Focus(PackListType type, String packId, boolean scroll) when packId == null ->
                    applyFocusTarget(type, new FocusTarget.LastSelected(scroll));
            case UiEffect.Focus(PackListType type, String packId, boolean scroll) ->
                    applyFocusTarget(type, new FocusTarget.PackEntry(packId, scroll));
        }
    }

    private PackListType getClosestListType() {
        GuiEventListener focused = getFocused();
        if (focused == availableList) {
            return PackListType.AVAILABLE;
        } else if (focused == enabledList) {
            return PackListType.ENABLED;
        }

        GuiEventListener hovered = fidgetz$getHovered();
        if (hovered == availableList) {
            return PackListType.AVAILABLE;
        } else if (hovered == enabledList) {
            return PackListType.ENABLED;
        }

        return PackListType.AVAILABLE;
    }

    private @Nullable FZTextField getClosestSearchField() {
        if (availableSearch == null || enabledSearch == null) return null;

        GuiEventListener focused = getFocused();
        if (focused == availableList) {
            return availableSearch;
        } else if (focused == enabledList) {
            return enabledSearch;
        }

        GuiEventListener hovered = fidgetz$getHovered();
        if (hovered == availableList || hovered == availableSearch) {
            return availableSearch;
        } else if (hovered == enabledList || hovered == enabledSearch) {
            return enabledSearch;
        }

        return null;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (state().value().dragging() != null) {
            return true;
        }
        if (super.charTyped(codePoint, modifiers)) {
            return true;
        }
        if (fidgetz$Dialogs().stream().anyMatch(FZDialog::isOpen)) {
            return false;
        }
        if (codePoint != KEY_SPACE && (noModifiers(modifiers) || shiftOnly(modifiers))) {
            FZTextField searchField = getClosestSearchField();
            if (searchField != null && !searchField.isFocused()) {
                ribbonOpen.set(true);
                setFocused(searchField);
                return searchField.charTyped(codePoint, modifiers);
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (state().value().dragging() != null) {
            return true;
        }
        dialogManager.remove(GLOBAL_CONTEXT_MENU_ID);
        if (keyCode == KEY_ESCAPE && dialogManager.dialogs().stream().noneMatch(FZDialog::isOpen)) {
            PackListType type = getClosestListType();
            PackListKey key = null;

            for (int i = 0; i < 2; i++) {
                key = state().value().getTailKey(type);
                if (key.depth() > 0) break;
                type = type.other();
            }

            if (key.depth() > 0) {
                context.dispatch(new PackListIntent.CloseFolder(key.unnest()));
                return true;
            }
        }
        if (isDeveloperMode(keyCode, modifiers)) {
            context.dispatch(new Intent.ToggleDevMode());
            rebuildWidgets();
            return true;
        }
        if (isSwitchDefaultProfile(keyCode, modifiers)) {
            context.dispatch(new ProfileIntent.Select(state().value().profiles().isSelectedDefault()
                    ? null
                    : state().value().profiles().defaultProfile())
            );
            return true;
        }
        if (isRefresh(keyCode, modifiers)) {
            if (context.canReload()) {
                context.reload();
            }
            return true;
        }
        if (isOpenProfiles(keyCode, modifiers)) {
            sidebarOpen.set(prev -> !prev);
            return true;
        }
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (isRedo(keyCode, modifiers)) {
            context.redo();
            return true;
        }
        if (isUndo(keyCode, modifiers)) {
            context.undo();
            return true;
        }
        if (keyCode == InputConstants.KEY_BACKSPACE) {
            FZTextField searchField = getClosestSearchField();
            if (searchField != null && !searchField.isFocused() && !searchField.getValue().isEmpty()) {
                ribbonOpen.set(true);
                setFocused(searchField);
                return searchField.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        if (isSearch(keyCode, modifiers)) {
            FZTextField searchField = getClosestSearchField();
            if (searchField != null && !searchField.isFocused()) {
                ribbonOpen.set(true);
                setFocused(searchField);
                return true;
            }
        }
        return false;
    }

    @Override
    public void fidgetz$updateContextEntries(double x, double y, FZContextMenu.Collector collector) {
        ContextMenuEventImpl<ContextMenuEvent.Screen.Pos> screenExtensions = ContextMenuEventImpl.postScreen(context);

        screenExtensions.entries(ContextMenuEvent.Screen.Pos.TOP).forEach(collector::addEntry);

        super.fidgetz$updateContextEntries(x, y, collector);
        collector.nextSection();

        if (context.devMode()) {
            collector.nextSection();

            if (state().value().profiles().selectedProfile() != null) {
                collector.addEntry(builder -> buildDevEntry(builder)
                        .message(Component.translatable("packed_packs.profile.save"))
                        .onPress(context::saveSelectedProfile));
                collector.nextSection();
            }

            FZPopoverMenuItem.Builder preferences = buildDevEntry(FZPopoverMenuItem.builder())
                    .message(Component.translatable("packed_packs.preferences"));

            ContextMenuEventImpl<ContextMenuEvent.Preferences.Pos> prefExtensions = ContextMenuEventImpl.postPreferences(context);

            prefExtensions.entries(ContextMenuEvent.Preferences.Pos.TOP).forEach(preferences::child);

            List<Preferences.Option<Boolean>> standardOptions = Preferences.standardBooleanOptions();
            standardOptions.forEach(standardOption -> preferences.child(PreferenceHelper.createEntry(standardOption)));

            prefExtensions.entries(ContextMenuEvent.Preferences.Pos.BOTTOM).forEach(preferences::child);

            preferences.nextSection();
            preferences.child(builder -> buildDevEntry(builder
                    .message(Component.translatable("packed_packs.preferences.reset"))
                    .closeOnInteraction(false)
                    .onPress(() -> {
                        standardOptions.forEach(Preference::reset);
                        prefExtensions.getPreferences().forEach(Preference::reset);
                    })));

            collector.addEntry(preferences.build());
            collector.nextSection();
        }

        collector.addEntry(builder -> builder
                .message(Component.translatable("packed_packs.reset_enabled"))
                .active(() -> !state().value().profiles().isLocked())
                .onPress(() -> context.dispatch(new Intent.Reset())));

        collector.addEntry(builder -> builder
                .message(Component.translatable("packed_packs.refresh"))
                .active(context::canReload)
                .onPress(context::reload));

        List<Path> folders = context.otherDirectories();
        if (folders.isEmpty()) {
            collector.addEntry(builder -> builder
                    .message(Component.translatable("pack.openFolder"))
                    .onPress(() -> PackedPacks.openPath(context.packDir())));
        } else {
            FZPopoverMenuItem.Builder parent = FZPopoverMenuItem.builder().message(Component.translatable("pack.openFolder"));
            parent.child(builder -> builder
                    .message(Component.literal(context.packDir().getFileName().toString()))
                    .onPress(() -> PackedPacks.openPath(context.packDir())));
            parent.nextSection();

            for (Path folder : folders) {
                parent.child(FZPopoverMenuItem.builder()
                        .message(Component.literal(folder.getFileName().toString()))
                        .onPress(() -> PackedPacks.openPath(folder))
                        .build());
            }

            collector.addEntry(parent.build());
        }

        screenExtensions.entries(ContextMenuEvent.Screen.Pos.BOTTOM).forEach(collector::addEntry);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (state().value().dragging() != null) {
            return true;
        }
        if (isClickForward(button)) {
            context.redo();
            return true;
        }
        if (isClickBack(button)) {
            context.undo();
            return true;
        }

        boolean clicked = super.mouseClicked(mouseX, mouseY, button);
        if (!clicked && fidgetz$getHovered() == null) {
            ComponentPath path = getCurrentFocusPath();
            if (path != null) path.applyFocus(false);
        }

        return clicked;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return state().value().dragging() != null || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        ActiveAction.Dragging dragged = state().value().dragging();
        if (isLeftClick(button) && dragged != null) {
            if (availableList.isHovered()) {
                availableList.onDrop(dragged, (int) mouseX, (int) mouseY);
            } else if (enabledList.isHovered()) {
                enabledList.onDrop(dragged, (int) mouseX, (int) mouseY);
            } else {
                context.dispatch(new PackListIntent.Drop(dragged.src(), null, 0));
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        ActiveAction.Dragging dragging = state().value().dragging();
        if (dragging != null) {
            if (!state().value().profiles().isLocked()) {
                dragActionRenderer.render(dragging, graphics, mouseX, mouseY, partialTick);
            } else {
                MinecraftCursor.get().setNotAllowed();
            }
        }

        if (context.devMode()) {
            float scale = 0.5f;
            int y = (int) ((height - this.font.lineHeight * scale) / scale);
            graphics.pose().pushPose();
            graphics.pose().scale(scale, scale, 300f);
            graphics.drawString(font, DEV_MODE_TEXT, 0, y, Colors.WHITE);
            graphics.pose().popPose();
        }
    }
}
