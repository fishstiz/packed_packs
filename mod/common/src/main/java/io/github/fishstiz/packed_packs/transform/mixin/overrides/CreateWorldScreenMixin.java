package io.github.fishstiz.packed_packs.transform.mixin.overrides;

import io.github.fishstiz.packed_packs.config.DevConfig;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.fidgetz.util.lang.CollectionsUtil;
import io.github.fishstiz.packed_packs.config.ProfileManager;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.WorldDataConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.List;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {
    @ModifyArg(method = "openCreateWorldScreen", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;createDefaultLoadConfig(Lnet/minecraft/server/packs/repository/PackRepository;Lnet/minecraft/world/level/WorldDataConfiguration;)Lnet/minecraft/server/WorldLoader$InitConfig;"
    ))
    private static WorldDataConfiguration applyDefaultProfile(WorldDataConfiguration worldDataConfiguration) {
        Profile defaultProfile = ProfileManager.serverData().getDefault();
        if (defaultProfile == null) return worldDataConfiguration;

        DataPackConfig dataPackConfig = worldDataConfiguration.dataPacks();
        List<String> disabled = CollectionsUtil.addAll(dataPackConfig.getDisabled(), dataPackConfig.getEnabled());
        List<String> enabled = defaultProfile.getPackIds().reversed();
        disabled.removeAll(enabled);

        return new WorldDataConfiguration(new DataPackConfig(enabled, disabled), worldDataConfiguration.enabledFeatures());
    }
}
