package io.github.fishstiz.packed_packs.gui2.components;

import com.mojang.blaze3d.platform.cursor.CursorType;
import io.github.fishstiz.fidgetz.v0.gui.components.*;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZComposedLayout;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZFlexLayout;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZLayout;
import io.github.fishstiz.fidgetz.v0.gui.layouts.Justification;
import io.github.fishstiz.fidgetz.v0.gui.renderables.Renderables;
import io.github.fishstiz.fidgetz.v0.gui.state.FZRef;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.gui.ContainerEventHandlerPatch;
import io.github.fishstiz.packed_packs.gui.FocusPathProvider;
import io.github.fishstiz.packed_packs.gui.FocusTarget;
import io.github.fishstiz.packed_packs.gui.model.PackListKey;
import io.github.fishstiz.packed_packs.gui.model.PackListType;
import io.github.fishstiz.packed_packs.gui.model.PackListUtils;
import io.github.fishstiz.packed_packs.gui.states.ActiveAction;
import io.github.fishstiz.packed_packs.gui.states.PackListState;
import io.github.fishstiz.packed_packs.gui.states.PackedPacksState;
import io.github.fishstiz.packed_packs.gui.states.ProfilesState;
import io.github.fishstiz.packed_packs.gui2.intents.PackListIntent;
import io.github.fishstiz.packed_packs.gui2.models.PackEntry;
import io.github.fishstiz.packed_packs.gui2.services.PackIconCache;
import io.github.fishstiz.packed_packs.gui2.services.PackResourcesService;
import io.github.fishstiz.packed_packs.impl.context.ScreenContextImpl;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import io.github.fishstiz.packed_packs.util.Colors;
import io.github.fishstiz.packed_packs.util.GuiUtils;
import io.github.fishstiz.packed_packs.util.PackUtil;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static io.github.fishstiz.packed_packs.util.GuiUtils.*;

public class PackListContainer extends AbstractWidget implements FocusPathProvider, Layout, ContainerEventHandlerPatch {
    private final PackListKey key;
    private final @Nullable PackListContainer root;
    private final ScreenContextImpl screenContext;
    private final PackResourcesService resourcesService;
    private final PackList packList;
    private @Nullable Folder folder;
    private @Nullable GuiEventListener focused;
    private boolean dragging;
    private List<GuiEventListener> children;
    private PackListState listState = PackListState.empty();
    private ProfilesState profilesState = ProfilesState.empty();

    private PackListContainer(
            @Nullable PackListContainer root,
            ScreenContextImpl screenContext,
            PackResourcesService resourcesService,
            PackListKey key
    ) {
        super(0, 0, 0, 0, CommonComponents.EMPTY);
        this.key = key;
        this.root = root;
        this.screenContext = screenContext;
        this.resourcesService = resourcesService;
        this.packList = new PackList(screenContext, resourcesService, key);
        this.children = List.of(this.packList);
    }

    private PackListContainer(PackListContainer root, PackResourcesService resourcesService, PackListKey key) {
        this(root, root.screenContext, resourcesService, key);
    }

    public PackListContainer(
            ScreenContextImpl screenContext,
            PackResourcesService resourcesService,
            PackListType type,
            FZRef<PackedPacksState> state
    ) {
        this(null, screenContext, resourcesService, PackListKey.root(type));
        state.subscribe(key.toString(), value -> this.onStateChanged(value.rootTargetList(type), value.profiles()));
    }

    private void updateChild(GuiEventListener target) {
        this.children = List.of(target);
        if (getFocused() != null) {
            setFocused(target);
        }
    }

