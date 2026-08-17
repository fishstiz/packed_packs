package io.github.fishstiz.packed_packs.gui.states;

import io.github.fishstiz.packed_packs.config.Profile;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ProfilesState implements ProfileSelection {
    private static final ProfilesState EMPTY = new ProfilesState(Collections.emptyList(), null, null);
    private final List<Profile> profiles;
    private final @Nullable Profile selectedProfile;
    private final @Nullable Profile defaultProfile;
    private final boolean renaming;

    // hack to trigger state changes
    private final String initialName;
    private final boolean initialLocked;
    private final int initialOverrideGen;

    public ProfilesState(
            List<Profile> profiles,
            @Nullable Profile selectedProfile,
            @Nullable Profile defaultProfile,
            boolean renaming
    ) {
        this.profiles = profiles;
        this.selectedProfile = selectedProfile;
        this.defaultProfile = defaultProfile;
        this.renaming = renaming;
        this.initialName = selectedProfile != null ? selectedProfile.getName() : null;
        this.initialLocked = selectedProfile != null && selectedProfile.isLocked();
        this.initialOverrideGen = selectedProfile != null ? selectedProfile.overridesGen() : -1;
    }

    public ProfilesState(List<Profile> profiles, @Nullable Profile selectedProfile, @Nullable Profile defaultProfile) {
        this(profiles, selectedProfile, defaultProfile, false);
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
        return new ProfilesState(profiles, selectedProfile, defaultProfile, renaming && canRename());
    }

    public boolean canRename() {
        return selectedProfile != null && !selectedProfile.isLocked();
    }

    public List<Profile> profiles() {
        return profiles;
    }

    @Override
    public @Nullable Profile selectedProfile() {
        return selectedProfile;
    }

    @Override
    public @Nullable Profile defaultProfile() {
        return defaultProfile;
    }

    public boolean renaming() {
        return renaming;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ProfilesState) obj;
        return Objects.equals(this.profiles, that.profiles) &&
               Objects.equals(this.selectedProfile, that.selectedProfile) &&
               Objects.equals(this.defaultProfile, that.defaultProfile) &&
               Objects.equals(this.initialName, that.initialName) &&
               this.initialLocked == that.initialLocked &&
               this.initialOverrideGen == that.initialOverrideGen &&
               this.renaming == that.renaming;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                profiles,
                selectedProfile,
                defaultProfile,
                renaming,
                initialName,
                initialLocked,
                initialOverrideGen
        );
    }

    @Override
    public String toString() {
        return "ProfilesState[" +
               "profiles=" + profiles + ", " +
               "selectedProfile=" + selectedProfile + ", " +
               "defaultProfile=" + defaultProfile + ", " +
               "renaming=" + renaming + ']';
    }

}
