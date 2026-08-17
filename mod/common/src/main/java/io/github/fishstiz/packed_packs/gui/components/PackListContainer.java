package io.github.fishstiz.packed_packs.gui.components;

import com.mojang.blaze3d.platform.cursor.CursorType;
import io.github.fishstiz.fidgetz.v0.gui.components.*;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZComposedLayout;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZFlexLayout;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZLayout;
import io.github.fishstiz.fidgetz.v0.gui.layouts.Justification;
import io.github.fishstiz.fidgetz.v0.gui.renderables.Renderables;
import io.github.fishstiz.packed_packs.config.FolderPackMeta;
import io.github.fishstiz.packed_packs.gui.ContainerEventHandlerPatch;
import io.github.fishstiz.packed_packs.gui.FocusPathProvider;
import io.github.fishstiz.packed_packs.gui.FocusTarget;
import io.github.fishstiz.packed_packs.gui.states.PackListKey;
import io.github.fishstiz.packed_packs.gui.states.PackListType;
import io.github.fishstiz.packed_packs.gui.states.ActiveAction;
import io.github.fishstiz.packed_packs.gui.states.PackListState;
import io.github.fishstiz.packed_packs.gui.actions.intents.PackListIntent;
import io.github.fishstiz.packed_packs.gui.states.PackListComputed;
import io.github.fishstiz.packed_packs.pack.PackIconCache;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksContext;
import io.github.fishstiz.packed_packs.pack.PackNode;
import io.github.fishstiz.packed_packs.gui.states.ProfileSelection;
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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static io.github.fishstiz.packed_packs.util.GuiUtils.*;

public class PackListContainer extends AbstractWidget implements FocusPathProvider, Layout, ContainerEventHandlerPatch {
    private final @Nullable PackListContainer head;
    private final PackedPacksContext context;
    private final PackList packList;
    private final PackListComputed state;
    private @Nullable Folder folder;
    private @Nullable GuiEventListener focused;
    private boolean dragging;
    private List<GuiEventListener> children;

    private PackListContainer(@Nullable PackListContainer head, PackedPacksContext context, PackListComputed state) {
        super(0, 0, 0, 0, CommonComponents.EMPTY);
        this.head = head;
        this.context = context;
        this.state = state;
        this.packList = new PackList(context,  state);
        this.children = List.of(this.packList);
    }

    private PackListContainer(PackListContainer head, PackListState state, PackListKey key) {
        this(head, head.context, new PackListComputed(key, state, head.state.profiles()));
    }

    public static PackListContainer createHead(PackedPacksContext context, PackListType type) {
        PackListKey key = PackListKey.head(type);
        PackListComputed computed = new PackListComputed(
                key,
                context.state().value().getHeadList(type),
                context.state().value().profiles()
        );

        PackListContainer root = new PackListContainer(null, context, computed);

        context.state().subscribe(
                "PackListContainer@" + key,
                value -> {
                    root.onStateChanged(value.getHeadList(type), value.profiles(), type.available());
                    root.onStateChanged(value.dragging());
                }
        );

        return root;
    }

    private void updateChild(GuiEventListener target) {
        this.children = List.of(target);
        if (getFocused() != null) {
            setFocused(target);
        }
    }

    private void onStateChanged(ActiveAction.@Nullable Dragging dragging) {
        state.onDrag(dragging);

        if (folder != null) {
            folder.listContainer.onStateChanged(dragging);
        }
    }

    private void onStateChanged(PackListState newState, ProfileSelection newProfiles, boolean folderUnlockable) {
        PackListState prev = this.state.state();
        ProfileSelection prevProfiles = this.state.profiles();

        if (prev == newState && newProfiles == prevProfiles) {
            return;
        }

        this.state.onStateChanged(newState, newProfiles);

        PackListState folderState = newState.folder();

        if ((folderState == null) != (this.folder == null)) {
            if (folderState != null) {
                Folder newFolder = new Folder(head == null ? this : head, this.state.key().nest(), folderState);
                this.folder = newFolder;
                updateChild(newFolder);
                ComponentPath path = newFolder.nextFocusPath(new FocusNavigationEvent.InitialFocus());
                if (path != null) path.applyFocus(true);
            } else {
                this.folder.onClose();
                this.folder = null;
                updateChild(packList);
            }
        }

        if (this.folder != null && folderState != null) {
            this.folder.onStateChanged(folderState, newProfiles, folderUnlockable && !newState.module());
        } else if (prev.visiblePacks() != newState.visiblePacks() || prevProfiles != newProfiles) {
            packList.rebuildEntries();
        }
    }

