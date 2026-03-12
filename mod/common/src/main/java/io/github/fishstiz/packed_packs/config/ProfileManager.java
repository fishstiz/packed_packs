package io.github.fishstiz.packed_packs.config;

import io.github.fishstiz.fidgetz.util.lang.CollectionsUtil;
import io.github.fishstiz.fidgetz.util.lang.FunctionsUtil;
import io.github.fishstiz.packed_packs.PackedPacks;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.FileUtil;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import static io.github.fishstiz.packed_packs.config.JsonLoader.loadJsonOrDefault;
import static io.github.fishstiz.packed_packs.config.JsonLoader.saveJson;

public final class ProfileManager {
    private static final ProfileManager RESOURCE_PACK_PROFILES = new ProfileManager(PackType.CLIENT_RESOURCES);
    private static final ProfileManager DATA_PACK_PROFILES = new ProfileManager(PackType.SERVER_DATA);
    private static final String PROFILE_EXTENSION = ".profile.json";
    private static final String PROFILE_EXTENSION_QUOTE = Pattern.quote(PROFILE_EXTENSION);
    private static final String PROFILE_DIR = "profiles";
    private static final String RESOURCE_PACK_DIR = "resourcepacks";
    private static final String DATA_PACK_DIR = "datapacks";
    private static final int NAME_MAX_LENGTH = 32;
    private final PackType packType;
    private SequencedMap<String, Profile> profiles;
    private Profile defaultProfile;
    private Profile lastViewed;

    private ProfileManager(PackType packType) {
        this.packType = packType;
    }

    public static ProfileManager get(PackType type) {
        return switch (type) {
            case CLIENT_RESOURCES -> RESOURCE_PACK_PROFILES;
            case SERVER_DATA -> DATA_PACK_PROFILES;
        };
    }

    public static ProfileManager clientResources() {
        return RESOURCE_PACK_PROFILES;
    }

    public static ProfileManager serverData() {
        return DATA_PACK_PROFILES;
    }

    private static Path getProfileDir(PackType packType) {
        return PackedPacks.getConfigDir().resolve(PROFILE_DIR).resolve(switch (packType) {
            case CLIENT_RESOURCES -> RESOURCE_PACK_DIR;
            case SERVER_DATA -> DATA_PACK_DIR;
        });
    }

    private static String removeExtension(String id) {
        return id.replaceFirst(PROFILE_EXTENSION_QUOTE + "$", "");
    }

    private static Path getFile(Path saveFolder, String id) {
        return saveFolder.resolve(id + PROFILE_EXTENSION);
    }

    private static Path getFile(PackType packType, String id) {
        return getFile(getProfileDir(packType), id);
    }

