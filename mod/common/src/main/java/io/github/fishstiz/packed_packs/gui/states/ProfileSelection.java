package io.github.fishstiz.packed_packs.gui.states;

import io.github.fishstiz.packed_packs.config.PackOverride;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.pack.PackEntry;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiPredicate;

public interface ProfileSelection {
    @Nullable Profile defaultProfile();

    @Nullable Profile selectedProfile();

    default boolean isSelected(Profile profile) {
        Profile selectedProfile = selectedProfile();
        return selectedProfile != null && Objects.equals(selectedProfile.getId(), profile.getId());
    }

    default boolean isLocked() {
        Profile selectedProfile = selectedProfile();
        return selectedProfile != null && selectedProfile.isLocked();
    }

    default boolean isPackHidden(PackEntry entry) {
        Profile selectedProfile = selectedProfile();
        Profile defaultProfile = defaultProfile();

        PackOverride overrides = selectedProfile == null ? null : selectedProfile.getOverrides(entry.id());
        PackOverride defaultOverrides = defaultProfile == null ? null : defaultProfile.getOverrides(entry.id());

        if (defaultOverrides != null && Boolean.TRUE.equals(defaultOverrides.hidden())) {
            return true;
        } else if (overrides != null && overrides.hidden() != null) {
            return overrides.hidden();
        }

        return false;
    }

    default boolean isPackRequired(PackEntry entry) {
        Profile selectedProfile = selectedProfile();
        Profile defaultProfile = defaultProfile();

        PackOverride overrides = selectedProfile == null ? null : selectedProfile.getOverrides(entry.id());
        PackOverride defaultOverrides = defaultProfile == null ? null : defaultProfile.getOverrides(entry.id());

        if (defaultOverrides != null && defaultOverrides.required() != null) {
            return defaultOverrides.required();
        } else if (overrides != null && overrides.required() != null) {
            return overrides.required();
        }

        return entry.selectionConfig().required();
    }

    default boolean isPackFixed(PackEntry entry) {
        Profile selectedProfile = selectedProfile();
        Profile defaultProfile = defaultProfile();

        PackOverride overrides = selectedProfile == null ? null : selectedProfile.getOverrides(entry.id());
        PackOverride defaultOverrides = defaultProfile == null ? null : defaultProfile.getOverrides(entry.id());

        if (defaultOverrides != null && defaultOverrides.position() != null) {
            return defaultOverrides.position().fixed();
        } else if (overrides != null && overrides.position() != null) {
            return overrides.position().fixed();
        }

        return entry.selectionConfig().fixedPosition();
    }

    default Pack.Position getPackPosition(PackEntry entry) {
        Profile selectedProfile = selectedProfile();
        Profile defaultProfile = defaultProfile();

        PackOverride overrides = selectedProfile == null ? null : selectedProfile.getOverrides(entry.id());
        PackOverride defaultOverrides = defaultProfile == null ? null : defaultProfile.getOverrides(entry.id());

        if (defaultOverrides != null && defaultOverrides.position() != null) {
            return defaultOverrides.position().override();
        } else if (overrides != null && overrides.position() != null) {
            return overrides.position().override();
        }

        return entry.selectionConfig().defaultPosition();
    }

    default PackSelectionConfig getPackSelectionConfig(PackEntry entry) {
        Profile selectedProfile = selectedProfile();
        Profile defaultProfile = defaultProfile();

        PackOverride overrides = selectedProfile == null ? null : selectedProfile.getOverrides(entry.id());
        PackOverride defaultOverrides = defaultProfile == null ? null : defaultProfile.getOverrides(entry.id());

        if ((defaultOverrides != null && (defaultOverrides.required() != null || defaultOverrides.position() != null)) ||
            (overrides != null && (overrides.required() != null || overrides.position() != null))) {
            return new PackSelectionConfig(isPackRequired(entry), getPackPosition(entry), isPackFixed(entry));
        } else {
            return entry.selectionConfig();
        }
    }


    default boolean isSelectedDefault() {
        Profile selectedProfile = selectedProfile();
        return selectedProfile != null && selectedProfile == defaultProfile();
    }

    default boolean hasOverride(String packId) {
        Profile defaultProfile = defaultProfile();
        Profile selectedProfile = selectedProfile();

        return (defaultProfile != null && defaultProfile.hasOverride(packId)) ||
               (selectedProfile != null && selectedProfile.hasOverride(packId));
    }

    default ProfileScope hasOverride(String packId, BiPredicate<Profile, String> option) {
        Profile defaultProfile = defaultProfile();
        Profile selectedProfile = selectedProfile();
        ProfileScope scope = ProfileScope.NONE;

        if (defaultProfile != null && option.test(defaultProfile, packId)) {
            scope = ProfileScope.GLOBAL;
        }
        if (selectedProfile != null && option.test(selectedProfile, packId)) {
            scope = !scope.exists() ? ProfileScope.LOCAL : ProfileScope.COMPOSITE;
        }

        return scope;
    }

    default void validate(PackEntry pack) {
        Profile selectedProfile = selectedProfile();
        if (selectedProfile == null) return;

        Profile defaultProfile = defaultProfile();

        // non-default profiles cannot override required to false (why?? i forgor now)
        if (defaultProfile == null || !defaultProfile.overridesRequired(pack.id())) {
            if (selectedProfile.overridesRequired(pack.id()) && !selectedProfile.isRequired(pack.id())) {
                selectedProfile.withRequiredOverride(null, pack.id());
            }
        }
    }
}
