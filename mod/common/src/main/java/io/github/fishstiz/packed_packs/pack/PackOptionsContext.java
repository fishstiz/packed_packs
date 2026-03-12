package io.github.fishstiz.packed_packs.pack;

import io.github.fishstiz.packed_packs.config.PackOptions;
import io.github.fishstiz.packed_packs.config.Profile;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

public class PackOptionsContext implements PackOptions {
    private final PackOptionsResolver resolver;

    public PackOptionsContext(PackOptionsResolver resolver) {
        this.resolver = resolver;
    }

    public PackOptionsContext(Supplier<@Nullable Profile> profileSupplier, Supplier<@Nullable Profile> defaultProfileSupplier) {
        this(new PackOptionsResolver(profileSupplier, defaultProfileSupplier));
    }

    @Override
    public boolean isHidden(Pack pack) {
        return this.resolver.isHidden(pack);
    }

    @Override
    public boolean isRequired(Pack pack) {
        return this.resolver.isRequiredOrDefault(pack);
    }

    @Override
    public boolean isFixed(Pack pack) {
        return this.resolver.isFixedOrDefault(pack);
    }

    @Override
    public Pack.Position getPosition(Pack pack) {
        return this.resolver.getPositionOrDefault(pack);
    }

    @Override
    public PackSelectionConfig getSelectionConfig(Pack pack) {
        return this.resolver.getSelectionConfigOrDefault(pack);
    }

    public Optional<Profile> getProfile() {
        return Optional.ofNullable(this.resolver.profileSupplier().get());
    }

    public Optional<Profile> getDefaultProfile() {
        return Optional.ofNullable(this.resolver.defaultProfileSupplier().get());
    }

    public void validate(Pack pack) {
        Profile profile = this.resolver.profileSupplier().get();
        if (profile == null) return;

        Profile defaultProfile = this.resolver.defaultProfileSupplier().get();

        // non-default profiles cannot override required to false (why?? i forgor now)
        if (defaultProfile == null || !defaultProfile.overridesRequired(pack)) {
            if (profile.overridesRequired(pack) && !profile.isRequired(pack)) {
                profile.setRequired(null, pack);
            }
        }
    }

    public boolean isLocked() {
        Profile profile = this.resolver.profileSupplier().get();
        return profile != null && profile.isLocked();
    }

    public boolean isDefaultProfile() {
        Profile profile = this.resolver.profileSupplier().get();
        return profile != null && profile == this.resolver.defaultProfileSupplier().get();
    }

    public boolean hasOverride(Pack pack) {
        Profile defaultProfile = this.resolver.defaultProfileSupplier().get();
        Profile profile = this.resolver.profileSupplier().get();

        return (defaultProfile != null && defaultProfile.hasOverride(pack)) ||
               (profile != null && profile.hasOverride(pack));
    }

    public ProfileScope hasOverride(Pack pack, BiPredicate<Profile, Pack> option) {
        Profile defaultProfile = this.resolver.defaultProfileSupplier().get();
        Profile profile = this.resolver.profileSupplier().get();
        ProfileScope scope = ProfileScope.NONE;

        if (defaultProfile != null && option.test(defaultProfile, pack)) {
            scope = ProfileScope.GLOBAL;
        }
        if (profile != null && option.test(profile, pack)) {
            scope = !scope.exists() ? ProfileScope.LOCAL : ProfileScope.COMPOSITE;
        }

        return scope;
    }
}
