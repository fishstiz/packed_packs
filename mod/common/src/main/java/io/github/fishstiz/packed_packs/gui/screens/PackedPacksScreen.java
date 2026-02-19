package io.github.fishstiz.packed_packs.gui.screens;

import com.google.common.collect.ImmutableList;
import io.github.fishstiz.fidgetz.gui.components.*;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.*;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.fidgetz.gui.renderables.ColoredRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.util.lang.FunctionsUtil;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.api.Event;
import io.github.fishstiz.packed_packs.api.events.ScreenContext;
import io.github.fishstiz.packed_packs.api.events.ScreenEvent;
import io.github.fishstiz.packed_packs.config.*;
import io.github.fishstiz.packed_packs.gui.components.contextmenu.DirectoryMenuItem;
import io.github.fishstiz.packed_packs.gui.components.contextmenu.PackMenuHeader;
import io.github.fishstiz.packed_packs.gui.components.pack.*;
import io.github.fishstiz.packed_packs.gui.layouts.pack.AvailablePacksLayout;
import io.github.fishstiz.packed_packs.gui.layouts.pack.CurrentPacksLayout;
import io.github.fishstiz.packed_packs.gui.layouts.pack.PackAliasLayout;
import io.github.fishstiz.packed_packs.gui.layouts.pack.PackLayout;
import io.github.fishstiz.packed_packs.gui.components.ToggleableHelper;
import io.github.fishstiz.packed_packs.impl.PackedPacksApiImpl;
import io.github.fishstiz.packed_packs.pack.*;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionModelAccessor;
import io.github.fishstiz.packed_packs.util.AsyncUtil;
import io.github.fishstiz.packed_packs.util.ToastUtil;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import io.github.fishstiz.packed_packs.gui.layouts.*;
import io.github.fishstiz.packed_packs.gui.components.events.*;
import io.github.fishstiz.packed_packs.gui.history.HistoryManager;
import io.github.fishstiz.packed_packs.gui.history.Restorable;
import io.github.fishstiz.packed_packs.gui.metadata.PackSelectionScreenArgs;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionScreenAccessor;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.fidgetz.util.lang.CollectionsUtil;
import io.github.fishstiz.fidgetz.util.lang.ObjectsUtil;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.util.Util;
import net.minecraft.client.gui.ComponentPath;
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
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.mojang.blaze3d.platform.InputConstants.KEY_BACKSPACE;
import static com.mojang.blaze3d.platform.InputConstants.KEY_SPACE;
import static io.github.fishstiz.packed_packs.util.InputUtil.*;
import static io.github.fishstiz.packed_packs.util.PackUtil.*;
import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.*;

