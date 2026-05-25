package io.github.fishstiz.packed_packs.gui.states;

import io.github.fishstiz.packed_packs.config.PackOptions;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.pack.PackOptionsContext;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public record ProfilesState(
        List<Profile> profiles,
        @Nullable Profile selectedProfile,
        @Nullable Profile defaultProfile,
        PackOptions options,
        boolean renaming
) {
    private static final ProfilesState EMPTY = new ProfilesState(Collections.emptyList(), null, null);

    public ProfilesState {
        if (options == null) {
            options = new PackOptionsContext(this::selectedProfile, this::defaultProfile);
        }
    }

    public ProfilesState(
            List<Profile> profiles,
            @Nullable Profile selectedProfile,
            @Nullable Profile defaultProfile,
            @Nullable PackOptions options
    ) {
        this(profiles, selectedProfile, defaultProfile, options, false);
    }

    public ProfilesState(
            List<Profile> profiles,
            @Nullable Profile selectedProfile,
            @Nullable Profile defaultProfile,
            boolean renaming
    ) {
        this(profiles, selectedProfile, defaultProfile, null, renaming);
    }

    public ProfilesState(List<Profile> profiles, @Nullable Profile selectedProfile, @Nullable Profile defaultProfile) {
        this(profiles, selectedProfile, defaultProfile, null);
    }

    public static ProfilesState empty() {
        return EMPTY;
    }

    public ProfilesState withProfiles(List<Profile> newProfiles) {
        Profile newSelectedProfile = newProfiles.contains(this.selectedProfile) ? this.selectedProfile : null;
        Profile newDefaultProfile = newProfiles.contains(this.defaultProfile) ? this.defaultProfile : null;
        return new ProfilesState(newProfiles, newSelectedProfile, newDefaultProfile);
    }

    public ProfilesState withSelected(@Nullable Profile selectedProfile) {
        return new ProfilesState(this.profiles, selectedProfile, this.defaultProfile);
    }

    public ProfilesState withDefault(@Nullable Profile defaultProfile) {
        return new ProfilesState(this.profiles, defaultProfile == null ? this.selectedProfile : defaultProfile, defaultProfile);
    }

    public ProfilesState withRenaming(boolean renaming) {
        return new ProfilesState(profiles, selectedProfile, defaultProfile, options, renaming && canRename());
    }

    // make profile immutable at some point

    public boolean isLocked() {
        return selectedProfile != null && selectedProfile.isLocked();
    }

    public boolean canRename() {
        return selectedProfile != null && !selectedProfile.isLocked();
    }
}