    private void onStateChanged(PackListState listState, ProfilesState profilesState) {
        if (listState == this.listState && profilesState == this.profilesState) {
            return;
        }

        this.listState = listState;
        this.profilesState = profilesState;

        PackListState.Folder folderState = listState.folder();
        if ((folderState == null) != (this.folder == null)) {
            if (folderState != null) {
                Folder newFolder = new Folder(root == null ? this : root, resourcesService, key.nest(), folderState.pack());
                this.folder = newFolder;
                updateChild(newFolder);
                ComponentPath path = newFolder.nextFocusPath(new FocusNavigationEvent.InitialFocus());
                if (path != null) path.applyFocus(true);
            } else {
                this.folder = null;
                updateChild(packList);
            }
        }

        packList.onStateChanged(listState, profilesState);

        if (folderState != null) {
            this.folder.onStateChanged(folderState, profilesState);
        }
    }

    public boolean renderDroppableZone(ActiveAction.Dragging dragging, GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.folder != null) {
            return this.folder.listContainer.renderDroppableZone(dragging, guiGraphics, mouseX, mouseY, partialTick);
        }
        packList.renderDroppableZone(guiGraphics, dragging, mouseX, mouseY, partialTick);
        if (packList.isMouseOver(mouseX, mouseY)) {
            return dragging.target() == key || PackListUtils.canInteract(dragging.target(), key);
        }
        return false;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        packList.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (this.folder != null) {
            this.folder.extractRenderState(graphics, mouseX, mouseY, partialTick);
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
        if (listState.folder() != null && this.folder != null) {
            this.folder.listContainer.visitDeepestList(visitor);
        } else {
            visitor.accept(packList);
        }
    }