public class PackedPacksScreen extends PackListEventHandler implements
        HoverStateHandler,
        ToggleableDialogContainer,
        ContextMenuContainer,
        Restorable<PackedPacksScreen.Snapshot> {
    private static final Component OPEN_FOLDER_TEXT = Component.translatable("pack.openFolder");
    private final Screen previous;
    private final PackSelectionScreenArgs original;
    private final ScreenContext context;
    private final HistoryManager<Snapshot> history;
    private final LayoutWrapper<FlexLayout> layout;
    private final ProfilesLayout profiles;
    private final PackOptionsContext options;
    private final PackRepositoryManager repository;
    private final AvailablePacksLayout availablePacks;
    private final CurrentPacksLayout currentPacks;
    private final FolderDialog folderDialog;
    private final List<PackList> packLists;
    private final FileRenameModal fileRenameModal;
    private final ContextMenu contextMenu;
    private final Modal<OptionsLayout> optionsModal;
    private final List<ToggleableDialog<?>> dialogs;
    private Modal<PackAliasLayout> aliasModal;
    private List<Path> additionalFolders;
    private CompletableFuture<Void> refreshFuture;
    private CompletableFuture<Void> watcherFuture;
    private PackWatcher watcher;
    private boolean showActionBar = Config.get().isShowActionBar();
    private @Nullable GuiEventListener hoveredElement;
    private boolean initialized = false;

    private PackedPacksScreen(Screen previous, PackSelectionScreenArgs original, boolean initState) {
        super(ResourceUtil.getModName());

        this.previous = previous;
        this.original = original;
        Config.Packs userConfig = Config.get().get(original.packType());
        DevConfig.Packs config = DevConfig.get().get(original.packType());

        this.context = new ScreenContext(
                this,
                () -> this.previous instanceof PackSelectionScreen originalScreen ? originalScreen : this.original.createDummy(),
                original.repository(),
                original.packType(),
                Config.get().isDevMode()
        );

        this.history = new HistoryManager<>();
        this.layout = new LayoutWrapper<>(FlexLayout.vertical(this::getMaxHeight).spacing(SPACING));
        this.layout.setPadding(SPACING);

        this.profiles = new ProfilesLayout(this, userConfig, config, this::onProfileChange, this::onProfileCopy);
        this.options = new PackOptionsContext(this.profiles::getProfile, userConfig, config);
        this.repository = new PackRepositoryManager(this.original.repository(), this.options, this.original.packDir());

        WidgetFactory.PackedPacksWidgets widgets = WidgetFactory.createWidgets(this, this.options, this.repository, this.assetManager);
        this.availablePacks = widgets.availablePacksLayout();
        this.currentPacks = widgets.currentPacksLayout();
        this.folderDialog = widgets.folderDialog();
        this.packLists = widgets.packLists();
        this.fileRenameModal = widgets.fileRenameModal();
        this.contextMenu = widgets.contextMenu();
        this.optionsModal = Modal.builder(this, new OptionsLayout(this.minecraft, this.layout::getHeight, userConfig))
                .setBackdrop(new ColoredRect(Theme.BLACK.withAlpha(0.5f)))
                .setCaptureFocus(true)
                .padding(SPACING)
                .build();

        if (Config.get().isDevMode()) {
            PackAliasLayout packAliasLayout = new PackAliasLayout(config, this.assetManager);
            this.aliasModal = Modal.builder(this, packAliasLayout)
                    .addListener(open -> {
                        if (!open) this.aliasModal.root().layout().saveAliases();
                    })
                    .padding(SPACING)
                    .build();
            this.dialogs = List.of(this.optionsModal, this.contextMenu, this.aliasModal, this.fileRenameModal, this.profiles.getSidebar(), this.folderDialog);
        } else {
            this.dialogs = List.of(this.optionsModal, this.contextMenu, this.fileRenameModal, this.profiles.getSidebar(), this.folderDialog);
        }

        this.initAdditionalFolders();
        if (initState) {
            if (userConfig.isLastViewedProfileRemembered()) {
                Profile lastViewed = userConfig.getLastViewedProfile();
                Profile defaultProfile = config.getDefaultProfile();
                if (Objects.equals(lastViewed, defaultProfile)) {
                    lastViewed = defaultProfile;
                }
                this.profiles.setProfile(lastViewed);
            } else {
                this.useSelected();
            }
        }
    }

    public PackedPacksScreen(Screen previous, PackSelectionScreenArgs original) {
        this(previous, original, true);
    }

    public PackedPacksScreen(Screen previous, PackSelectionScreenArgs original, Profile profile) {
        this(previous, original, false);
        this.profiles.setProfile(profile);
    }

    public PackedPacksScreen(Screen previous, PackSelectionScreenArgs original, PackGroup packs) {
        this(previous, original, false);
        this.applyPacks(packs.unselected(), packs.selected());
    }

    @Override
    public void added() {
        if (this.initialized) {
            this.refreshPacks();
            this.initAdditionalFolders();
            this.createWatcher();
        }
    }

    @Override
    public void removed() {
        this.closeWatcher();

        this.availablePacks.saveFilters();

        Profile profile = this.profiles.getProfile();
        this.syncProfile(profile);
        this.options.getUserConfig().setLastViewedProfile(profile);

        List<Profile> profiles = this.options.getUserConfig().getProfiles();
        this.options.getUserConfig().setProfileOrder(profiles);

        Runnable profileSaver = profile != null ? () -> Profiles.save(this.original.packType(), profile) : FunctionsUtil.nop();
        AsyncUtil.submitAndWait(
                Util.backgroundExecutor(),
                profileSaver,
                Config.get()::save,
                DevConfig.get()::save,
                Preferences.INSTANCE::save
        );
    }

    @Override
    protected void init() {
        if (this.initialized) return;

        this.profiles.init(this::setInitialFocus);

        Map<ScreenEvent.InitLayout.Phase, List<LayoutElement>> elements = new EnumMap<>(ScreenEvent.InitLayout.Phase.class);
        this.postApiEvent(new ScreenEvent.InitLayout(this.context, (phase, element) ->
                elements.computeIfAbsent(phase, p -> new ArrayList<>()).add(element)
        ));

        this.layout.layout().addChild(this.createHeader(elements));
        this.layout.layout().addFlexChild(this.createContents());
        this.layout.layout().addChild(this.createFooter(elements));

        this.dialogs.forEach(this::addWidget);
        this.layout.visitWidgets(this::addRenderableWidget);
        CollectionsUtil.forEachReverse(this.dialogs, this::addRenderableOnly);

        this.clearHistory();
        this.repositionElements();

        this.refreshPacks();
        this.createWatcher();

        this.initialized = true;
    }

    private FlexLayout createHeader(Map<ScreenEvent.InitLayout.Phase, List<LayoutElement>> elements) {
        FlexLayout header = FlexLayout.horizontal(this::getMaxWidth).spacing(SPACING);
        final boolean devMode = Config.get().isDevMode();

        header.addChild(
                FidgetzButton.builder()
                        .makeSquare()
                        .setMessage(ProfilesLayout.TITLE_TEXT)
                        .setTooltip(Tooltip.create(ProfilesLayout.TITLE_TEXT))
                        .setSprite(HAMBURGER_SPRITE)
                        .setOnPress(this.profiles.getSidebar()::toggle)
                        .build()
        );

        if (devMode || Preferences.INSTANCE.actionBarWidget.get()) {
            header.addChild(
                    ToggleableHelper.applyPref(Preferences.INSTANCE.actionBarWidget, FidgetzButton.<Void>builder())
                            .makeSquare()
                            .setTooltip(Tooltip.create(ResourceUtil.getText("toggle_actionbar.info")))
                            .setSprite(Sprite.of16(ResourceUtil.getIcon("filter")))
                            .setOnPress(this::toggleActionBar)
                            .build()
            );
        }
        header.addChild(this.profiles.getToggleNameButton());
        header.addFlexChild(this.profiles.getNameField());

        for (var element : elements.getOrDefault(ScreenEvent.InitLayout.Phase.AFTER_HEADER_TITLE, Collections.emptyList())) {
            header.addChild(element);
        }

        if (devMode || Preferences.INSTANCE.optionsWidget.get()) {
            header.addChild(
                    ToggleableHelper.applyPref(Preferences.INSTANCE.optionsWidget, FidgetzButton.<Void>builder())
                            .makeSquare()
                            .setMessage(OPTIONS_TEXT)
                            .setTooltip(Tooltip.create(OPTIONS_TEXT.copy().append(CommonComponents.ELLIPSIS)))
                            .setSprite(Sprite.of16(ResourceUtil.getIcon("gear")))
                            .setOnPress(this.optionsModal::toggle)
                            .build()
            );
        }
        if (devMode || Preferences.INSTANCE.originalScreenWidget.get()) {
            header.addChild(
                    ToggleableHelper.applyPref(Preferences.INSTANCE.originalScreenWidget, FidgetzButton.<Void>builder())
                            .makeSquare()
                            .setTooltip(Tooltip.create(ResourceUtil.getText("original_screen.info").append(CommonComponents.ELLIPSIS)))
                            .setSprite(Sprite.of16(ResourceUtil.getIcon("exit")))
                            .setOnPress(this::setOriginalScreen)
                            .build()
            );
        }

        return header;
    }

    private FlexLayout createContents() {
        FlexLayout contents = FlexLayout.horizontal(this::getMaxWidth).spacing(SPACING);
        FlexLayout packLayout = FlexLayout.vertical().spacing(SPACING);
        this.availablePacks.init(contents.addFlexChild(packLayout, true));
        this.currentPacks.init(contents.addFlexChild(packLayout.copyLayout(), true));
        this.currentPacks.getSearchField().addListener(this::recordState);
        this.availablePacks.getSearchField().addListener(this::recordState);
        return contents;
    }

    private FlexLayout createFooter(Map<ScreenEvent.InitLayout.Phase, List<LayoutElement>> elements) {
        final FlexLayout footer = FlexLayout.horizontal(this::getMaxWidth).spacing(SPACING);
        FlexLayout firstColumn = FlexLayout.horizontal().spacing(SPACING);
        FlexLayout secondColumn = firstColumn.copyLayout();

        for (var element : elements.getOrDefault(ScreenEvent.InitLayout.Phase.BEFORE_FOOTER, Collections.emptyList())) {
            firstColumn.addChild(element);
        }

        firstColumn.addFlexChild(
                FidgetzButton.builder()
                        .setMessage(OPEN_FOLDER_TEXT)
                        .setTooltip(Tooltip.create(Component.translatable("pack.folderInfo")))
                        .setOnPress(this.repository::openDir)
                        .build()
        );

        for (var element : elements.getOrDefault(ScreenEvent.InitLayout.Phase.AFTER_FOOTER_LEFT, Collections.emptyList())) {
            firstColumn.addChild(element);
        }

        for (var element : elements.getOrDefault(ScreenEvent.InitLayout.Phase.BEFORE_FOOTER_RIGHT, Collections.emptyList())) {
            secondColumn.addChild(element);
        }

        if (this.original.packType() == PackType.CLIENT_RESOURCES) {
            secondColumn.addFlexChild(FidgetzButton.builder().setMessage(ResourceUtil.getText("apply")).setOnPress(this::commit).build());
        }

        for (var element : elements.getOrDefault(ScreenEvent.InitLayout.Phase.BETWEEN_FOOTER_RIGHT, Collections.emptyList())) {
            secondColumn.addChild(element);
        }

        secondColumn.addFlexChild(FidgetzButton.builder().setMessage(CommonComponents.GUI_DONE).setOnPress(this::onClose).build());

        for (var element : elements.getOrDefault(ScreenEvent.InitLayout.Phase.AFTER_FOOTER, Collections.emptyList())) {
            secondColumn.addChild(element);
        }

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

    @Override
    protected void rebuildWidgets() {
        PackedPacksScreen screen;
        Profile profile = this.profiles.getProfile();

        if (profile != null) {
            profile.setPacks(this.currentPacks.list().copyPacks());
            screen = new PackedPacksScreen(this.previous, this.original, profile);
        } else {
            PackGroup packs = PackGroup.of(this.currentPacks.list().copyPacks(), this.availablePacks.list().copyPacks());
            screen = new PackedPacksScreen(this.previous, this.original, packs);
        }

        this.minecraft.setScreen(screen);
    }

    @Override
    public void onFilesDrop(@NonNull List<Path> packs) {
        this.minecraft.setScreen(new ConfirmScreen(
                this.confirmFileDrop(packs),
                Component.translatable("pack.dropConfirm"),
                Component.literal(joinPackNames(packs))
        ));
    }

    private BooleanConsumer confirmFileDrop(List<Path> packs) {
        return confirmed -> {
            if (!confirmed) {
                this.minecraft.setScreen(this);
                return;
            }
            PathValidationResults results = validatePaths(packs);

            if (!results.symlinkWarnings().isEmpty()) {
                this.minecraft.setScreen(NoticeWithLinkScreen.createPackSymlinkWarningScreen(() -> this.minecraft.setScreen(this)));
                return;
            }
            if (!results.valid().isEmpty()) {
                PackSelectionScreen.copyPacks(this.minecraft, results.valid(), this.original.packDir());
                this.refreshPacks();
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
        };
    }

    private void setOriginalScreen() {
        if (this.previous instanceof PackSelectionScreen) {
            this.onClose();
        } else {
            this.minecraft.setScreen(this.original.createScreen(this.previous));
        }
    }

    @Override
    public void onClose() {
        var closingEvent = new ScreenEvent.Closing(this.context);

        if (closingEvent.isCommited() || !(this.options.getUserConfig() instanceof Config.ResourcePacks resourceConfig) || resourceConfig.isApplyOnClose()) {
            this.commit();
        }

        if (this.original.packType() == PackType.SERVER_DATA && !(this.previous instanceof PackSelectionScreen)) {
            this.original.output().accept(this.repository.getRepository()); // validate datapacks
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
        if (this.watcher != null) {
            this.watcher.poll();
        }
    }

    private void createWatcher() {
        if (this.watcher == null) {
            this.watcherFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    List<Path> paths = new ObjectArrayList<>(this.additionalFolders.size() + 1);
                    paths.add(this.repository.getBaseDir());
                    paths.addAll(this.additionalFolders);
                    return new PackWatcher(this.context, paths, this::refreshPacks);
                } catch (Exception e) {
                    PackedPacks.LOGGER.error("[packed_packs] Failed to initialize pack directory watcher.", e);
                    return null;
                }
            }, Util.backgroundExecutor()).thenAcceptAsync(watcher -> {
                if (watcher != null) {
                    this.watcher = watcher;
                } else {
                    this.closeWatcher();
                }
            }, this.minecraft);
        }
    }

    private void closeWatcher() {
        if (this.watcherFuture != null) {
            this.watcherFuture.cancel(true);
        }

        if (this.watcher != null) {
            this.watcher.close();
            this.watcher = null;
        }
    }

    private void initAdditionalFolders() {
        this.additionalFolders = CollectionsUtil.deduplicate(CollectionsUtil.addAll(
                mapValidDirectories(this.options.getUserConfig().getAdditionalFolders()),
                this.repository.getAdditionalDirs()
        ));
    }

    private void repositionLists() {
        this.availablePacks.setHeaderVisibility(this.showActionBar);
        this.currentPacks.setHeaderVisibility(this.showActionBar);
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        this.layout.setPosition(0, 0);
        this.dialogs.forEach(ToggleableDialog::repositionElements);
        this.contextMenu.setOpen(false);
        this.repositionLists();
    }

    public void toggleActionBar() {
        this.showActionBar = !this.showActionBar;
        Config.get().setShowActionBar(this.showActionBar);
        this.repositionLists();
    }

    public void commit() {
        this.currentPacks.getSearchField().setValue("");
        this.syncProfile(this.profiles.getProfile());
        this.repository.selectPacks(this.currentPacks.list().copyPacks());

        if (this.original.packType() == PackType.CLIENT_RESOURCES) {
            this.original.output().accept(this.repository.getRepository());
        }
    }

    private void replacePacks(PackList list, List<Pack> packs) {
        list.captureState().replaceAll(packs).restore();
    }

    private void revalidateFolder() {
        if (this.folderDialog.isOpen()) {
            FolderPack folderPack = this.folderDialog.getFolderPack();
            if (folderPack == null || this.repository.getFolderConfig(folderPack) == null) {
                this.folderDialog.setOpen(false);
            } else {
                this.replacePacks(this.folderDialog.root(), ImmutableList.copyOf(this.repository.getNestedPacks(folderPack)));
            }
        }
    }

    public void revalidatePacks() {
        PackList availableList = this.availablePacks.list();
        PackList currentList = this.currentPacks.list();
        PackGroup packs = this.repository.validatePacks(availableList.copyPacks(), currentList.copyPacks());
        this.assetManager.clearIconCache();
        this.replacePacks(availableList, packs.unselected());
        this.replacePacks(currentList, packs.selected());
        this.revalidateFolder();
        this.clearHistory();
    }

    public void refreshPacks() {
        this.refreshFuture = CompletableFuture.runAsync(this.repository::refresh, Util.backgroundExecutor())
                .thenRunAsync(this::revalidatePacks, this.minecraft);
    }

    public void useSelected() {
        PackGroup packs = this.repository.getPacksBySelected();
        this.availablePacks.list().reload(packs.unselected());
        this.currentPacks.list().reload(packs.selected());
        this.clearHistory();
    }

    public void onProfileChange(@Nullable Profile previous, @Nullable Profile current) {
        if (previous != null) {
            previous.setPacks(this.currentPacks.list().copyPacks());
            Profiles.save(this.original.packType(), previous);
        }

        boolean unlocked = current == null || !current.isLocked();
        this.availablePacks.getTransferButton().active = unlocked;
        this.currentPacks.getTransferButton().active = unlocked;
        this.availablePacks.getSearchField().setValueSilently("");
        this.currentPacks.getSearchField().setValueSilently("");
        this.availablePacks.list().search("");
        this.currentPacks.list().search("");

        if (current != null && !current.getPackIds().isEmpty()) {
            this.applyProfile(current);
        } else {
            this.useSelected();
        }
    }

    public void onProfileCopy(@Nullable Profile original, @NonNull Profile copy) {
        copy.setPacks(this.currentPacks.list().copyPacks());
    }

    private void applyProfile(@NonNull Profile profile) {
        List<Pack> available = this.availablePacks.list().copyPacks();
        List<Pack> current = this.repository.getPacksByFlattenedIds(profile.getPackIds());
        this.applyPacks(available, current);
    }

    private void applyPacks(List<Pack> available, List<Pack> current) {
        PackGroup packs = this.repository.validatePacks(available, current);
        this.availablePacks.list().reload(packs.unselected());
        this.currentPacks.list().reload(packs.selected());
        this.clearHistory();
    }

    public void syncProfile(@Nullable Profile profile) {
        if (profile != null) {
            profile.syncPacks(this.repository.getPacks(), this.currentPacks.list().copyPacks());
        }
    }

    @Override
    public boolean isUnlocked() {
        Profile profile = this.profiles.getProfile();
        return profile == null || !profile.isLocked();
    }

    @Override
    public @NonNull List<PackList> getPackLists() {
        return this.packLists;
    }

    @Override
    public @Nullable PackList getDestination(PackList source) {
        if (source == this.availablePacks.list()) {
            return this.currentPacks.list();
        } else if (source == this.currentPacks.list()) {
            return this.availablePacks.list();
        }
        return null;
    }

    @Override
    protected void transferFocus(PackList source, PackList destination) {
        super.transferFocus(source, destination);

        if (destination == currentPacks.list()) {
            currentPacks.list().scrollToLastSelected();
        }
    }

    private void onFolderOpen(FolderOpenEvent event) {
        this.folderDialog.root().reload(this.repository.getNestedPacks(event.opened()));
        this.folderDialog.updateFolder(event.target(), event.opened(), this.assetManager);
        this.folderDialog.setOpen(true);
    }

    private void onFolderClose(FolderCloseEvent event) {
        this.folderDialog.setOpen(false);

        FolderPack folderPack = event.folderPack();
        if (folderPack == null) return;

        Folder folder = this.repository.getFolderConfig(folderPack);
        if (folder != null && this.isUnlocked()) {
            if (folder.trySetPacks(this.repository.validateAndOrderNestedPacks(folderPack, event.target().copyPacks()))) {
                folderPack.saveConfig(folder);
            }
            this.focusList(ObjectsUtil.firstNonNullOrDefault(this.availablePacks.list(), this.folderDialog.getParent()));
        }
    }

    private void onFileRename(FileRenameEvent event) {
        if (this.folderDialog.isOpen()) {
            this.folderDialog.onRename(event.renamed(), event.newName());
        }
        this.refreshPacks();
    }

    @Override
    protected void handleMoveEvent(MoveEvent event) {
        if (event.target() != this.folderDialog.root()) {
            super.handleMoveEvent(event);
            return;
        }

        PackList.Entry entry = event.target().getEntry(event.trigger());
        if (entry != null) {
            this.focus(ComponentPath.path(entry, event.target(), this.folderDialog, this));
        } else {
            this.focus(ComponentPath.path(event.target(), this.folderDialog, this));
        }
    }

    private void onOpenAliases(PackAliasOpenEvent event) {
        Objects.requireNonNull(this.aliasModal, "aliasModal");
        this.aliasModal.clear();
        this.aliasModal.root().layout().editAliases(event.trigger(), this.aliasModal::closeModal);
        this.aliasModal.root().visitWidgets(this.aliasModal::addRenderableWidget);
        this.aliasModal.repositionElements();
        this.aliasModal.setOpen(true);
    }

    @Override
    public void onEvent(PackListEvent event) {
        super.onEvent(event);

        this.profiles.getSidebar().setOpen(false);
        this.contextMenu.setOpen(false);
        this.fileRenameModal.setOpen(false);
        ObjectsUtil.ifPresent(this.aliasModal, Modal::closeModal);

        boolean notFolderDialogEvent = event.target() != this.folderDialog.root();
        if (notFolderDialogEvent) this.folderDialog.setOpen(false);

        switch (event) {
            case FileDeleteEvent ignore -> this.revalidatePacks();
            case FileRenameOpenEvent(PackList target, Pack trigger) -> this.fileRenameModal.open(target, trigger);
            case FileRenameEvent e -> this.onFileRename(e);
            case FileRenameCloseEvent e -> this.focusList(e.target());
            case FolderOpenEvent e -> this.onFolderOpen(e);
            case FolderCloseEvent e -> this.onFolderClose(e);
            case PackAliasOpenEvent e -> this.onOpenAliases(e);
            default -> {
            }
        }

        if (this.isUnlocked() && event.pushToHistory() && notFolderDialogEvent) {
            this.history.push(this.captureState());
        }
    }

    @Override
    public ScreenContext ctx() {
        return this.context;
    }

    @Override
    public <T extends ScreenEvent & Event> void postApiEvent(T event) {
        PackedPacksApiImpl.getInstance().eventBus().post(event);
    }

    public @Nullable PackLayout getLayoutFromSelectedList() {
        return ObjectsUtil.firstNonNull(
                ObjectsUtil.pick(this.availablePacks, this.currentPacks, pl -> pl.list() == this.getFocused()),
                ObjectsUtil.pick(this.availablePacks, this.currentPacks, pl -> pl.list().isHovered()),
                ObjectsUtil.pick(this.availablePacks, this.currentPacks, pl -> pl.list().isFocused())
        );
    }

    public ToggleableEditBox<Void> focusSearchField(@NonNull PackLayout packLayout) {
        if (!this.showActionBar) this.toggleActionBar();
        ToggleableEditBox<Void> searchField = packLayout.getSearchField();
        this.focus(searchField);
        return searchField;
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent charEvent) {
        if (super.charTyped(charEvent)) {
            return true;
        }
        if (CollectionsUtil.anyMatch(this.dialogs, ToggleableDialog::isOpen)) {
            return false;
        }
        if (charEvent.codepoint() != KEY_SPACE && noModifiers(charEvent.modifiers())) {
            PackLayout packLayout = this.getLayoutFromSelectedList();
            if (packLayout != null && !packLayout.getSearchField().isFocused()) {
                return this.focusSearchField(packLayout).charTyped(charEvent);
            }
        }
        return false;
    }

    public void toggleDevMode() {
        Config.get().setDevMode(!Config.get().isDevMode());
        ToastUtil.onDevModeToggleToast(Config.get().isDevMode());
        this.rebuildWidgets();
    }

    public void switchDefaultProfile() {
        this.options.getDefaultProfile().ifPresent(profile -> {
            if (Objects.equals(this.profiles.getProfile(), profile)) {
                this.profiles.setProfile(null);
            } else {
                this.profiles.setProfile(profile);
            }
        });
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent keyEvent) {
        this.contextMenu.setOpen(false);

        if (isDeveloperMode(keyEvent)) {
            this.toggleDevMode();
            return true;
        }
        if (isSwitchDefaultProfile(keyEvent)) {
            this.switchDefaultProfile();
            return true;
        }
        if (isRefresh(keyEvent) && (this.refreshFuture == null || this.refreshFuture.isDone())) {
            this.refreshPacks();
            return true;
        }
        if (isOpenProfiles(keyEvent)) {
            this.profiles.getSidebar().toggle();
            return true;
        }
        if (super.keyPressed(keyEvent)) {
            return true;
        }
        if (isRedo(keyEvent) && this.isUnlocked()) {
            return this.history.redo();
        }
        if (isUndo(keyEvent) && this.isUnlocked()) {
            return this.history.undo();
        }
        if (isSelectAll(keyEvent)) {
            PackLayout packLayout = this.getLayoutFromSelectedList();
            if (packLayout != null) {
                packLayout.list().selectAll();
                this.onEvent(new SelectionEvent(packLayout.list()));
                return true;
            }
        }
        if (keyEvent.key() == KEY_BACKSPACE) {
            PackLayout packLayout = this.getLayoutFromSelectedList();
            if (packLayout != null) {
                ToggleableEditBox<Void> searchField = packLayout.getSearchField();
                if (!searchField.isFocused() && !searchField.getValue().isEmpty()) {
                    return this.focusSearchField(packLayout).keyPressed(keyEvent);
                }
            }
        }
        return false;
    }

    private boolean hasHeader(List<MenuItem> items) {
        return !items.isEmpty() && items.getFirst() instanceof PackMenuHeader;
    }

    private void openContextMenu(int mouseX, int mouseY) {
        if (this.contextMenu.isMouseOver(mouseX, mouseY)) return;

        Map<ScreenEvent.OpenCtxMenu.Phase, ContextMenuItemBuilder> builders = new EnumMap<>(ScreenEvent.OpenCtxMenu.Phase.class);
        this.postApiEvent(new ScreenEvent.OpenCtxMenu(this.context, phase -> builders.computeIfAbsent(phase, p -> new ContextMenuItemBuilder())));

        this.buildItems(mouseX, mouseY)
                .whenNonNull(builders.get(ScreenEvent.OpenCtxMenu.Phase.BEFORE_ALL))
                .ifTrue((extraBuilder, b) -> b.addAll(extraBuilder.build()))
                .when(Config.get().isDevMode())
                .ifTrue(dev -> dev.separatorIfNonEmpty()
                        .whenNonNull(this.profiles.getProfile())
                        .ifTrue((profile, b) -> b.
                                add(devItem(ResourceUtil.getText("profile.save"))
                                        .action(() -> profile.setPacks(this.currentPacks.list().copyPacks()))
                                        .build())
                                .separator())
                        .parent(children -> devItem(ResourceUtil.getText("preferences"))
                                .addChildren(children)
                                .build(), builder -> builder
                                .addAll(ToggleableHelper.preferences())
                                .whenNonNull(builders.get(ScreenEvent.OpenCtxMenu.Phase.PREFERENCES))
                                .ifTrue((extraBuilder, b) -> b.addAll(extraBuilder.build()))
                                .add(devItem(ResourceUtil.getText("preferences.reset"))
                                        .action(Preferences.INSTANCE::reset)
                                        .build()))
                )
                .separatorIfNonEmpty()
                .simpleItem(ResourceUtil.getText("reset_enabled"), this::isUnlocked, this::useSelected)
                .simpleItem(ResourceUtil.getText("refresh"), this::canRefresh, this::refreshPacks)
                .when(this.additionalFolders, List::isEmpty)
                .ifTrue(b -> b.simpleItem(OPEN_FOLDER_TEXT, this.repository::openDir))
                .orElse((dirs, b) -> b
                        .parent(OPEN_FOLDER_TEXT, p -> p
                                .add(new DirectoryMenuItem(this.repository.getBaseDir()))
                                .separator()
                                .iterate(dirs)
                                .map(DirectoryMenuItem::new)))
                .whenNonNull(builders.get(ScreenEvent.OpenCtxMenu.Phase.AFTER_ALL))
                .ifTrue((extraBuilder, b) -> b.addAll(extraBuilder.build()))
                .peek(items -> {
                    int yOffset = this.hasHeader(items) ? this.contextMenu.getItemHeight() : 0;
                    this.contextMenu.open(mouseX, mouseY - yOffset, items);
                });
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent mouseEvent, boolean doubleClicked) {
        this.setDragged(null);
        if (isRightClick(mouseEvent) && !this.optionsModal.isMouseOver(mouseEvent.x(), mouseEvent.y())) {
            this.openContextMenu((int) mouseEvent.x(), (int) mouseEvent.y());
            return true;
        }
        if (ToggleableDialogContainer.super.mouseClicked(mouseEvent, doubleClicked)) {
            return true;
        }
        if (isClickForward(mouseEvent) && this.isUnlocked()) {
            return this.history.redo();
        }
        if (isClickBack(mouseEvent) && this.isUnlocked()) {
            return this.history.undo();
        }
        if (isLeftClick(mouseEvent) && !(this.getFocused() instanceof PackList)) {
            this.setFocused(this.children().getFirst());
            this.layout.visitWidgets(w -> w.setFocused(false));
        }
        this.contextMenu.setOpen(false);
        return false;
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

        if (Config.get().isDevMode()) {
            float scale = 0.5f;
            int y = (int) ((height - this.font.lineHeight * scale) / scale);

            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().scale(scale);
            guiGraphics.drawString(this.font, ResourceUtil.getText("dev_mode", DEV_MODE_SHORTCUT), 0, y, Theme.WHITE.getARGB());
            guiGraphics.pose().popMatrix();
        }
    }

    public boolean canRefresh() {
        return this.refreshFuture == null || this.refreshFuture.isDone();
    }

    public void clearHistory() {
        this.history.reset(this.captureState());
    }

    public void recordState(String eventName) {
        this.history.push(this.captureState(eventName));
    }

    @Override
    public @NonNull Snapshot captureState(String eventName) {
        return new Snapshot(this, this.availablePacks.list().captureState(), this.currentPacks.list().captureState());
    }

    @Override
    public void replaceState(@NonNull Snapshot snapshot) {
        Set<Pack> validPacks = new ObjectOpenHashSet<>(this.repository.getPacks());
        Query availablePacksQuery = snapshot.availablePacks.model().query();
        this.availablePacks.getSortButton().setValueSilently(availablePacksQuery.sort());
        this.availablePacks.getCompatButton().setValueSilently(availablePacksQuery.hideIncompatible());
        this.availablePacks.getSearchField().setValueSilently(availablePacksQuery.unmodifiedSearch());
        this.currentPacks.getSearchField().setValueSilently(snapshot.currentPacks().model().query().unmodifiedSearch());
        snapshot.availablePacks.retainAll(validPacks).restore();
        snapshot.currentPacks.retainAll(validPacks).restore();
        this.availablePacks.list().scrollToLastSelected();
        this.currentPacks.list().scrollToLastSelected();
    }

    public record Snapshot(
            PackedPacksScreen target,
            PackList.Snapshot availablePacks,
            PackList.Snapshot currentPacks
    ) implements Restorable.Snapshot<Snapshot> {
    }
}
