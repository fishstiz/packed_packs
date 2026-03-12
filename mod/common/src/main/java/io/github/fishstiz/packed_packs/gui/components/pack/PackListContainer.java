package io.github.fishstiz.packed_packs.gui.components.pack;

import io.github.fishstiz.fidgetz.gui.components.*;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuContainer;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.fidgetz.util.DrawUtil;
import io.github.fishstiz.fidgetz.util.GuiUtil;
import io.github.fishstiz.fidgetz.util.lang.ObjectsUtil;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.gui.FocusPathProvider;
import io.github.fishstiz.packed_packs.gui.FocusTarget;
import io.github.fishstiz.packed_packs.gui.components.contextmenu.PackMenuHeader;
import io.github.fishstiz.packed_packs.gui.model.PackListUtils;
import io.github.fishstiz.packed_packs.gui.model.PackListViewModel;
import io.github.fishstiz.packed_packs.gui.states.ActiveAction;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import io.github.fishstiz.packed_packs.transform.interfaces.FilePack;
import io.github.fishstiz.packed_packs.util.PackUtil;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.*;
import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.DELETE_FILE_TEXT;
import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.OPEN_FILE_TEXT;
import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.OPEN_PARENT_TEXT;

public class PackListContainer extends AbstractWidget implements FocusPathProvider, Layout, ContextMenuContainer, ContainerEventHandlerPatch {
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
        this.viewModel.subscribe(PackListViewModel.Property.FOLDER, this::refresh);
        this.children = List.of(this.packList);
        this.refresh();
    }

    private PackListContainer(PackListContainer root, PackListViewModel.Module viewModel) {
        this(root, root.screenContext, viewModel);
    }

    public PackListContainer(ScreenContext screenContext, PackListViewModel viewModel) {
        this(null, screenContext, viewModel);
    }

    private void updateState(GuiEventListener target) {
        this.children = List.of(target);
        this.setFocused(target);
        ComponentPath path = target.nextFocusPath(new FocusNavigationEvent.InitialFocus());
        if (path != null) {
            path.applyFocus(true);
        }
    }

    private void refresh() {
        boolean isOpened = this.viewModel.isFolderOpened();
        if (isOpened == (this.folder != null)) {
            return;
        }

        if (isOpened) {
            Folder newFolder = new Folder(this.root == null ? this : this.root, this.viewModel.createFolderSlice());
            this.folder = newFolder;
            this.updateState(newFolder);
        } else {
            this.folder = null;
            this.updateState(this.packList);
        }
    }

    public boolean renderDroppableZone(ActiveAction.Dragging dragging, GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.folder != null) {
            return this.folder.listContainer.renderDroppableZone(dragging, guiGraphics, mouseX, mouseY, partialTick);
        }
        this.packList.renderDroppableZone(guiGraphics, dragging, mouseX, mouseY, partialTick);
        if (this.packList.isMouseOver(mouseX, mouseY)) {
            return dragging.target() == this.packList.key() || PackListUtils.canInteract(dragging.target(), this.packList.key());
        }
        return false;
    }

    @Override
    protected void renderWidget(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.packList.render(guiGraphics, mouseX, mouseY, partialTick);
        if (this.folder != null) {
            this.folder.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    protected void updateWidgetNarration(@NonNull NarrationElementOutput narrationElementOutput) {
        if (this.folder != null) {
            this.folder.listContainer.updateNarration(narrationElementOutput.nest());
        } else {
            this.packList.updateNarration(narrationElementOutput.nest());
        }
    }

    public void visitPackLists(Consumer<PackList> visitor) {
        visitor.accept(this.packList);
        if (this.viewModel.isFolderOpened() && this.folder != null) {
            this.folder.listContainer.visitPackLists(visitor);
        }
    }

    public void visitDeepestList(Consumer<PackList> visitor) {
        if (this.viewModel.isFolderOpened() && this.folder != null) {
            this.folder.listContainer.visitDeepestList(visitor);
        } else {
            visitor.accept(this.packList);
        }
    }

    public void onDrop(ActiveAction.Dragging dragging, int mouseX, int mouseY) {
        this.visitDeepestList(list -> list.onDrop(dragging, mouseX, mouseY));
    }

    @Override
    public @NonNull List<GuiEventListener> children() {
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
            this.setFocused(null);
        } else {
            this.setFocused(this.folder == null ? this.packList : this.folder);
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
        FocusPathProvider child = this.folder != null ? this.folder : this.packList;
        return ComponentPath.path(this, child.getFocusPath(target));
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(@NonNull FocusNavigationEvent event) {
        GuiEventListener child = this.folder != null ? this.folder : this.packList;
        return ComponentPath.path(this, child.nextFocusPath(event));
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        this.packList.setHeight(height);
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        this.packList.setWidth(width);
    }

    @Override
    public int getX() {
        return this.packList.getX();
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        this.packList.setX(x);
    }

    @Override
    public int getY() {
        return this.packList.getY();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        this.packList.setY(y);
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        this.setWidth(width);
        this.setHeight(height);
    }

    @Override
    public void setPosition(int x, int y) {
        super.setPosition(x, y);
        this.setX(x);
        this.setY(y);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
        return ContainerEventHandlerPatch.super.mouseClicked(mouseButtonEvent, doubleClicked);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent mouseButtonEvent) {
        return ContainerEventHandlerPatch.super.mouseReleased(mouseButtonEvent);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
        return ContainerEventHandlerPatch.super.mouseDragged(mouseButtonEvent, dragX, dragY);
    }

    @Override
    public void visitChildren(Consumer<LayoutElement> visitor) {
        visitor.accept(this);
    }

    @Override
    public void arrangeElements() {
        if (this.folder != null) this.folder.arrangeElements();
    }

    int getMaxFolderWidth() {
        return this.getWidth() - SPACING * 2;
    }

    int getMaxFolderHeight() {
        return this.getHeight() - SPACING * 2;
    }

    static class Folder extends AbstractContainerEventHandler implements FocusPathProvider, ContextMenuContainer, ContainerEventHandlerPatch, Renderable {
        private static final Component BACK_TEXT = CommonComponents.GUI_BACK.copy().append(CommonComponents.ELLIPSIS);
        private final PackListContainer root;
        private final RenderableRectWidget<Void> background;
        private final LayoutWrapper<FlexLayout> layout;
        private final FidgetzButton<Void> closeButton;
        private final FidgetzText<Void> folderTitle;
        private final RenderableRectWidget<Void> folderIcon;
        private final PackListViewModel.Module viewModel;
        private final PackListContainer listContainer;
        private final Runnable unsubscribe;
        private List<GuiEventListener> children = Collections.emptyList();
        private List<Renderable> renderables = Collections.emptyList();

        Folder(PackListContainer root, PackListViewModel.Module viewModel) {
            this.root = root;
            this.viewModel = viewModel;

            this.background = RenderableRectWidget.<Void>builder(DrawUtil.DEMO_BACKGROUND).build();
            this.folderIcon = RenderableRectWidget.<Void>builder(this.viewModel.sprite()).makeSquare().build();
            this.folderTitle = FidgetzText.<Void>builder().setOffsetY(1).build();
            this.closeButton = FidgetzButton.<Void>builder()
                    .makeSquare()
                    .setSprite(CROSS_SPRITE)
                    .setOnPress(this.viewModel::close)
                    .build();
            this.listContainer = new PackListContainer(root, viewModel);

            FlexLayout header = FlexLayout.horizontal(this.root::getMaxFolderWidth).spacing(SPACING);
            header.addChild(this.folderIcon);
            header.addFlexChild(this.folderTitle);
            header.addChild(this.closeButton);

            FlexLayout contents = FlexLayout.horizontal(this.root::getMaxFolderWidth).spacing(SPACING);
            contents.addFlexChild(this.listContainer, true);

            FlexLayout body = FlexLayout.vertical(this.root::getMaxFolderHeight).spacing(SPACING);
            body.addChild(header, LayoutSettings.defaults().paddingBottom(-(SPACING / 2)));
            body.addFlexChild(contents);

            this.layout = new LayoutWrapper<>(body);
            this.layout.setPadding(SPACING);

            this.refresh();
            this.arrangeElements();
            this.refreshChild();

            Runnable unsubscribeCurrentFolder = this.viewModel.subscribeToCurrentFolder(this::refresh);
            Runnable unsubscribeChildFolder = this.viewModel.subscribe(PackListViewModel.Property.FOLDER, this::refreshChild);

            this.unsubscribe = () -> {
                unsubscribeCurrentFolder.run();
                unsubscribeChildFolder.run();
            };
        }

        private void refresh() {
            FolderPack folderPack = this.viewModel.currentFolder();

            if (folderPack == null) {
                this.unsubscribe.run();
                this.children = Collections.emptyList();
                this.renderables = Collections.emptyList();
                return;
            }

            this.folderTitle.setMessage(folderPack.getTitle());
            this.folderIcon.setRenderableRect(this.viewModel.sprite());
        }

        private void refreshChild() {
            if (!this.viewModel.isFolderOpened()) {
                this.renderables = List.of(this.background, this.folderIcon, this.folderTitle, this.closeButton, this.listContainer);
                this.children = List.of(this.closeButton, this.listContainer);
            } else {
                this.renderables = List.of(this.listContainer);
                this.children = List.of(this.listContainer);
            }
        }

        @Override
        public void render(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            for (Renderable renderable : this.renderables) {
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }

        @Override
        public void buildItems(ContextMenuItemBuilder builder, int mouseX, int mouseY) {
            ContextMenuContainer.super.buildItems(
                    builder.whenNonNull(this.viewModel.currentFolder())
                            .ifTrue((folderPack, folderMenuBuilder) -> folderMenuBuilder
                                    .add(new PackMenuHeader(folderPack, this.viewModel.sprite()))
                                    .simpleItem(BACK_TEXT, this.viewModel::close)
                                    .when(this.getChildAt(mouseX, mouseY).isEmpty())
                                    .ifTrue(b -> b
                                            .whenNonNull(ObjectsUtil.mapOrNull(folderPack, FilePack::packed_packs$getPath))
                                            .ifTrue((path, operationsMenuBuilder) -> operationsMenuBuilder
                                                    .separator()
                                                    .simpleItem(RENAME_FILE_TEXT, this.viewModel::fileModifiable, this.viewModel::openRename)
                                                    .simpleItem(DELETE_FILE_TEXT, this.viewModel::fileModifiable, this.viewModel::delete)
                                                    .simpleItem(OPEN_FILE_TEXT, () -> PackUtil.openPack(folderPack))
                                                    .simpleItem(OPEN_PARENT_TEXT, () -> PackUtil.openParent(folderPack))
                                            )
                                    )
                            ),
                    mouseX,
                    mouseY
            );
        }

        void arrangeElements() {
            this.layout.arrangeElements();
            this.layout.setPosition(this.root.getX(), this.root.getY());
            this.background.setPosition(this.root.getX(), this.root.getY());
            this.background.setSize(this.root.getWidth(), this.root.getHeight());
        }

        @Override
        public @Nullable ComponentPath getFocusPath(FocusTarget target) {
            return ComponentPath.path(this, this.listContainer.getFocusPath(target));
        }

        @Override
        public @Nullable ComponentPath nextFocusPath(@NonNull FocusNavigationEvent event) {
            if (this.viewModel.isFolderOpened()) {
                return ComponentPath.path(this, this.listContainer.nextFocusPath(event));
            }

            if (event instanceof FocusNavigationEvent.InitialFocus) {
                ComponentPath path = this.listContainer.nextFocusPath(event);
                return path == null ? ComponentPath.path(this.closeButton, this) : ComponentPath.path(this, path);
            }

            if (!this.isFocused() &&
                event instanceof FocusNavigationEvent.ArrowNavigation(ScreenDirection direction) &&
                direction.getAxis() == ScreenAxis.HORIZONTAL) {
                ComponentPath path = this.listContainer.nextFocusPath(event);
                if (path != null) {
                    return ComponentPath.path(this, path);
                }
            }

            return super.nextFocusPath(event);
        }

        @Override
        public void setFocused(boolean isFocused) {
            if (!isFocused) {
                this.setFocused(null);
            }
        }

        @Override
        public boolean mouseClicked(@NonNull MouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
            return ContainerEventHandlerPatch.super.mouseClicked(mouseButtonEvent, doubleClicked) && this.viewModel.isOpened();
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return GuiUtil.containsPoint(this.layout, mouseX, mouseY);
        }

        @Override
        public @NonNull List<GuiEventListener> children() {
            return this.children;
        }

        @Override
        public @NonNull ScreenRectangle getRectangle() {
            return this.layout.getRectangle();
        }
    }
}
