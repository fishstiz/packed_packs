package io.github.fishstiz.packed_packs.pack;

import io.github.fishstiz.fidgetz.util.lang.FunctionsUtil;
import io.github.fishstiz.packed_packs.config.PackOptions;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.config.ProfileManager;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.*;

/**
 * Checks the default profile first, then the selected profile, then pack defaults.
 */
public record PackOptionsResolver(
        Supplier<@Nullable Profile> profileSupplier,
        Supplier<@Nullable Profile> defaultProfileSupplier
) implements PackOptions {
    public static final PackOptionsResolver DATA_PACKS = new PackOptionsResolver(ProfileManager.get(PackType.SERVER_DATA));
    public static final PackOptionsResolver RESOURCE_PACKS = new PackOptionsResolver(ProfileManager.get(PackType.CLIENT_RESOURCES));

    public PackOptionsResolver(ProfileManager manager) {
        this(FunctionsUtil.nullSupplier(), manager::getDefault);
    }

    @Override
    public boolean isHidden(Pack pack) {
        return Boolean.TRUE.equals(this.inDefaultOrSelected(pack, Profile::isHidden, Profile::isHidden));
    }

    public boolean isRequired(Pack pack) {
        return Boolean.TRUE.equals(this.inDefaultOrSelected(pack, Profile::overridesRequired, Profile::isRequired));
    }

    @Override
    public boolean isFixed(Pack pack) {
        return Boolean.TRUE.equals(this.inDefaultOrSelected(pack, Profile::overridesPosition, Profile::isFixed));
    }

    @Override
    public Pack.@Nullable Position getPosition(Pack pack) {
        return this.inDefaultOrSelected(pack, Profile::overridesPosition, Profile::getPosition);
    }

    @Override
    public @Nullable PackSelectionConfig getSelectionConfig(Pack pack) {
        return this.inDefaultOrSelected(pack, (profile, p) -> profile.overridesPosition(p) || profile.overridesRequired(p), Profile::getSelectionConfig);
    }

    public boolean isRequiredOrDefault(Pack pack) {
        return this.inDefaultOrSelected(pack, Profile::overridesRequired, Profile::isRequired, Pack::isRequired);
    }

    public boolean isFixedOrDefault(Pack pack) {
        return this.inDefaultOrSelected(pack, Profile::overridesPosition, Profile::isFixed, Pack::isFixedPosition);
    }

    public Pack.@NonNull Position getPositionOrDefault(Pack pack) {
        return Objects.requireNonNull(this.inDefaultOrSelected(pack, Profile::overridesPosition, Profile::getPosition, Pack::getDefaultPosition));
    }

    public @NonNull PackSelectionConfig getSelectionConfigOrDefault(Pack pack) {
        return Objects.requireNonNull(this.getOrDefault(pack, PackOptions::getSelectionConfig, Objects::nonNull, Pack::selectionConfig));
    }

    public boolean overridesRequired(Pack pack) {
        return this.hasOverride(pack, Profile::overridesRequired);
    }

    public boolean overridesPosition(Pack pack) {
        return this.hasOverride(pack, Profile::overridesPosition);
    }

    private boolean hasOverride(Pack pack, BiPredicate<Profile, Pack> option) {
        Profile defaultProfile = this.defaultProfileSupplier.get();
        Profile selected = this.profileSupplier.get();

        if (defaultProfile != null && option.test(defaultProfile, pack)) {
            return true;
        }
        return selected != null && option.test(selected, pack);
    }

    private <T> T getOrDefault(Pack pack, BiFunction<PackOptions, Pack, T> option, Predicate<T> predicate, Function<Pack, T> defaultValue) {
        T value = option.apply(this, pack);
        return predicate.test(value) ? value : defaultValue.apply(pack);
    }

    private <T> T inDefaultOrSelected(Pack pack, BiPredicate<Profile, Pack> shouldApply, BiFunction<Profile, Pack, T> option, Function<Pack, T> defaultValue) {
        Profile defaultProfile = this.defaultProfileSupplier.get();
        if (defaultProfile != null && shouldApply.test(defaultProfile, pack)) {
            return option.apply(defaultProfile, pack);
        }
        Profile selected = this.profileSupplier.get();
        if (selected != null && shouldApply.test(selected, pack)) {
            return option.apply(selected, pack);
        }
        return defaultValue.apply(pack);
    }

    private <T> @Nullable T inDefaultOrSelected(Pack pack, BiPredicate<Profile, Pack> shouldApply, BiFunction<Profile, Pack, T> option) {
        return this.inDefaultOrSelected(pack, shouldApply, option, p -> null);
    }
}
