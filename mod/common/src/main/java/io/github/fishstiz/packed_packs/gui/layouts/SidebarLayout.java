package io.github.fishstiz.packed_packs.gui.layouts;

import io.github.fishstiz.fidgetz.v0.gui.components.*;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZFlexLayout;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZLayout;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.gui.components.ProfileList;
import io.github.fishstiz.packed_packs.gui.model.PackedPacksStore;
import io.github.fishstiz.packed_packs.gui.model.ProfilesViewModel;
import net.minecraft.network.chat.Component;

import static io.github.fishstiz.packed_packs.util.GuiUtils.*;

public class SidebarLayout extends WrappedLayout {
    private static final int WIDTH = 175;

    protected SidebarLayout(FZLayout layout) {
        super(layout);
    }

    public static SidebarLayout create(ScreenContext context, PackedPacksStore store, Runnable close) {
        final ProfilesViewModel model = store.createProfilesSlice();
        return new SidebarLayout(FZFlexLayout.vertical().also(sidebar -> {
            sidebar.spacing(SPACING).maxWidth(WIDTH);

            sidebar.child(FZFlexLayout.horizontal(), sidebar.flexChildHorizontalSettings()).also(header -> {
                header.spacing(SPACING).defaultChildSettings().alignVerticallyMiddle();

                header.child(FZIconButton.builder()
                        .square()
                        .icon(new WidgetElements(CROSS_SPRITE, 16, 16))
                        .onPress(close)
                        .build());
                header.child(
                        FZText.builder(ProfilesViewModel.TITLE_TEXT).build(),
                        header.flexChildHorizontalSettings()
                );
            });

            sidebar.child(FZFlexLayout.horizontal(), sidebar.flexChildHorizontalSettings()).also(actions -> {
                actions.spacing(SPACING).defaultChildSettings().flexMain();

                actions.child(FZButton.bind("NoProfileButton", store
                        .map(s -> s.profiles().selectedProfile())
                        .map(profile -> FZButton.builder()
                                .message(ProfilesViewModel.NO_PROFILE_TEXT)
                                .onPress(model::unselect)
                                .active(profile != null)
                                .toProps())));

                actions.child(FZButton.builder()
                        .message(Component.translatable("packed_packs.profile.new"))
                        .tooltip(Component.translatable("packed_packs.profile.new.info"))
                        .onPress(() -> {
                            model.copySelected();
                            close.run();
                        })
                        .build());
            });

            sidebar.child(new ProfileList(context, model), sidebar.flexChildSettings().minFlexWidth(WIDTH));
        }));
    }
}
