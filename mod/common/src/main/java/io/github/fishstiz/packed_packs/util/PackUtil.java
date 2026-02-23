package io.github.fishstiz.packed_packs.util;

import com.sun.jna.platform.FileUtils;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.config.PackOptions;
import io.github.fishstiz.packed_packs.pack.PackGroup;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import io.github.fishstiz.packed_packs.pack.folder.FolderResources;
import io.github.fishstiz.packed_packs.platform.Services;
import io.github.fishstiz.packed_packs.transform.interfaces.FilePack;
import io.github.fishstiz.packed_packs.transform.mixin.UtilAccess;
import io.github.fishstiz.fidgetz.util.lang.CollectionsUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackDetector;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class PackUtil {
    public static final String HIGH_CONTRAST_ID = "high_contrast";
    public static final String VANILLA_ID = "vanilla";
    public static final String FABRIC_ID = "fabric";
    public static final String NEOFORGE_ID = "mod_resources";
    public static final String ZIP_PACK_EXTENSION = ".zip";
    public static final String ICON_FILENAME = "pack.png";
    public static final PackSource PACK_SOURCE = PackSource.create(name ->
            Component.translatable("pack.nameAndSource", name, ResourceUtil.getModName().withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.GRAY), false);

    // Changing these fields would be breaking changes
    private static final String FILE_PREFIX = "file/";
    private static final String DELIMITER = "/";

    private PackUtil() {
    }

    public static String fileName(Path path) {
        return path.getFileName().toString();
    }

    public static String generatePackName(Path path) {
        return fileName(path);
    }

    public static String generatePackId(String name) {
        return FILE_PREFIX + name;
    }

    public static String generateNestedPackId(Path path, String name) {
        return FILE_PREFIX + generatePackName(path.getParent()) + DELIMITER + name;
    }

    public static String generatePackId(Path path) {
        return generatePackId(generatePackName(path));
    }

    public static String generateNestedPackId(Path path) {
        return generateNestedPackId(path, generatePackName(path));
    }

    public static PackLocationInfo replicateLocationInfo(PackLocationInfo info, String id) {
        return new PackLocationInfo(id, info.title(), info.source(), info.knownPackInfo());
    }

    public static long getLastUpdatedEpochMs(Pack pack) {
        Path path = ((FilePack) pack).packed_packs$getPath();
        if (path == null) {
            return -1;
        }

        try {
            return Files.getLastModifiedTime(path).toInstant().toEpochMilli();
        } catch (IOException e) {
            PackedPacks.LOGGER.error("Failed to get age of pack '{}'", pack.getId());
            return -1;
        }
    }

    public static List<String> extractPackIds(Collection<Pack> packs) {
        return CollectionsUtil.extractNonNull(packs, Pack::getId);
    }

    public static String joinPackNames(Collection<Path> paths) {
        return String.join(", ", CollectionsUtil.extractNonNull(paths, PackUtil::generatePackName));
    }

    public static boolean hasMcmeta(Path path) {
        return Files.isRegularFile(path.resolve(PackResources.PACK_META), LinkOption.NOFOLLOW_LINKS);
    }

    public static boolean hasFolderConfig(Path path) {
        return Files.isRegularFile(path.resolve(FolderResources.FOLDER_CONFIG_FILENAME), LinkOption.NOFOLLOW_LINKS);
    }

    public static boolean isBuiltIn(Pack pack) {
        PackSource packSource = pack.getPackSource();
        return packSource == PackSource.BUILT_IN || Services.PLATFORM.isBuiltInPack(pack);
    }

    public static boolean isEssential(Pack pack) {
        String packId = pack.getId();
        return packId.equals(VANILLA_ID) || packId.equals(FABRIC_ID) || packId.equals(NEOFORGE_ID);
    }

    public static boolean isFeature(Pack pack) {
        return pack.getPackSource() == PackSource.FEATURE;
    }

    public static boolean isNonPackDirectory(Path path) {
        return Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) && !hasMcmeta(path);
    }

    public static boolean isZipPack(Pack pack) {
        Path path = validatePackPath(pack);
        return path != null && Files.isRegularFile(path) && PackUtil.fileName(path).endsWith(ZIP_PACK_EXTENSION);
    }

    public static List<Pack> flattenPacks(Collection<Pack> packs) {
        List<Pack> flattened = new ObjectArrayList<>(packs.size());
        for (Pack pack : packs) {
            if (pack instanceof FolderPack folderPack) {
                flattened.addAll(folderPack.flatten());
            } else {
                flattened.add(pack);
            }
        }
        return flattened;
    }

    public static List<String> flattenPackIds(Collection<Pack> packs) {
        List<String> flattened = new ObjectArrayList<>(packs.size());
        for (Pack pack : packs) {
            if (pack instanceof FolderPack folderPack) {
                flattened.addAll(extractPackIds(folderPack.flatten()));
            } else {
                flattened.add(pack.getId());
            }
        }
        return flattened;
    }

    public static Path validatePackPath(Pack pack) {
        if (pack == null) {
            return null;
        }
        Path path = ((FilePack) pack).packed_packs$getPath();
        if (path == null) {
            return null;
        }

        try {
            return Files.exists(path) ? path : null;
        } catch (Exception e) {
            PackedPacks.LOGGER.error("[packed_packs] Could not read file: '{}'", path);
            return null;
        }
    }

    public static List<Path> mapValidDirectories(Collection<String> paths) {
        if (paths == null || paths.isEmpty()) return Collections.emptyList();

        List<Path> validPaths = new ObjectArrayList<>(paths.size());
        for (String path : paths) {
            if (path == null || path.isBlank()) continue;

            try {
                Path resolved = Paths.get(path);
                if (Files.isDirectory(resolved, LinkOption.NOFOLLOW_LINKS)) {
                    validPaths.add(resolved.toAbsolutePath().normalize());
                } else {
                    PackedPacks.LOGGER.error("[packed_packs] Path is not a valid directory: '{}', ignoring.", path);
                }
            } catch (Exception e) {
                PackedPacks.LOGGER.error("[packed_packs] Failed to resolve path: '{}', ignoring.", path, e);
            }
        }

        return validPaths;
    }

    public static void openPack(Pack pack) {
        var path = ((FilePack) pack).packed_packs$getPath();
        if (path != null) {
            Util.getPlatform().openPath(path);
        }
    }

    public static void openParent(Pack pack) {
        var path = ((FilePack) pack).packed_packs$getPath();
        if (path != null) {
            PackUtil.openParent(path);
        }
    }

    public static void openParent(Path path) {
        File file = path.toFile();
        if (!file.exists()) return;

        try {
            switch (Util.getPlatform()) {
                case WINDOWS -> new ProcessBuilder("explorer.exe", "/select,", file.getAbsolutePath()).start();
                case OSX -> new ProcessBuilder("open", "-R", file.getAbsolutePath()).start();
                case LINUX -> {
                    File parentFile = file.getParentFile();
                    if (parentFile != null) new ProcessBuilder("xdg-open", parentFile.getAbsolutePath()).start();
                }
                default -> {
                    Path parent = path.getParent();
                    if (parent != null) Util.getPlatform().openPath(parent);
                }
            }
        } catch (IOException e) {
            Path parent = path.getParent();
            if (parent != null) Util.getPlatform().openPath(parent);
        }
    }

    public static boolean deletePath(Path path) {
        FileUtils fileUtils = FileUtils.getInstance();

        if (fileUtils.hasTrash()) {
            try {
                fileUtils.moveToTrash(path.toFile());
                return true;
            } catch (IOException e) {
                PackedPacks.LOGGER.warn("[packed_packs] Failed to move to trash: '{}'", path, e);
            }
        }

        if (Files.isDirectory(path)) {
            try {
                org.apache.commons.io.FileUtils.deleteDirectory(path.toFile());
                return true;
            } catch (IOException e) {
                PackedPacks.LOGGER.error("[packed_packs] Failed to delete path: '{}'", path, e);
                return false;
            }
        }

        return UtilAccess.packed_packs$createDeleter(path).getAsBoolean();
    }

    public static boolean renamePath(Path path, Path newName) {
        return UtilAccess.packed_packs$createRenamer(path, newName).getAsBoolean();
    }

    public static String getNewIdOnRename(Pack pack, String newName) {
        FilePack filePack = (FilePack) pack;
        Path path = filePack.packed_packs$getPath();
        if (path == null) return pack.getId();

        return filePack.packed_packs$nestedPack() ? generateNestedPackId(path, newName) : generatePackId(newName);
    }

    public static PathValidationResults validatePaths(List<Path> packs) {
        PackDetector<@NonNull Path> packDetector = new PackDetector<>(Minecraft.getInstance().directoryValidator()) {
            @Override
            protected Path createZipPack(@NonNull Path path) {
                return path;
            }

            @Override
            protected Path createDirectoryPack(@NonNull Path path) {
                return path;
            }
        };

        PathValidationResults results = new PathValidationResults(packs);
        for (Path path : packs) {
            try {
                if (!isNonPackDirectory(path)) {
                    if (validatePath(path, packDetector, results.symlinkWarnings)) {
                        results.addValid(path);
                    }
                    continue;
                }

                try (DirectoryStream<Path> paths = Files.newDirectoryStream(path)) {
                    for (Path child : paths) {
                        if (validatePath(child, packDetector, results.symlinkWarnings)) {
                            results.addValid(path);
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                PackedPacks.LOGGER.warn("Failed to check {} for packs", path, e);
            }
        }

        return results;
    }

    private static boolean validatePath(Path path, PackDetector<@NonNull Path> packDetector, List<ForbiddenSymlinkInfo> symlinkWarnings) throws IOException {
        Path detectedPack = packDetector.detectPackResources(path, symlinkWarnings);
        if (detectedPack == null) {
            PackedPacks.LOGGER.warn("Path {} does not seem like pack", path);
            return false;
        }
        return true;
    }

    public record PathValidationResults(
            List<Path> valid,
            Set<Path> rejected,
            List<ForbiddenSymlinkInfo> symlinkWarnings
    ) {
        private PathValidationResults(Collection<Path> packs) {
            this(new ArrayList<>(packs.size()), new ObjectOpenHashSet<>(packs), new ArrayList<>());
        }

        private void addValid(Path path) {
            this.valid.add(path);
            this.rejected.remove(path);
        }
    }

    public static PackGroup syncPackSelection(ObjectOpenHashSet<Pack> allPacks, List<Pack> unselectedPacks, List<Pack> selectedPacks, PackOptions options) {
        Set<Pack> seen = new ObjectOpenHashSet<>(allPacks.size());

        List<Pack> newSelectedPacks = new ObjectArrayList<>(selectedPacks.size());
        for (Pack pack : selectedPacks) {
            Pack validPack = allPacks.get(pack);
            if (validPack != null && seen.add(validPack)) {
                newSelectedPacks.add(validPack);
            }
        }

        List<Pack> newUnselectedPacks = new ObjectArrayList<>(unselectedPacks.size());
        for (Pack unselectedPack : unselectedPacks) {
            // use pack from the master list as the metadata may have updated
            Pack pack = allPacks.get(unselectedPack);
            if (pack != null && seen.add(pack)) {
                if (options.isRequired(pack)) {
                    options.getPosition(pack).insert(newSelectedPacks, pack, options::getSelectionConfig, true);
                } else {
                    newUnselectedPacks.add(pack);
                }
            }
        }

        for (Pack pack : allPacks) {
            if (seen.add(pack)) {
                if (options.isRequired(pack)) {
                    options.getPosition(pack).insert(newSelectedPacks, pack, options::getSelectionConfig, true);
                } else {
                    newUnselectedPacks.add(pack);
                }
            }
        }

        return new PackGroup(newSelectedPacks, newUnselectedPacks);
    }
}