    public void onDrop(ActiveAction.Dragging dragging, int mouseX, int mouseY) {
        this.visitDeepestList(list -> list.onDrop(dragging, mouseX, mouseY));
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
        if (root != null && listState.folder() != null && this.folder != null) {
            return root.isMouseOver(mouseX, mouseY);
        }
        return super.isMouseOver(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
        return ContainerEventHandlerPatch.super.mouseClicked(mouseButtonEvent, doubleClicked);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        return ContainerEventHandlerPatch.super.mouseReleased(mouseButtonEvent);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
        return ContainerEventHandlerPatch.super.mouseDragged(mouseButtonEvent, dragX, dragY);
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

    @Override
    public void removeChildren() {
        if (this.folder != null) {
            screenContext.dispatch(new PackListIntent.CloseFolder(key));
        }
    }

    static class Folder extends AbstractContainerEventHandler implements FocusPathProvider, FZContextMenu.Source, ContainerEventHandlerPatch, Renderable {
        private static final int HEADER_SIZE = 16;
        private static final int LAYOUT_SPACING = SPACING / 2;
        private final PackListContainer root;
        private final FZIcon background;
        private final PackListContainer listContainer;
        private final FZIconButton closeButton;
        private final FZIcon folderIcon;
        private final FZText folderTitle;
        private final FZLayout layout;
        private PackEntry.@Nullable Parent pack;
        private List<GuiEventListener> children = Collections.emptyList();
        private List<Renderable> renderables = Collections.emptyList();
        private boolean childOpened;

        Folder(PackListContainer root, PackResourcesService resourcesService, PackListKey key, PackEntry.Parent pack) {
            this.root = root;
            this.listContainer = new PackListContainer(root, resourcesService, key);
            this.background = FZIcon.builder(Identifier.withDefaultNamespace("popup/background")).build();
            this.closeButton = FZIconButton.builder()
                    .size(HEADER_SIZE, HEADER_SIZE)
                    .icon(new WidgetElements(PackedPacks.id("icon/cross"), 16, 16))
                    .onPress(() -> root.screenContext.dispatch(new PackListIntent.CloseFolder(key)))
                    .build();
            this.folderIcon = FZIcon.builder(GuiUtils.lazyTexture(() -> resourcesService.getIcon(pack), 16, 16))
                    .size(HEADER_SIZE, HEADER_SIZE)
                    .build();
            this.folderTitle = FZText.builder(pack.title())
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

            arrangeElements();
        }

        private void onStateChanged(PackListState.Folder listState, ProfilesState profilesState) {
            if (this.pack != listState.pack()) {
                this.pack = listState.pack();

                if (this.pack == null) {
                    this.children = Collections.emptyList();
                    this.renderables = Collections.emptyList();
                    this.childOpened = false;
                    return;
                }

                if (listState.contents().folder() == null) {
                    folderIcon.setMessage(this.pack.title());

                    this.renderables = List.of(background, folderIcon, folderTitle, closeButton, listContainer);
                    this.children = List.of(closeButton, listContainer);
                    this.childOpened = false;
                } else {
                    this.renderables = List.of(listContainer);
                    this.children = List.of(listContainer);
                    this.childOpened = true;
                }
            }

            if (listContainer.listState != listState.contents() || listContainer.profilesState != profilesState) {
                listContainer.onStateChanged(listState.contents(), profilesState);
            }
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            if (isMouseOver(mouseX, mouseY)) {
                graphics.requestCursor(CursorType.DEFAULT);
            }
            for (Renderable renderable : this.renderables) {
                renderable.extractRenderState(graphics, mouseX, mouseY, partialTick);
            }
        }

        @Override
        public void fidgetz$updateContextEntries(double x, double y, FZContextMenu.Collector collector) {
            PackEntry.Parent pack = this.pack;

            if (this.childOpened || pack == null) {
                FZContextMenu.Source.super.fidgetz$updateContextEntries(x, y, collector);
                return;
            }

            collector.addEntry(builder -> builder
                    .message(pack.title())
                    .icon(padded16Rect(Renderables.texture(listContainer.resourcesService.getIcon(pack), 32, 32)))
                    .background(Renderables.fill(Colors.GRAY_500))
                    .onPress(e -> {
                        e.context().closeMenu();
                        return false;
                    })
                    .allowAutoDivideAfterEntry(false)
                    .applyCursorChangeWhenActive(false));

            collector.addEntry(builder -> builder
                    .message(CommonComponents.GUI_BACK.copy().append(CommonComponents.ELLIPSIS))
                    .onPress(() -> root.screenContext.dispatch(new PackListIntent.CloseFolder(listContainer.key.unnest()))));

            if (!listContainer.packList.isHovered()) {
                if (listContainer.resourcesService.isModifiable(listContainer.profilesState, pack)) {
                    collector.addEntry(builder -> builder
                            .message(RENAME_FILE_TEXT)
                            .active(() -> listContainer.resourcesService.isModifiable(listContainer.profilesState, pack))
                            .onPress(() -> root.screenContext.dispatch(new PackListIntent.OpenRenameModal(listContainer.key, pack))));
                    collector.addEntry(builder -> builder
                            .message(DELETE_FILE_TEXT)
                            .active(() -> listContainer.resourcesService.isModifiable(listContainer.profilesState, pack))
                            .onPress(() -> root.screenContext.dispatch(new PackListIntent.Delete(listContainer.key, pack))));
                }

                collector.addEntry(builder -> builder.message(OPEN_FILE_TEXT).onPress(() -> PackUtil.openPack(pack.path())));
                collector.addEntry(builder -> builder.message(OPEN_PARENT_TEXT).onPress(() -> PackUtil.openParent(pack.path())));
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
            if (this.childOpened) {
                return ComponentPath.path(this, listContainer.nextFocusPath(event));
            }

            if (event instanceof FocusNavigationEvent.InitialFocus) {
                ComponentPath path = listContainer.nextFocusPath(event);
                return path == null ? ComponentPath.path(closeButton, this) : ComponentPath.path(this, path);
            }

            if (!isFocused() &&
                event instanceof FocusNavigationEvent.ArrowNavigation(ScreenDirection direction, _) &&
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
        public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
            return ContainerEventHandlerPatch.super.mouseClicked(mouseButtonEvent, doubleClicked) && pack != null;
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
