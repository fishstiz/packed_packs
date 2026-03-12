package io.github.fishstiz.packed_packs.config;

import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.gui.components.pack.Query;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.packs.PackType;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.*;

public final class Config implements Serializable {
    private static final String FILENAME = "config.json";
    private static final Config INSTANCE = JsonLoader.loadOrCreateJson(getPath(), Config.class, Config::new);
    private boolean devMode = false;
    private boolean showActionBar = false;
    private boolean hideIncompatible = false;
    private String sort;
    private final ResourcePacks resourcepacks = new ResourcePacks();
    private final DataPacks datapacks = new DataPacks();

    private Config() {
    }

    private static Path getPath() {
        return PackedPacks.getConfigDir().resolve(FILENAME);
    }

    public static Config get() {
        return INSTANCE;
    }

    public static Packs packs(PackType packType) {
        return INSTANCE.get(packType);
    }

    public Packs get(PackType packType) {
        return switch (packType) {
            case CLIENT_RESOURCES -> this.getResourcepacks();
            case SERVER_DATA -> this.getDatapacks();
        };
    }

    public void save() {
        JsonLoader.saveJson(this, getPath());
    }

    public boolean isDevMode() {
        return this.devMode;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    public void setShowActionBar(boolean showActionBar) {
        this.showActionBar = showActionBar;
    }

    public boolean isShowActionBar() {
        return this.showActionBar;
    }

    public boolean isHideIncompatible() {
        return this.hideIncompatible;
    }

    public void setHideIncompatible(boolean hideIncompatible) {
        this.hideIncompatible = hideIncompatible;
    }

    public Query.SortOption getSort() {
        return Query.SortOption.getOrDefault(this.sort);
    }

    public void setSort(Query.SortOption sort) {
        this.sort = sort.name();
    }

    public ResourcePacks getResourcepacks() {
        return this.resourcepacks;
    }

    public DataPacks getDatapacks() {
        return this.datapacks;
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    public abstract static sealed class Packs implements Serializable {
        private boolean replaceOriginal = true;
        private boolean hideIncompatibleWarnings = false;
        private final List<String> additionalFolders = new ObjectArrayList<>();
        private boolean rememberLastViewedProfile = false;
        private @Nullable String lastViewedProfile = null;
        private List<String> profileOrder = new ObjectArrayList<>();

        public abstract PackType packType();

        public boolean isLastViewedProfileRemembered() {
            return this.rememberLastViewedProfile;
        }

        public void setRememberLastViewedProfile(boolean rememberLastViewedProfile) {
            this.rememberLastViewedProfile = rememberLastViewedProfile;
        }

        List<String> getProfileOrder() {
            return this.profileOrder;
        }

        void setProfileOrder(List<String> profileOrder) {
            this.profileOrder = profileOrder;
        }

        @Nullable String getLastViewedProfile() {
            return this.lastViewedProfile;
        }

        void setLastViewedProfile(@Nullable Profile lastViewedProfile) {
            this.lastViewedProfile = lastViewedProfile != null ? lastViewedProfile.getId() : null;
        }

        public boolean isReplaceOriginal() {
            return this.replaceOriginal;
        }

        public void setReplaceOriginal(boolean replaceOriginal) {
            this.replaceOriginal = replaceOriginal;
        }

        public boolean isIncompatibleWarningsHidden() {
            return this.hideIncompatibleWarnings;
        }

        public void setHideIncompatibleWarnings(boolean hidden) {
            this.hideIncompatibleWarnings = hidden;
        }

        public List<String> getAdditionalFolders() {
            return List.copyOf(this.additionalFolders);
        }
    }

    public static final class DataPacks extends Packs {
        @Override
        public PackType packType() {
            return PackType.SERVER_DATA;
        }
    }

    public static final class ResourcePacks extends Packs {
        private boolean applyOnClose = true;

        @Override
        public PackType packType() {
            return PackType.CLIENT_RESOURCES;
        }

        public boolean isApplyOnClose() {
            return this.applyOnClose;
        }

        public void setApplyOnClose(boolean applyOnClose) {
            this.applyOnClose = applyOnClose;
        }
    }
}
