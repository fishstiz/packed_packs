package io.github.fishstiz.packed_packs.pack;

import io.github.fishstiz.packed_packs.config.FolderPackMeta;
import io.github.fishstiz.packed_packs.config.JsonLoader;
import io.github.fishstiz.packed_packs.gui.states.ProfileSelection;
import io.github.fishstiz.packed_packs.util.PackUtil;

import java.nio.file.Files;
import java.nio.file.Path;

public class PackResourcesService {
    private final PackNodeRepository repository;

    public PackResourcesService(PackNodeRepository repository) {
        this.repository = repository;
    }

    public boolean isModifiable(ProfileSelection profiles, PackNode pack) {
        return pack.path() != null &&
               !profiles.isLocked() &&
               !profiles.isPackRequired(pack) &&
               !repository.isEnabled(pack.id());
    }

    public boolean saveFolderMetadata(Path folderPath, FolderPackMeta metadata) {
        return JsonLoader.saveJson(metadata, folderPath.resolve(FolderPackMeta.FILENAME), false);
    }

    public boolean saveFolderMetadata(PackNode.Parent parent, FolderPackMeta metadata) {
        if (repository.setFolderMetadata(parent.id(), metadata)) {
            return saveFolderMetadata(parent.path(), metadata);
        }
        return false;
    }

    public boolean renamePack(PackNode pack, ProfileSelection profiles, String name) {
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
        if (pack instanceof PackNode.Parent) {
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
}
