package io.github.fishstiz.packed_packs.gui.components.profile;

import io.github.fishstiz.fidgetz.gui.components.AbstractFixedListWidget;
import io.github.fishstiz.fidgetz.gui.components.FidgetzButton;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuContainer;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuProvider;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.ButtonSprites;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.util.ARGBColor;
import io.github.fishstiz.fidgetz.util.DrawUtil;
import io.github.fishstiz.fidgetz.util.GuiUtil;
import io.github.fishstiz.fidgetz.util.debounce.PollingDebouncer;
import io.github.fishstiz.fidgetz.util.debounce.SimplePollingDebouncer;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.gui.model.ProfilesViewModel;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.function.Consumer;

import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.*;

class ProfileList extends AbstractFixedListWidget<ProfileList.Entry> implements ContextMenuContainer {
    private static final int ITEM_HEIGHT = 20;
    private static final Component EMPTY_TEXT = ResourceUtil.getText("profile.empty");
    private static final Component DELETE_TEXT = ResourceUtil.getText("profile.delete");
    private static final Tooltip DELETE_INFO = Tooltip.create(ResourceUtil.getText("profile.delete.info"));
    private static final Sprite TRASH_SPRITE = Sprite.of16(ResourceUtil.getIcon("trash"));
    private static final Sprite STAR_OUTLINE_SPRITE = Sprite.of16(ResourceUtil.getIcon("star_outline"));
    private final ButtonSprites cachedStarSprites = ButtonSprites.of(STAR_SPRITE);
    private final ButtonSprites cachedLockedSprites = ButtonSprites.unclamp(LOCK_SPRITE);
    private final ButtonSprites cachedTrashSprites = ButtonSprites.of(TRASH_SPRITE);
    private final PollingDebouncer<Void> debouncedRefresh = new SimplePollingDebouncer<>(this::refresh, 200);
    private final ProfilesViewModel viewModel;

    ProfileList(ProfilesViewModel viewModel) {
        super(ITEM_HEIGHT);
        this.viewModel = viewModel;
        this.refresh();
        this.viewModel.subscribe(ProfilesViewModel.Property.PROFILES, this::refresh);
    }

    void scheduleRefresh() {
        this.debouncedRefresh.run();
    }

    @Override
    public void clearEntries() {
        this.children.forEach(e -> e.unsubscribe.run());
        super.clearEntries();
    }

    private void refresh() {
        this.clearEntries();
        this.viewModel.forEachEntry((entry, i) -> this.addEntry(new Entry(entry, i)));
    }

    @Override
    public void renderWidget(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.debouncedRefresh.poll();

        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);

        if (this.children().isEmpty()) {
            this.renderScrollingStringOverContents(guiGraphics.textRenderer(), EMPTY_TEXT, 0);
        }
    }

    public class Entry extends AbstractFixedListWidget<Entry>.Entry implements ContextMenuProvider {
        private final List<FidgetzButton<Void>> children;
        private final FidgetzButton<Void> selectButton;
        private final FidgetzButton<Void> deleteButton;
        private final ProfilesViewModel.Entry viewModel;
        private final Runnable unsubscribe;

        protected Entry(ProfilesViewModel.Entry viewModel, int index) {
            super(index);
            this.viewModel = viewModel;
            this.deleteButton = FidgetzButton.<Void>builder()
                    .makeSquare(this.getHeight())
                    .setMessage(DELETE_TEXT)
                    .setSprite(this.getButtonSprites())
                    .setOnPress(this.viewModel::delete)
                    .build();
            this.deleteButton.active = !viewModel.isLocked() && !viewModel.isDefault();
            if (this.deleteButton.active) this.deleteButton.setTooltip(DELETE_INFO);

            this.selectButton = FidgetzButton.<Void>builder()
                    .setMessage(this.viewModel.name())
                    .setOnPress(this.viewModel::select)
                    .build();
            this.selectButton.active = !this.viewModel.isSelected();

            this.refresh();
            this.children = List.of(this.selectButton, this.deleteButton);
            this.unsubscribe = ProfileList.this.viewModel.subscribe(ProfilesViewModel.Property.ALL, this::refresh);
        }

        private ButtonSprites getButtonSprites() {
            return this.viewModel.isDefault()
                    ? ProfileList.this.cachedStarSprites : this.viewModel.isLocked()
                    ? ProfileList.this.cachedLockedSprites : ProfileList.this.cachedTrashSprites;
        }

        private void refresh() {
            this.selectButton.active = !this.viewModel.isSelected();
            this.deleteButton.active = !this.viewModel.isLocked() && !this.viewModel.isDefault();
            this.deleteButton.setSprites(this.getButtonSprites());
        }

        @Override
        public void renderContent(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovering, float partialTick) {
            int left = this.getX();
            int top = this.getY();

            this.deleteButton.setPosition(left, top);
            this.selectButton.setPosition(left + this.deleteButton.getWidth(), top);
            this.selectButton.setWidth(width - this.deleteButton.getWidth());

            this.deleteButton.render(guiGraphics, mouseX, mouseY, partialTick);
            this.selectButton.render(guiGraphics, mouseX, mouseY, partialTick);

            if (Config.get().isDevMode()) {
                boolean hasProperty = true;
                int width = this.getWidth();
                int height = this.getHeight();
                int borderColor;

                if (this.viewModel.isDefault() && this.viewModel.isLocked()) {
                    borderColor = Theme.PURPLE_500.getARGB();
                } else if (this.viewModel.isDefault()) {
                    borderColor = Theme.BLUE_500.getARGB();
                } else if (this.viewModel.isLocked()) {
                    borderColor = Theme.RED_700.getARGB();
                } else {
                    borderColor = Theme.WHITE.getARGB();
                    hasProperty = false;
                }

                boolean hovered = guiGraphics.containsPointInScissor(mouseX, mouseY) && GuiUtil.isHovered(this, mouseX, mouseY);
                if (hasProperty || hovered) {
                    DrawUtil.renderOutline(guiGraphics, left, top, width, height, borderColor);
                }
                if (hovered) {
                    int foregroundColor = ARGBColor.withAlpha(borderColor, 0.25f);
                    guiGraphics.fill(left, top, left + width, top + height, foregroundColor);
                }
            }
        }

        @Override
        public @NonNull List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public @NonNull List<? extends NarratableEntry> narratables() {
            return this.children;
        }

        @Override
        public void visitWidgets(@NonNull Consumer<AbstractWidget> consumer) {
            this.children.forEach(consumer);
        }

        @Override
        public void buildItems(ContextMenuItemBuilder builder, int mouseX, int mouseY) {
            if (!Config.get().isDevMode()) return;

            builder.separatorIfNonEmpty();
            builder.add(GuiConstants.devItem(ResourceUtil.getText("profile.default." + (this.viewModel.isDefault() ? "unset" : "set")))
                    .icon(() -> this.viewModel.isDefault() ? STAR_SPRITE : STAR_OUTLINE_SPRITE)
                    .action(this.viewModel::toggleDefault)
                    .build());
            builder.add(GuiConstants.devItem(ResourceUtil.getText("profile." + (this.viewModel.isLocked() ? "unlock" : "lock")))
                    .icon(() -> this.viewModel.isLocked() ? LOCK_SPRITE_SMALL : UNLOCK_SPRITE_SMALL)
                    .action(this.viewModel::toggleLock)
                    .build());
        }
    }
}
