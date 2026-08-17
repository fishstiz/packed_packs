package io.github.fishstiz.packed_packs.gui2.services;

import io.github.fishstiz.packed_packs.gui2.models.PackEntry;
import io.github.fishstiz.packed_packs.gui2.models.ProfileSelection;
import net.minecraft.resources.Identifier;
// todo can probably just combine both
public class PackResourcesService {
    private final PackRepositoryService repositoryService;
    private final PackIconCache iconCache;

    public PackResourcesService(PackRepositoryService repositoryService, PackIconCache iconCache) {
        this.repositoryService = repositoryService;
        this.iconCache = iconCache;
    }

    public Identifier getIcon(PackEntry pack) {
        return iconCache.get(pack);
    }

    public boolean isModifiable(ProfileSelection profile, PackEntry pack) {
        return !profile.isLocked() &&
               !profile.isPackRequired(pack) &&
               pack.path() != null &&
               !repositoryService.isEnabled(pack.id());
    }
}
