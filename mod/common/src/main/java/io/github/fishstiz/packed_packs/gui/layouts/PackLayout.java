package io.github.fishstiz.packed_packs.gui.layouts;

import io.github.fishstiz.fidgetz.gui.components.CyclicButton;
import io.github.fishstiz.fidgetz.gui.components.FidgetzButton;
import io.github.fishstiz.fidgetz.gui.components.ToggleButton;
import io.github.fishstiz.fidgetz.gui.components.ToggleableEditBox;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayoutElement;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.gui.shapes.Size;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.config.Preferences;
import io.github.fishstiz.packed_packs.gui.components.PreferenceToggle;
import io.github.fishstiz.packed_packs.gui.components.pack.PackList;
import io.github.fishstiz.packed_packs.gui.components.pack.PackListContainer;
import io.github.fishstiz.packed_packs.gui.components.pack.Query;
import io.github.fishstiz.packed_packs.gui.model.PackListKey;
import io.github.fishstiz.packed_packs.gui.model.PackListType;
import io.github.fishstiz.packed_packs.gui.model.PackListViewModel;
import io.github.fishstiz.packed_packs.gui.model.PackedPacksViewModel;
import io.github.fishstiz.packed_packs.gui.states.PackedPacksState;
import io.github.fishstiz.packed_packs.gui.states.ProfilesState;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.SPACING;

public class PackLayout implements FlexLayoutElement, Layout {
    private final FlexLayout layout = FlexLayout.vertical().spacing(SPACING);
    private final FlexLayout headerLayout;
    private final PackListViewModel viewModel;
    private final PackListContainer root;
    private final ToggleableEditBox<Void> searchField;
    private final FidgetzButton<Void> transferButton;
    private @Nullable CyclicButton<Query.SortOption, Void> sortButton;
    private @Nullable ToggleButton<Void> compatButton;

    public PackLayout(ScreenContext screenContext, PackedPacksViewModel viewModel, PackListType type) {
        this.viewModel = viewModel.createPackListSlice(type);
        this.root = new PackListContainer(screenContext, this.viewModel);
        this.headerLayout = FlexLayout.horizontal(this.root::getWidth).spacing(SPACING);

        this.searchField = ToggleableEditBox.<Void>builder()
                .setHint(ResourceUtil.getText("search").append(CommonComponents.ELLIPSIS))
                .setEditable(true)
                .addListener(this.viewModel::search)
                .build();
        this.transferButton = FidgetzButton.<Void>builder()
                .makeSquare()
                .setOnPress(this.viewModel::transferAll)
                .setTooltip(Tooltip.create(ResourceUtil.getText("transfer_all.info")))
                .build();
        this.transferButton.active = !this.viewModel.locked();

        switch (type) {
            case ENABLED -> {
                this.transferButton.setMessage(Component.literal("<<"));
                this.headerLayout.addChild(transferButton);
                this.headerLayout.addFlexChild(this.searchField);
            }
            case AVAILABLE -> {
                this.transferButton.setMessage(Component.literal(">>"));

                this.sortButton = CyclicButton.<Query.SortOption, Void>builder(Query.SortOption.values())
                        .setPrefix(ResourceUtil.getText("sort"))
                        .makeSquare()
                        .addListener(this.viewModel::sort)
                        .addListener(Config.get()::setSort)
                        .setValue(Config.get().getSort())
                        .build();
                this.compatButton = PreferenceToggle.bind(Preferences.INCOMPATIBLE_TOGGLE_WIDGET, ToggleButton.<Void>builder()).map(
                        bound -> bound.value()
                                .setMessage(ResourceUtil.getText("hide_incompatible"))
                                .setTooltip(Tooltip.create(ResourceUtil.getText("hide_incompatible.info")))
                                .setSprite(ToggleButton.Sprites.of(
                                        new Sprite(ResourceUtil.getIcon("incompatible_hidden"), Size.of16()),
                                        new Sprite(ResourceUtil.getIcon("incompatible"), Size.of16())
                                ))
                                .setContextMenuBuilder(bound.apply(toggle -> (btn, b) -> toggle.updateBuilder(b)))
                                .setForeground(bound.toggle())
                                .makeSquare()
                                .addListener(this.viewModel::hideIncompatible)
                                .addListener(Config.get()::setHideIncompatible)
                                .setValue(Config.get().isHideIncompatible())
                                .build()).orElse(null);

                this.headerLayout.addFlexChild(this.searchField);
                this.headerLayout.addChild(sortButton);
                this.headerLayout.addChild(compatButton);
                this.headerLayout.addChild(transferButton);
            }
        }

        this.layout.addChild(this.headerLayout);
        this.layout.addFlexChild(this.root, true);

        viewModel.subscribe(PackedPacksState::profiles, this::refreshTransferButton);
        this.viewModel.subscribe(PackListViewModel.Property.QUERY, this::refresh);
    }

    private void refreshTransferButton(ProfilesState profilesState) {
        this.transferButton.active = !this.viewModel.locked();
    }

    private void refresh() {
        Query query = this.viewModel.query();

        this.searchField.setValueSilently(query.unmodifiedSearch());

        if (this.sortButton != null && query.sort() != null) {
            this.sortButton.setValueSilently(query.sort());
        }
        if (this.compatButton != null) {
            this.compatButton.setValueSilently(query.hideIncompatible());
        }
    }

    public PackListKey key() {
        return this.viewModel.key();
    }

    public PackListContainer container() {
        return this.root;
    }

    public ToggleableEditBox<Void> getSearchField() {
        return this.searchField;
    }

    public void setHeaderVisibility(boolean visible) {
        this.headerLayout.visitWidgets(widget -> widget.visible = visible);
        ScreenRectangle headerRect = this.headerLayout.getRectangle();
        int y = visible ? headerRect.bottom() + SPACING : headerRect.top();
        int height = visible ? this.layout.getHeight() - headerRect.height() - SPACING : this.layout.getHeight();

        this.root.setY(y);
        this.root.setHeight(height);
        this.root.visitPackLists(PackList::clampScrollAmount);
    }

    @Override
    public void setX(int x) {
        this.layout.setX(x);
    }

    @Override
    public void setY(int y) {
        this.layout.setY(y);
    }

    @Override
    public int getX() {
        return this.layout.getX();
    }

    @Override
    public int getY() {
        return this.layout.getY();
    }

    @Override
    public int getWidth() {
        return this.layout.getWidth();
    }

    @Override
    public int getHeight() {
        return this.layout.getHeight();
    }

    @Override
    public void setWidth(int width) {
        this.layout.setWidth(width);
    }

    @Override
    public void setHeight(int height) {
        this.layout.setHeight(height);
    }

    @Override
    public void visitChildren(@NonNull Consumer<LayoutElement> visitor) {
        this.layout.visitChildren(visitor);
    }

    @Override
    public void visitWidgets(@NonNull Consumer<AbstractWidget> visitor) {
        this.layout.visitWidgets(visitor);
    }

    @Override
    public void arrangeElements() {
        this.layout.arrangeElements();
    }
}
