package io.github.fishstiz.packed_packs.gui2.models;

import io.github.fishstiz.packed_packs.config.PackOverride;
import io.github.fishstiz.packed_packs.config.Profile;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.Nullable;

public interface ProfileSelection {
    @Nullable Profile defaultProfile();

    @Nullable Profile selectedProfile();

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
}
