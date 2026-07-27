package io.github.fishstiz.packed_packs.transform.mixin.folders.additional;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.util.PackUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.world.level.validation.DirectoryValidator;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    @Final
    private DirectoryValidator directoryValidator;

    @WrapOperation(method = "<init>", at = @At(
            value = "NEW",
            target = "([Lnet/minecraft/server/packs/repository/RepositorySource;)Lnet/minecraft/server/packs/repository/PackRepository;")
    )
    private PackRepository addAdditionalFolders(RepositorySource[] sources, Operation<PackRepository> original) {
        RepositorySource[] folders = PackUtil.mapValidDirectories(Config.get().getResourcepacks().getAdditionalFolders())
                .stream()
                .map(path -> path.toAbsolutePath().normalize())
                .distinct()
                .map(path -> new FolderRepositorySource(path, PackType.CLIENT_RESOURCES, PackSource.DEFAULT, this.directoryValidator))
                .toArray(RepositorySource[]::new);

        return original.call((Object) ArrayUtils.addAll(sources, folders));
    }
}