    private static ObjectArrayList<Profile> getAllProfiles(PackType type) {
        Path dir = getProfileDir(type);
        if (!Files.isDirectory(dir)) {
            return new ObjectArrayList<>();
        }

        List<CompletableFuture<Profile>> futures = new ObjectArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path file : stream) {
                if (file.getFileName().toString().endsWith(PROFILE_EXTENSION)) {
                    futures.add(CompletableFuture.supplyAsync(() -> {
                        String id = removeExtension(file.getFileName().toString());
                        Profile profile = loadJsonOrDefault(file, Profile.class, () -> new Profile(id));
                        profile.id = id;
                        return profile;
                    }, Util.backgroundExecutor()));
                }
            }
        } catch (IOException e) {
            PackedPacks.LOGGER.error("[packed_packs] Failed to fetch profiles at {}. ", dir, e);
        }

        if (futures.isEmpty()) {
            return new ObjectArrayList<>();
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        return CollectionsUtil.map(futures, CompletableFuture::join, ObjectArrayList::new);
    }

    private static @Nullable Profile getProfile(String id, PackType type) {
        Profile profile = loadJsonOrDefault(getFile(getProfileDir(type), id), Profile.class, FunctionsUtil.nullSupplier());
        if (profile != null) profile.id = id;
        return profile;
    }

    private static String findAvailableId(Path saveFolder, String name) {
        try {
            return removeExtension(FileUtil.findAvailableName(saveFolder, name, PROFILE_EXTENSION));
        } catch (IOException e) {
            return name + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss.SSS"));
        }
    }

    private static String trimName(String name) {
        if (name == null) return "";
        return name.length() <= NAME_MAX_LENGTH ? name : name.substring(0, NAME_MAX_LENGTH);
    }

    public static int getNameMaxLength() {
        return NAME_MAX_LENGTH;
    }

    private @Nullable Profile get(String id) {
        if (this.profiles != null) {
            Profile cached = this.profiles.get(id);
            if (cached != null) return cached;
        }

        Profile profile = getProfile(id, this.packType);
        if (profile != null) {
            profile.id = id;
            if (this.profiles != null) this.profiles.put(id, profile);
        }

        return profile;
    }

    public @Nullable Profile getDefault() {
        Profile localDefault = this.defaultProfile;
        if (localDefault != null) return localDefault;

        String id = DevConfig.packs(this.packType).getDefaultProfile();
        if (id == null) return null;

        Profile fetched = this.get(id);
        if (fetched == null) {
            DevConfig.packs(this.packType).setDefaultProfile(null);
        } else {
            this.defaultProfile = fetched;
        }

        return fetched;
    }

    public @Nullable Profile getLastViewed() {
        Profile localLast = this.lastViewed;
        if (localLast != null) return localLast;

        String id = Config.packs(this.packType).getLastViewedProfile();
        if (id == null) return null;

        Profile fetched = this.get(id);
        if (fetched == null) {
            Config.packs(this.packType).setLastViewedProfile(null);
        } else {
            this.lastViewed = fetched;
        }

        return fetched;
    }

    public Profile create(String name) {
        String trimmed = trimName(name);
        String id = findAvailableId(getProfileDir(this.packType), trimmed);
        Profile profile = new Profile(id);
        profile.temp = true;
        return profile;
    }

    public Profile copy(Profile profile) {
        if (profile.temp) this.save(profile);
        String cleanName = profile.getName().replaceAll("\\s\\(\\d+\\)$", "");
        String id = findAvailableId(getProfileDir(this.packType), cleanName);
        Profile copy = profile.copy(id);
        copy.temp = true;
        return copy;
    }

    public boolean save(Profile profile) {
        if (saveJson(profile, getFile(this.packType, profile.getId()))) {
            profile.temp = false;
            Map<String, Profile> map = this.profiles;
            if (map != null) map.putIfAbsent(profile.getId(), profile);
            return true;
        }
        return false;
    }

    public void setOrder(List<Profile> profiles) {
        Config.packs(this.packType).setProfileOrder(CollectionsUtil.map(profiles, Profile::getId, ObjectArrayList::new));
    }

    public void setDefault(@Nullable Profile profile) {
        DevConfig.packs(this.packType).setDefaultProfile(profile);
        this.defaultProfile = profile;
    }

    public void setLastViewed(@Nullable Profile profile) {
        Config.packs(this.packType).setLastViewedProfile(profile);
        this.lastViewed = profile;
    }

    public boolean delete(Profile profile) {
        Path file = getFile(this.packType, profile.getId());
        try {
            Files.deleteIfExists(file);

            Config.Packs config = Config.packs(this.packType);
            DevConfig.Packs metaConfig = DevConfig.packs(this.packType);

            if (Objects.equals(profile.getId(), metaConfig.getDefaultProfile())) {
                metaConfig.setDefaultProfile(null);
                this.defaultProfile = null;
            }
            if (Objects.equals(profile.getId(), config.getLastViewedProfile())) {
                config.setLastViewedProfile(null);
                this.lastViewed = null;
            }

            Map<String, Profile> map = this.profiles;
            if (map != null) {
                map.remove(profile.getId());
            }

            return true;
        } catch (IOException e) {
            PackedPacks.LOGGER.error("[packed_packs] Failed to delete profile at {}", file, e);
            return false;
        }
    }

    public void rename(Profile profile, String name) {
        if (profile.isLocked()) return;

        String trimmed = trimName(name);
        profile.setName(trimmed);
        if (profile.temp) {
            profile.id = findAvailableId(getProfileDir(this.packType), trimmed);
        }
    }

    public List<Profile> getProfiles() {
        if (this.profiles != null) return List.copyOf(this.profiles.sequencedValues());

        ObjectArrayList<Profile> profiles = getAllProfiles(this.packType);
        List<String> order = Config.packs(this.packType).getProfileOrder();
        Map<String, Integer> orderMap = new Object2IntOpenHashMap<>(order.size());
        for (int i = 0; i < order.size(); i++) {
            orderMap.put(order.get(i), i);
        }

        profiles.sort(Comparator.<Profile>comparingInt(profile ->
                orderMap.containsKey(profile.getId()) ? orderMap.get(profile.getId()) + orderMap.size() : 0
        ).thenComparing(Profile::getName));

        SortedMap<String, Profile> profileMap = new Object2ObjectLinkedOpenHashMap<>(profiles.size());

        Profile lastViewed = this.lastViewed;
        Profile defaultProfile = this.defaultProfile;

        for (Profile profile : profiles) {
            if (Objects.equals(profile, defaultProfile)) {
                profileMap.put(profile.getId(), defaultProfile);
            } else if (Objects.equals(profile, lastViewed)) {
                profileMap.put(profile.getId(), lastViewed);
            } else {
                profileMap.put(profile.getId(), profile);
            }
        }

        this.profiles = profileMap;
        return List.copyOf(profileMap.sequencedValues());
    }

    public void remapAndSavePackIds(String oldPackId, String newPackId) {
        Profile defaultProfile = this.getDefault();
        if (defaultProfile != null && defaultProfile.remapPackId(oldPackId, newPackId)) {
            this.save(defaultProfile);
        }
        for (Profile profile : this.getProfiles()) {
            if (!Objects.equals(profile, defaultProfile) && profile.remapPackId(oldPackId, newPackId)) {
                this.save(profile);
            }
        }
    }
}
