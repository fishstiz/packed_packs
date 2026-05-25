package io.github.fishstiz.packed_packs.gui.components;

import io.github.fishstiz.fidgetz.v0.gui.components.*;
import io.github.fishstiz.fidgetz.v0.gui.components.ContainerEventHandlerPatch;
import io.github.fishstiz.fidgetz.v0.gui.layouts.*;
import io.github.fishstiz.fidgetz.v0.gui.renderables.Renderables;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.gui.FocusPathProvider;
import io.github.fishstiz.packed_packs.gui.FocusTarget;
import io.github.fishstiz.packed_packs.gui.model.PackListUtils;
import io.github.fishstiz.packed_packs.gui.model.PackListViewModel;
import io.github.fishstiz.packed_packs.gui.states.ActiveAction;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.packed_packs.util.Colors;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static io.github.fishstiz.packed_packs.util.GuiUtils.*;

public class PackListContainer extends AbstractWidget implements FocusPathProvider, Layout, ContainerEventHandlerPatch {
    private final @Nullable PackListContainer root;
    private final ScreenContext screenContext;
    private final PackListViewModel viewModel;
    private final PackList packList;
    private PackListContainer.@Nullable Folder folder;
    private @Nullable GuiEventListener focused;
    private boolean dragging;
    private List<GuiEventListener> children;

    private PackListContainer(@Nullable PackListContainer root, ScreenContext screenContext, PackListViewModel viewModel) {
        super(0, 0, 0, 0, CommonComponents.EMPTY);
        this.root = root;
        this.screenContext = screenContext;
        this.viewModel = viewModel;
        this.packList = new PackList(screenContext, viewModel);
        this.children = List.of(this.packList);
        this.viewModel.subscribe(PackListViewModel.Property.FOLDER, this::refresh);
    }

    private PackListContainer(PackListContainer root, PackListViewModel.Module viewModel) {
        this(root, root.screenContext, viewModel);
    }

    public PackListContainer(ScreenContext screenContext, PackListViewModel viewModel) {
        this(null, screenContext, viewModel);
    }

    private void updateState(GuiEventListener target) {
        this.children = List.of(target);
        setFocused(target);
    }

    private void refresh() {
        boolean isOpened = viewModel.isFolderOpened();
        if (isOpened == (this.folder != null)) {
            return;
        }

        if (isOpened) {
            Folder newFolder = new Folder(root == null ? this : root, viewModel.createFolderSlice());
            this.folder = newFolder;
            updateState(newFolder);
            ComponentPath path = newFolder.nextFocusPath(new FocusNavigationEvent.InitialFocus());
            if (path != null) path.applyFocus(true);
        } else {
            this.folder = null;
            updateState(packList);
        }
    }

    public boolean renderDroppableZone(ActiveAction.Dragging dragging, GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.folder != null) {
            return this.folder.listContainer.renderDroppableZone(dragging, guiGraphics, mouseX, mouseY, partialTick);
        }
        packList.renderDroppableZone(guiGraphics, dragging, mouseX, mouseY, partialTick);
        if (packList.isMouseOver(mouseX, mouseY)) {
            return dragging.target() == viewModel.key() || PackListUtils.canInteract(dragging.target(), viewModel.key());
        }
        return false;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        packList.render(graphics, mouseX, mouseY, partialTick);
        if (this.folder != null) {
            this.folder.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        if (this.folder != null) {
            this.folder.listContainer.updateNarration(output.nest());
        } else {
            packList.updateNarration(output.nest());
        }
    }

    public void visitDeepestList(Consumer<PackList> visitor) {
        if (viewModel.isFolderOpened() && this.folder != null) {
            this.folder.listContainer.visitDeepestList(visitor);
        } else {
            visitor.accept(packList);
        }
    }

    public void onDrop(ActiveAction.Dragging dragging, int mouseX, int mouseY) {
        this.visitDeepestList(list -> list.onDrop(dragging, mouseX, mouseY));
    }

    public PackListViewModel model() {
        return viewModel;
    }

    @Override
    public List<GuiEventListener> children() {
        return this.children;
    }

    @Override
    public boolean isDragging() {
        return this.dragging;
    }

    @Override
    public void setDragging(boolean isDragging) {
        this.dragging = isDragging;
    }

