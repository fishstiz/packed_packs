package io.github.fishstiz.packed_packs.gui.services;

import io.github.fishstiz.packed_packs.config.FolderPackMeta;
import io.github.fishstiz.packed_packs.config.JsonLoader;
import io.github.fishstiz.packed_packs.pack.PackEntry;
import io.github.fishstiz.packed_packs.gui.states.ProfileSelection;
import io.github.fishstiz.packed_packs.util.PackUtil;
import net.minecraft.resources.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;

public class PackResourcesService {
    private final PackRepositoryService repository;
    private final PackIconCache iconCache;

    public PackResourcesService(PackRepositoryService repository, PackIconCache iconCache) {
        this.repository = repository;
        this.iconCache = iconCache;
    }

    public Identifier getIcon(PackEntry pack) {
        return iconCache.get(pack);
    }

    public void clearIcons() {
        iconCache.clear();
    }

    public boolean isModifiable(ProfileSelection profiles, PackEntry pack) {
        return pack.path() != null &&
               !profiles.isLocked() &&
               !profiles.isPackRequired(pack) &&
               !repository.isEnabled(pack.id());
    }

    public boolean saveFolderMetadata(Path folderPath, FolderPackMeta metadata) {
        return JsonLoader.saveJson(metadata, folderPath.resolve(FolderPackMeta.FILENAME));
    }

    public boolean saveFolderMetadata(PackEntry.Parent parent, FolderPackMeta metadata) {
        if (repository.setFolderMetadata(parent.id(), metadata)) {
            return saveFolderMetadata(parent.path(), metadata);
        }
        return false;
    }

    public boolean renamePack(PackEntry pack, ProfileSelection profiles, String name) {
        if (!isModifiable(profiles, pack)) {
            return false;
        }

        Path path = pack.path();
        if (path == null || !Files.exists(path)) {
            return false;
        }

        Path newPath = path.getParent().resolve(name);
        if (!PackUtil.renamePath(path, path.getParent().resolve(name))) {
            return false;
        }

        String newId = PackUtil.generatePackId(newPath);
        if (pack instanceof PackEntry.Parent) {
            FolderPackMeta metadata = repository.getFolderMetadata(pack.id());
            if (metadata != null) {
                if (repository.setFolderMetadata(newId, metadata)) {
                    saveFolderMetadata(newPath, metadata);
                }
                repository.removeFolderMetadata(pack.id());
            }
        }

        return true;
    }

    public boolean deletePack(PackEntry pack, ProfileSelection profiles) {
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
}
