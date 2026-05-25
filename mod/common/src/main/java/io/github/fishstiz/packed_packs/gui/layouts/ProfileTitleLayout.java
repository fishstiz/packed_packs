package io.github.fishstiz.packed_packs.gui.layouts;

import io.github.fishstiz.fidgetz.v0.gui.components.*;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZFlexLayout;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.config.ProfileManager;
import io.github.fishstiz.packed_packs.gui.intents.ProfileIntent;
import io.github.fishstiz.packed_packs.gui.model.PackedPacksStore;
import io.github.fishstiz.packed_packs.gui.states.PackedPacksState;
import io.github.fishstiz.packed_packs.gui.states.ProfilesState;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.util.CommonColors;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static io.github.fishstiz.packed_packs.gui.model.ProfilesViewModel.NO_PROFILE_TEXT;
import static io.github.fishstiz.packed_packs.util.GuiUtils.*;

public class ProfileTitleLayout {
    public static FZFlexLayout create(PackedPacksStore store) {
        FZIconButton editButton = FZIconButton.bind("ProfileNameEditButton", store
                .map(PackedPacksState::profiles)
                .map(profilesState -> FZIconButton.builder()
                        .square()
                        .tooltip(Component.translatable("packed_packs.profile.edit"))
                        .icon(getEditIcon(profilesState))
                        .visible(profilesState.selectedProfile() != null)
                        .active(profilesState.canRename())
                        .onPress(() -> store.dispatch(new ProfileIntent.ToggleRenaming()))
                        .toProps()));

        FZFlexLayout title = FZFlexLayout.horizontal().spacing(SPACING);
        {
            title.defaultChildSettings().visible();

            title.child(editButton);
            title.child(WrappedComponent.bind("ProfileNameContainer", store
                    .map(PackedPacksState::profiles)
                    .map(profilesState -> profilesState.canRename() && profilesState.renaming()
                            ? FZTextField.bind("ProfileName", store
                            .map(PackedPacksState::profiles)
                            .map(state -> createNameFieldProps(store, state)))
                            : FZText.bind("ProfileName", store
                            .map(PackedPacksState::profiles)
                            .map(ProfileTitleLayout::createNameProps))
                    )), title.flexChildHorizontalSettings());
        }

        return title;
    }

    private static FZTextField.Props createNameFieldProps(PackedPacksStore store, ProfilesState profilesState) {
        Profile selected = profilesState.selectedProfile();
        return FZTextField.builder()
                .height(20)
                .visible(selected != null)
                .active(selected != null)
                .hint(selected == null ? NO_PROFILE_TEXT : ProfileManager.getDefaultNameComponent())
                .maxLength(ProfileManager.getNameMaxLength())
                .text(selected == null ? "" : selected.getName())
                .onChange(e -> {
                    if (selected == null) return;
                    store.dispatch(new ProfileIntent.Rename(selected, e.value()));
                })
                .toProps();
    }

    private static FZText.Props createNameProps(ProfilesState profilesState) {
        Profile selected = profilesState.selectedProfile();
        return FZText.builder(getDisplayName(selected))
                .height(20)
                .visible(selected != null)
                .toProps();
    }

    private static Component getDisplayName(@Nullable Profile profile) {
        if (profile == null) return CommonComponents.EMPTY;
        String name = profile.getName();
        return name.isBlank()
                ? ComponentUtils.mergeStyles(ProfileManager.getDefaultNameComponent().copy(), Style.EMPTY.withColor(CommonColors.LIGHT_GRAY))
                : Component.literal(name);
    }

    private static WidgetElements getEditIcon(ProfilesState profilesState) {
        Profile selected = profilesState.selectedProfile();
        Profile main = profilesState.defaultProfile();

        if (profilesState.canRename()) {
            return new WidgetElements(WidgetRenderables.noFocus(PackedPacks.id("icon/edit"), PackedPacks.id("icon/edit_inactive")), 16, 16);
        }
        if (Objects.equals(main, selected)) {
            return new WidgetElements(STAR_SPRITE, 16, 16);
        }

        return new WidgetElements(LOCK_SPRITE_DISABLED, 20, 20);
    }
}