    @Override
    public @Nullable GuiEventListener getFocused() {
        return this.focused;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            setFocused(null);
        } else {
            setFocused(this.folder == null ? packList : this.folder);
        }
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        if (this.focused != focused) {
            if (this.focused != null) {
                this.focused.setFocused(false);
            }
            if (focused != null) {
                focused.setFocused(true);
            }
            this.focused = focused;
        }
    }

    @Override
    public @Nullable ComponentPath getFocusPath(FocusTarget target) {
        FocusPathProvider child = this.folder != null ? this.folder : packList;
        return ComponentPath.path(this, child.getFocusPath(target));
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
        GuiEventListener child = this.folder != null ? this.folder : packList;
        return ComponentPath.path(this, child.nextFocusPath(event));
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        packList.setHeight(height);
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        packList.setWidth(width);
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        packList.setX(x);
        repositionFolder();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        packList.setY(y);
        repositionFolder();
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        setWidth(width);
        setHeight(height);
    }

    @Override
    public void setPosition(int x, int y) {
        super.setPosition(x, y);
        setX(x);
        setY(y);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (root != null && viewModel.isFolderOpened() && this.folder != null) {
            return root.isMouseOver(mouseX, mouseY);
        }
        return super.isMouseOver(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return ContainerEventHandlerPatch.super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return ContainerEventHandlerPatch.super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return ContainerEventHandlerPatch.super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void visitChildren(Consumer<LayoutElement> visitor) {
        visitor.accept(this);
    }

    @Override
    public void arrangeElements() {
        packList.initializeEntries();
        if (this.folder != null) this.folder.arrangeElements();
    }

    private void repositionFolder() {
        if (this.folder != null) this.folder.repositionElements();
    }

    static class Folder extends AbstractContainerEventHandler implements FocusPathProvider, FZContextMenu.Source, ContainerEventHandlerPatch, Renderable {
        private static final int HEADER_SIZE = 16;
        private static final int LAYOUT_SPACING = SPACING / 2;
        private final PackListContainer root;
        private final PackListViewModel.Module moduleModel;
        private final FZIcon background;
        private final PackListContainer listContainer;
        private final FZIconButton closeButton;
        private final FZIcon folderIcon;
        private final FZText folderTitle;
        private final FZLayout layout;
        private final Runnable unsubscribe;
        private List<GuiEventListener> children = Collections.emptyList();
        private List<Renderable> renderables = Collections.emptyList();

        Folder(PackListContainer root, PackListViewModel.Module moduleModel) {
            this.root = root;
            this.moduleModel = moduleModel;
            this.listContainer = new PackListContainer(root, moduleModel);
            this.background = FZIcon.builder(ResourceLocation.withDefaultNamespace("popup/background")).build();
            this.closeButton = FZIconButton.builder()
                    .size(HEADER_SIZE, HEADER_SIZE)
                    .icon(new WidgetElements(PackedPacks.id("icon/cross"), 16, 16))
                    .onPress(moduleModel::close)
                    .build();
            this.folderIcon = FZIcon.builder(Renderables.texture(moduleModel.icon(), 16, 16))
                    .size(HEADER_SIZE, HEADER_SIZE)
                    .build();
            this.folderTitle = FZText.builder(Objects.requireNonNull(moduleModel.currentFolder()).getTitle())
                    .height(HEADER_SIZE)
                    .build();

            FZFlexLayout section = FZFlexLayout.vertical(root).spacing(LAYOUT_SPACING);
            {
                FZFlexLayout header = section.child(FZFlexLayout.horizontal(), section.flexChildHorizontalSettings());
                {
                    header.spacing(LAYOUT_SPACING).alignContents(Justification.CENTER);
                    header.child(closeButton);
                    header.child(folderIcon);
                    header.child(folderTitle, header.flexChildHorizontalSettings());
                }
                section.child(listContainer, section.flexChildSettings());
            }

            this.layout = FZComposedLayout.compose(section).padding(SPACING).clamp((LayoutElement) root);

            Runnable unsubscribeCurrentFolder = moduleModel.subscribeToCurrentFolder(this::refresh);
            Runnable unsubscribeChildFolder = moduleModel.subscribe(PackListViewModel.Property.FOLDER, this::refreshChild);

            this.unsubscribe = () -> {
                unsubscribeCurrentFolder.run();
                unsubscribeChildFolder.run();
            };

            refresh();
            arrangeElements();
            refreshChild();
        }

        private void refresh() {
            FolderPack folderPack = moduleModel.currentFolder();
            if (folderPack == null) {
                unsubscribe.run();
                children = Collections.emptyList();
                renderables = Collections.emptyList();
            }
        }

        private void refreshChild() {
            if (!moduleModel.isFolderOpened()) {
                this.renderables = List.of(background, folderIcon, folderTitle, closeButton, listContainer);
                this.children = List.of(closeButton, listContainer);
            } else {
                this.renderables = List.of(listContainer);
                this.children = List.of(listContainer);
            }
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            for (Renderable renderable : this.renderables) {
                renderable.render(graphics, mouseX, mouseY, partialTick);
            }
        }

        @Override
        public void fidgetz$updateContextEntries(double x, double y, FZContextMenu.Collector collector) {
            FolderPack folderPack = moduleModel.currentFolder();
            if (folderPack == null) {
                FZContextMenu.Source.super.fidgetz$updateContextEntries(x, y, collector);
                return;
            }

            collector.addEntry(builder -> builder
                    .message(folderPack.getTitle())
                    .icon(padded16Rect(Renderables.texture(moduleModel.icon(), 32, 32)))
                    .background(Renderables.fill(Colors.GRAY_500))
                    .onPress(e -> {
                        e.context().closeMenu();
                        return false;
                    })
                    .allowAutoDivideAfterEntry(false));

            collector.addEntry(builder -> builder
                    .message(CommonComponents.GUI_BACK.copy().append(CommonComponents.ELLIPSIS))
                    .onPress(moduleModel::close));

            if (!listContainer.packList.isHovered()) {
                if (moduleModel.fileModifiable()) {
                    collector.addEntry(builder -> builder
                            .message(RENAME_FILE_TEXT)
                            .active(moduleModel::fileModifiable)
                            .onPress(moduleModel::openRename));
                    collector.addEntry(builder -> builder
                            .message(DELETE_FILE_TEXT)
                            .active(moduleModel::fileModifiable)
                            .onPress(moduleModel::delete));
                }
                if (PackUtil.validatePackPath(folderPack) != null) {
                    collector.addEntry(builder -> builder.message(OPEN_FILE_TEXT).onPress(() -> PackUtil.openPack(folderPack)));
                    collector.addEntry(builder -> builder.message(OPEN_PARENT_TEXT).onPress(() -> PackUtil.openParent(folderPack)));
                }
            }

            FZContextMenu.Source.super.fidgetz$updateContextEntries(x, y, collector);
        }

        void repositionElements() {
            layout.setPosition(root.getX(), root.getY());
            background.setPosition(root.getX(), root.getY());
        }

        void arrangeElements() {
            if (layout.getWidth() != root.getWidth() || layout.getHeight() != root.getHeight()) {
                layout.arrangeElements();
                layout.fidgetz$setSize(root.getWidth(), root.getHeight());
                background.setSize(root.getWidth(), root.getHeight());
                repositionElements();
            }
        }

        @Override
        public @Nullable ComponentPath getFocusPath(FocusTarget target) {
            return ComponentPath.path(this, listContainer.getFocusPath(target));
        }

        @Override
        public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
            if (moduleModel.isFolderOpened()) {
                return ComponentPath.path(this, listContainer.nextFocusPath(event));
            }

            if (event instanceof FocusNavigationEvent.InitialFocus) {
                ComponentPath path = listContainer.nextFocusPath(event);
                return path == null ? ComponentPath.path(closeButton, this) : ComponentPath.path(this, path);
            }

            if (!isFocused() &&
                event instanceof FocusNavigationEvent.ArrowNavigation(ScreenDirection direction) &&
                direction.getAxis() == ScreenAxis.HORIZONTAL) {
                ComponentPath path = listContainer.nextFocusPath(event);
                if (path != null) {
                    return ComponentPath.path(this, path);
                }
            }

            return super.nextFocusPath(event);
        }

        @Override
        public void setFocused(boolean isFocused) {
            if (!isFocused) {
                setFocused(null);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return ContainerEventHandlerPatch.super.mouseClicked(mouseX, mouseY, button) && moduleModel.isOpened();
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return getRectangle().containsPoint((int) mouseX, (int) mouseY);
        }

        @Override
        public List<GuiEventListener> children() {
            return children;
        }

        @Override
        public ScreenRectangle getRectangle() {
            return layout.getRectangle();
        }
    }
}
