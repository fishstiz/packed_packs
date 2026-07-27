package io.github.fishstiz.packed_packs.transform.mixin.folders.additional;

import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(PackRepository.class)
public interface PackRepositoryAccessor {
    @Accessor("sources")
    Set<RepositorySource> packed_packs$getSources();
}
