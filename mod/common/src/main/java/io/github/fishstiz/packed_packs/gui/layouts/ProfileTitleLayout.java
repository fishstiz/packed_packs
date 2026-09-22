package io.github.fishstiz.packed_packs.gui.layouts;

import io.github.fishstiz.fidgetz.v0.gui.components.*;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZFlexLayout;
import io.github.fishstiz.fidgetz.v0.gui.state.FZRef;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.config.ProfileManager;
import io.github.fishstiz.packed_packs.gui.actions.intents.Intent;
import io.github.fishstiz.packed_packs.gui.states.ProfilesState;
import io.github.fishstiz.packed_packs.gui.actions.intents.ProfileIntent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

import static io.github.fishstiz.packed_packs.util.GuiUtils.*;

public class ProfileTitleLayout {
    public static FZFlexLayout create(FZRef<ProfilesState> state, Consumer<? super Intent> dispatcher) {
        FZIconButton editButton = FZIconButton.bind(
                "ProfileNameEditButton",
                state.map(profilesState -> FZIconButton.builder()
                        .square()
                        .tooltip(Component.translatable("packed_packs.profile.edit"))
                        .icon(getEditIcon(profilesState))
                        .visible(profilesState.selectedProfile() != null)
                        .active(profilesState.canRename())
                        .onPress(() -> dispatcher.accept(new ProfileIntent.ToggleRenaming()))
                        .toProps()
                )
        );

        FZTextField nameField = FZTextField.bind(
                "ProfileNameField",
                state.map(s -> {
                    Profile profile = s.selectedProfile();
                    return FZTextField.builder()
                            .height(20)
                            .visible(profile != null)
                            .active(profile != null)
                            .hint(profile == null ? NO_PROFILE_TEXT : ProfileManager.getDefaultNameComponent())
                            .maxLength(ProfileManager.getNameMaxLength())
                            .text(profile == null ? "" : profile.getName())
                            .onChange(e -> {
                                if (profile == null) return;
                                dispatcher.accept(new ProfileIntent.Rename(profile, e.value()));
                            })
                            .toProps();
                })
        );

        FZFlexLayout title = FZFlexLayout.horizontal().spacing(SPACING);
        {
            title.defaultChildSettings().visible();

            title.child(editButton);
            title.child(
                    WrappedComponent.bind(
                            "ProfileNameContainer",
                            state.map(profilesState -> profilesState.canRename() && profilesState.renaming()
                                    ? nameField
                                    : buildNameReadonly(profilesState)
                            )
                    ),
                    title.flexChildHorizontalSettings()
            );
        }

        return title;
    }

    private static FZText buildNameReadonly(ProfilesState profilesState) {
        Profile selected = profilesState.selectedProfile();
        return FZText.builder(getDisplayName(selected))
                .height(20)
                .visible(selected != null)
                .build();
    }

    private static Component getDisplayName(@Nullable Profile profile) {
        if (profile == null) return CommonComponents.EMPTY;
        String name = profile.getName();
        return name.isBlank()
                ? AbstractWidget.WithInactiveMessage.defaultInactiveMessage(ProfileManager.getDefaultNameComponent())
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
