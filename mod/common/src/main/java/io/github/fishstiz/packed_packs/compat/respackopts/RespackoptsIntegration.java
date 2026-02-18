package io.github.fishstiz.packed_packs.compat.respackopts;

import io.github.fishstiz.packed_packs.api.PackedPacksApi;
import io.github.fishstiz.packed_packs.api.PreferenceRegistry;
import io.github.fishstiz.packed_packs.api.events.ScreenEvent;
import io.github.fishstiz.packed_packs.compat.ModIntegration;
import io.github.fishstiz.packed_packs.compat.Mod;
import io.github.fishstiz.packed_packs.compat.ModContext;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import net.minecraft.server.packs.PackType;
import org.jspecify.annotations.NonNull;

public class RespackoptsIntegration implements ModIntegration {
    @Override
    public ModContext mod() {
        return Mod.RESPACKOPTS;
    }

    @Override
    public void onInitialize(@NonNull PackedPacksApi api) {
        if (!this.mod().isLoaded()) return;

        PreferenceRegistry.Key<Boolean> respackOptsPrefKey = api.preferences().register(ResourceUtil.id("respackopts_button"), true);

        api.eventBus().register(ScreenEvent.InitPackEntry.class, this.id(), event -> {
            if (event.ctx().getPackType() != PackType.CLIENT_RESOURCES) return;

            if (event.ctx().isDevMode() || Boolean.TRUE.equals(api.preferences().get(respackOptsPrefKey))) {
                RespackoptsWidget widget = RespackoptsWidget.create(respackOptsPrefKey, event.getContainer(), event.getPack());
                if (widget != null) event.addWidget(widget);
            }
        });

        api.eventBus().register(ScreenEvent.OpenCtxMenu.class, this.id(), ModIntegration.id(Mod.ETF), event -> {
            if (event.ctx().getPackType() != PackType.CLIENT_RESOURCES) return;

            ModIntegration.addPreferenceToggle(event, api.preferences(), respackOptsPrefKey);
        });

        api.eventBus().register(ScreenEvent.Closing.class, this.id(), event -> {
            if (event.ctx().getPackType() != PackType.CLIENT_RESOURCES) return;

            if (RespackoptsUtil.isForceReload()) {
                event.commit();
            }
        });

        api.eventBus().register(ScreenEvent.FileWatch.class, this.id(), event -> {
            if (event.ctx().getPackType() != PackType.CLIENT_RESOURCES) return;

            if (RespackoptsUtil.isRespackOptsFile(event.getPath())) {
                event.cancel();
            }
        });
    }
}
