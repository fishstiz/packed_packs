package io.github.fishstiz.packed_packs.pack;

import io.github.fishstiz.packed_packs.config.FolderPackMeta;
import io.github.fishstiz.packed_packs.config.JsonLoader;
import io.github.fishstiz.packed_packs.config.ProfileManager;
import io.github.fishstiz.packed_packs.gui.states.ProfileSelection;
import io.github.fishstiz.packed_packs.util.PackUtil;
import net.minecraft.server.packs.PackType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PackResourcesService {
    private final PackNodeRepository repository;
    private final PackType packType;

    public PackResourcesService(PackType packType, PackNodeRepository repository) {
        this.repository = repository;
        this.packType = packType;
    }

    public boolean isModifiable(ProfileSelection profiles, PackNode pack) {
        return pack.path() != null &&
               !profiles.isLocked() &&
               !profiles.isPackRequired(pack) &&
               !repository.isEnabled(pack.id());
    }

    public boolean setFolderMetadata(PackNode.Parent parent, FolderPackMeta metadata) {
        return repository.setFolderMetadata(parent.id(), metadata);
    }

    public boolean saveFolderMetadata(Path folderPath, FolderPackMeta metadata) {
        return JsonLoader.saveJson(metadata, folderPath.resolve(FolderPackMeta.FILENAME), false);
    }

    public boolean saveFolderMetadata(PackNode.Parent parent, FolderPackMeta metadata) {
        if (setFolderMetadata(parent, metadata)) {
            return saveFolderMetadata(parent.path(), metadata);
        }
        return false;
    }

    public void saveFolderMetadata(PackNode.Parent parent) {
        saveFolderMetadata(parent.path(), repository.getFolderMetadata(parent));
    }

    public boolean renamePack(PackNode pack, ProfileSelection profiles, String name) {
        if (!isModifiable(profiles, pack)) {
            return false;
        }

        Path path = pack.path();
        if (path == null || !Files.exists(path)) {
            return false;
        }

        PackNode.Parent parentNode = null;
        if (pack.parentId() != null && repository.getPackById(pack.parentId()) instanceof PackNode.Parent parent) {
            parentNode = parent;
        }

        Path newPath = path.getParent().resolve(name);
        if (!PackUtil.renamePath(path, path.getParent().resolve(name))) {
            return false;
        }

        String newId = PackUtil.getNewIdOnRename(pack.id(), name);
        if (parentNode != null) {
            FolderPackMeta parentMeta = repository.getFolderMetadata(parentNode.id());
            if (parentMeta != null) {
                boolean matchRelative = false;
                String oldRelativeId = PackUtil.replaceDirsWithRelative(pack.id());
                int index = -1;

                for (int i = 0; i < parentMeta.packIds().size(); i++) {
                    String packId = parentMeta.packIds().get(i);
                    matchRelative = packId.equals(oldRelativeId);
                    if (matchRelative || packId.equals(pack.id())) {
                        index = i;
                        break;
                    }
                }

                if (index != -1) {
                    List<String> packIds = new ArrayList<>(parentMeta.packIds());
                    packIds.set(index, matchRelative ? PackUtil.replaceDirsWithRelative(newId) : newId);
                    saveFolderMetadata(parentNode, new FolderPackMeta(parentMeta.module(), packIds));
                }
            }
        }

        if (pack instanceof PackNode.Parent parent) {
            FolderPackMeta metadata = repository.getFolderMetadata(pack.id());
            String regex = "^" + Pattern.quote(pack.id());

            if (metadata != null) {
                saveFolderMetadata(newPath, metadata.withPackIds(metadata.packIds().stream()
                        .map(id -> id.replaceFirst(regex, newId))
                        .toList()));

                repository.removeFolderMetadata(pack.id());
            }

            parent.visitNodes(node -> ProfileManager.get(packType).remapAndSavePackIds(
                    node.id(),
                    node.id().replaceFirst("^" + Pattern.quote(pack.id()), newId)
            ));
        }

        return true;
    }

    public boolean deletePack(PackNode pack, ProfileSelection profiles) {
        if (!isModifiable(profiles, pack)) {
            return false;
        }

        Path path = pack.path();
        if (path == null || !PackUtil.deletePath(path)) {
            return false;
        }

        repository.removePack(pack.id());
        return true;
    }

    public static List<String> replaceDirsWithRelative(List<PackNode> packs) {
        return packs.stream()
                .map(pack -> pack.parentId() == null ? pack.id() : PackUtil.replaceDirsWithRelative(pack.id()))
                .toList();
    }
}
