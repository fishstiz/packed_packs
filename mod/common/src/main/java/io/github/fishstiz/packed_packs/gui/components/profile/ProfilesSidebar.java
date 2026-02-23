package io.github.fishstiz.packed_packs.gui.components.profile;

import io.github.fishstiz.fidgetz.gui.components.*;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuContainer;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.gui.layouts.ProfileHeader;
import io.github.fishstiz.packed_packs.gui.model.ProfilesViewModel;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;

import static io.github.fishstiz.fidgetz.util.DrawUtil.DEMO_BACKGROUND;
import static io.github.fishstiz.packed_packs.gui.model.ProfilesViewModel.NO_PROFILE_TEXT;
import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.SPACING;

public class ProfilesSidebar extends ToggleableDialog<LayoutWrapper<FlexLayout>> implements ContextMenuContainer {
    private static final int MIN_WIDTH = 104;
    private static final int MAX_WIDTH = SPACING * 20;
    private final ProfilesViewModel viewModel;
    private final FidgetzButton<Void> noProfileButton;
    private final ProfileList profileList;

    public <S extends Screen & ToggleableDialogContainer> ProfilesSidebar(S screen, ProfilesViewModel viewModel) {
        super(createBuilder(screen));
        this.root().setPadding(SPACING);
        this.root().setMinWidth(MIN_WIDTH);

        this.viewModel = viewModel;

        FidgetzButton<Void> closeButton = this.addRenderableWidget(FidgetzButton.<Void>builder()
                .makeSquare()
                .setMessage(CommonComponents.GUI_DONE)
                .setSprite(GuiConstants.CROSS_SPRITE)
                .setOnPress(this::close)
                .build());
        FidgetzText<Void> titleWidget = this.addRenderableOnly(FidgetzText.<Void>builder()
                .setMessage(ProfilesViewModel.TITLE_TEXT)
                .setOffsetY(1)
                .build());
        this.noProfileButton = this.addRenderableWidget(FidgetzButton.<Void>builder()
                .setMessage(NO_PROFILE_TEXT)
                .setOnPress(this.viewModel::unselect)
                .build());
        FidgetzButton<Void> copyButton = this.addRenderableWidget(FidgetzButton.<Void>builder()
                .setMessage(ResourceUtil.getText("profile.new"))
                .setTooltip(Tooltip.create(ResourceUtil.getText("profile.new.info")))
                .setOnPress(this.viewModel::copySelected)
                .addListener(this::close)
                .build());
        this.profileList = this.addRenderableWidget(new ProfileList(viewModel));

        FlexLayout header = FlexLayout.horizontal(this::getMaxWidth).spacing(SPACING);
        header.addChild(closeButton);
        header.addFlexChild(titleWidget);

        FlexLayout actions = FlexLayout.horizontal(this::getMaxWidth).spacing(SPACING);
        actions.addFlexChild(this.noProfileButton);
        actions.addFlexChild(copyButton);

        FlexLayout contents = FlexLayout.horizontal(this::getMaxWidth);
        contents.addFlexChild(profileList, true);

        this.root().layout().addChild(header);
        this.root().layout().addChild(actions);
        this.root().layout().addFlexChild(contents, true);

        this.viewModel.subscribe(ProfilesViewModel.Property.SELECTED, this::refresh);
    }

    private int getMaxWidth() {
        return MAX_WIDTH;
    }

    private void close() {
        this.setOpen(false);
    }

    private void refresh() {
        this.noProfileButton.active = this.viewModel.selectedProfile() != null;
    }

    @Override
    public void repositionElements() {
        this.root().setMinHeight(getMaxHeight(this.screen));
        this.root().arrangeElements();
        this.root().setPosition(0, 0);
    }

    private static int getMaxHeight(Screen screen) {
        return screen.height - SPACING * 2;
    }

    public ProfileHeader createProfileHeader() {
        return new ProfileHeader(this.viewModel, value -> this.profileList.scheduleRefresh());
    }

    private static <S extends Screen & ToggleableDialogContainer> Builder<LayoutWrapper<FlexLayout>, ?> createBuilder(S screen) {
        FlexLayout layout = FlexLayout.vertical(() -> getMaxHeight(screen)).spacing(SPACING);
        return builder(screen, new LayoutWrapper<>(layout)).setBackground(DEMO_BACKGROUND).setFocusOnOpen(true);
    }
}