    public boolean extractDropCandidateRenderState(
            GuiGraphicsExtractor graphics,
            ActiveAction.Dragging dragging,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        PackListContainer leafContainer = this;

        while (leafContainer.folder != null) {
            leafContainer = leafContainer.folder.listContainer;
        }

        if ((leafContainer.packList.extractDropCandidateRenderState(graphics, dragging, mouseX, mouseY, partialTick)
             || dragging.src().equals(leafContainer.state.key()))
            && leafContainer.packList.isHovered()) {
            return true;
        }

        PackListContainer root = this.head == null ? this : this.head;
        if (state.key().type().available() && !state.isLocked() && state.canDrop(0)) {
            renderDropToDisabledZone(root, graphics);
            return root.isHovered();
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

    public void visitLeafList(Consumer<PackList> visitor) {
        if (state.state().folder() != null && this.folder != null) {
            this.folder.listContainer.visitLeafList(visitor);
        } else {
            visitor.accept(packList);
        }
    }

    public void visitLists(Consumer<PackList> visitor) {
        visitor.accept(packList);
        if (state.state().folder() != null && this.folder != null) {
            this.folder.listContainer.visitLists(visitor);
        }
    }

    public void onDrop(ActiveAction.Dragging dragging, int mouseX, int mouseY) {
        if (state.isLocked()) {
            context.dispatch(new PackListIntent.Drop(dragging.src()));
            return;
        }

        if (state.key().type().enabled()) {
            visitLeafList(list -> list.onDrop(dragging, mouseX, mouseY));
            return;
        }

        PackListContainer leafContainer = this;
        while (leafContainer.folder != null) {
            leafContainer = leafContainer.folder.listContainer;
        }

        if (leafContainer.packList.isHovered() && (leafContainer.state.isDropCandidate() || dragging.src().equals(leafContainer.state.key()))) {
            leafContainer.packList.onDrop(dragging, mouseX, mouseY);
            return;
        }

        context.dispatch(new PackListIntent.Drop(dragging.src(), state.key(), 0));
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
        if (folder != null) {
            folder.arrangeElements();
        }
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        packList.setWidth(width);
        if (folder != null) {
            folder.arrangeElements();
        }
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
        if (head != null && state.state().folder() != null && this.folder != null) {
            return head.isMouseOver(mouseX, mouseY);
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

    public void rebuildEntries() {
        visitLists(PackList::rebuildEntries);
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
        private final FZButton recallButton;
        private final FZIconButton lockButton;
        private final FZLayout layout;
        private PackListState folderState;
        private PackNode.Parent parent;
        private List<GuiEventListener> children = Collections.emptyList();
        private List<Renderable> renderables = Collections.emptyList();
        private boolean childOpened;
        private boolean closed;

        Folder(PackListContainer head, PackListKey key, PackListState state) {
            this.root = head;
            this.folderState = state;
            this.parent = Objects.requireNonNull(state.parent(), "folder parent cannot be null");
            this.listContainer = new PackListContainer(head, state, key);
            this.background = FZIcon.builder(Identifier.withDefaultNamespace("popup/background")).build();
            this.closeButton = FZIconButton.builder()
                    .size(HEADER_SIZE, HEADER_SIZE)
                    .icon(new WidgetElements(key.depth() > 1 ? ARROW_UP_SPRITE : CROSS_SPRITE, 16, 16))
                    .onPress(() -> head.context.dispatch(new PackListIntent.CloseFolder(key.unnest())))
                    .focusOnInteraction(false)
                    .build();
            PackIconCache iconCache = head.context.iconCache();
            this.folderIcon = FZIcon.builder(GuiUtils.lazyTexture(() -> iconCache.get(this.parent), 16, 16))
                    .size(HEADER_SIZE, HEADER_SIZE)
                    .build();
            this.folderTitle = FZText.builder(parent.title())
                    .height(HEADER_SIZE)
                    .build();
            this.recallButton = FZButton.builder()
                    .size(HEADER_SIZE, HEADER_SIZE)
                    .message(Component.literal("<<"))
                    .tooltip(Component.translatable("packed_packs.folder.recall"))
                    .visible(key.type().available())
                    .onPress(() -> head.context.dispatch(new PackListIntent.Recall(key, this.parent)))
                    .build();
            this.lockButton = FZIconButton.builder(new WidgetRenderables(
                            GuiUtils.lazySprite(() -> folderState.module() ? LOCK_SPRITE : UNLOCK_SPRITE),
                            Renderables.sprite(LOCK_SPRITE_DISABLED),
                            GuiUtils.lazySprite(() -> folderState.module() ? LOCK_SPRITE_HIGHLIGHTED : UNLOCK_SPRITE_HIGHLIGHTED)
                    ))
                    .size(HEADER_SIZE, HEADER_SIZE)
                    .onPress(() -> head.context.dispatch(
                            new PackListIntent.UpdateModule(key, this.parent, !this.folderState.module())
                    ))
                    .build();

            FZFlexLayout section = FZFlexLayout.vertical(head).spacing(LAYOUT_SPACING);
            {
                FZFlexLayout header = section.child(FZFlexLayout.horizontal(), section.flexChildHorizontalSettings());
                {
                    header.spacing(LAYOUT_SPACING).alignContents(Justification.CENTER);
                    header.child(closeButton);
                    header.child(folderIcon);
                    header.child(folderTitle, header.flexChildHorizontalSettings());
                    header.child(recallButton);
                    header.child(lockButton);
                }
                section.child(listContainer, section.flexChildSettings());
            }

            this.layout = FZComposedLayout.compose(section).padding(SPACING).clamp(head::getRectangle);

            arrangeElements();
            updateChildren();
        }

        private void updateChildren() {
            if (folderState.folder() == null) {
                this.renderables = List.of(
                        background,
                        folderIcon,
                        folderTitle,
                        closeButton,
                        recallButton,
                        lockButton,
                        listContainer
                );
                this.children = List.of(closeButton, recallButton, lockButton, listContainer);
                this.childOpened = false;
            } else {
                this.renderables = List.of(listContainer);
                this.children = List.of(listContainer);
                this.childOpened = true;
            }
        }

        private void onStateChanged(PackListState folderState, ProfileSelection profiles, boolean unlockable) {
            PackListState prevFolderState = this.folderState;
            this.folderState = folderState;

            lockButton.active = unlockable && !profiles.isLocked();
            recallButton.active = !folderState.module() && !profiles.isLocked();

            if (prevFolderState.parent() != folderState.parent()) { // I don't think this will ever be true, but just in case
                this.parent = Objects.requireNonNull(folderState.parent(), "folder parent cannot be null");
                folderIcon.setMessage(parent.title());
            }
            if ((prevFolderState.folder() == null) != (folderState.folder() == null)) {
                updateChildren();
            }
            if (listContainer.state.state() != folderState || listContainer.state.profiles() != profiles) {
                listContainer.onStateChanged(folderState, profiles, unlockable);
            }
        }

        private void onClose() {
            this.closed = true;

            listContainer.context.resources().saveFolderMetadata(this.parent, new FolderPackMeta(
                    folderState.module(),
                    folderState.packs().stream().map(PackNode::id).toList()
            ));

            if (listContainer.folder != null) {
                listContainer.folder.onClose();
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
            PackNode.Parent pack = this.parent;

            if (this.childOpened) {
                FZContextMenu.Source.super.fidgetz$updateContextEntries(x, y, collector);
                return;
            }

            collector.addEntry(builder -> builder
                    .message(pack.title())
                    .icon(padded16Rect(Renderables.texture(listContainer.context.iconCache().get(pack), 32, 32)))
                    .background(Renderables.fill(Colors.GRAY_500))
                    .onPress(e -> {
                        e.context().closeMenu();
                        return false;
                    })
                    .allowAutoDivideAfterEntry(false)
                    .applyCursorChangeWhenActive(false));

            collector.addEntry(builder -> builder
                    .message(CommonComponents.GUI_BACK.copy().append(CommonComponents.ELLIPSIS))
                    .onPress(() -> root.context.dispatch(new PackListIntent.CloseFolder(listContainer.state.key().unnest()))));

            if (!listContainer.packList.isHovered()) {
                if (listContainer.context.resources().isModifiable(listContainer.state.profiles(), pack)) {
                    collector.addEntry(builder -> builder
                            .message(RENAME_FILE_TEXT)
                            .active(() -> listContainer.context.resources().isModifiable(listContainer.state.profiles(), pack))
                            .onPress(() -> root.context.dispatch(new PackListIntent.OpenRenameModal(listContainer.state.key(), pack))));
                    collector.addEntry(builder -> builder
                            .message(DELETE_FILE_TEXT)
                            .active(() -> listContainer.context.resources().isModifiable(listContainer.state.profiles(), pack))
                            .onPress(() -> root.context.dispatch(new PackListIntent.Delete(listContainer.state.key(), pack))));
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
            if ((root.getWidth() != 0 && layout.getWidth() != root.getWidth())
                || (root.getHeight() != 0 && layout.getHeight() != root.getHeight())) {
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
        public boolean shouldTakeFocusAfterInteraction() {
            return !closed;
        }

        @Override
        public void setFocused(boolean isFocused) {
            if (!isFocused) {
                setFocused(null);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
            return ContainerEventHandlerPatch.super.mouseClicked(mouseButtonEvent, doubleClicked) && !closed;
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
