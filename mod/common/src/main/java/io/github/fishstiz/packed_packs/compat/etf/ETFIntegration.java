package io.github.fishstiz.packed_packs.compat.etf;

import io.github.fishstiz.packed_packs.api.PackedPacksApi;
import io.github.fishstiz.packed_packs.api.PreferenceRegistry;
import io.github.fishstiz.packed_packs.api.events.ScreenEvent;
import io.github.fishstiz.packed_packs.compat.ModIntegration;
import io.github.fishstiz.packed_packs.compat.Mod;
import io.github.fishstiz.packed_packs.compat.ModContext;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import net.minecraft.server.packs.PackType;
import org.jspecify.annotations.NonNull;

public class ETFIntegration implements ModIntegration {
    @Override
    public ModContext mod() {
        return Mod.ETF;
    }

    @Override
    public void onInitialize(@NonNull PackedPacksApi api) {
        if (!this.mod().isLoaded()) return;

        PreferenceRegistry.Key<Boolean> etfButtonPrefKey = api.preferences().register(ResourceUtil.id("etf_button"), true);

        api.eventBus().register(ScreenEvent.InitLayout.class, this.id(), event -> {
            if (event.ctx().getPackType() != PackType.CLIENT_RESOURCES) return;

            if (event.ctx().isDevMode() || Boolean.TRUE.equals(api.preferences().get(etfButtonPrefKey))) {
                event.addElement(
                        ScreenEvent.InitLayout.Phase.AFTER_HEADER_TITLE,
                        ETFButtonFactory.create(etfButtonPrefKey, event.ctx().getScreen())
                );
            }
        });

        api.eventBus().register(ScreenEvent.OpenCtxMenu.class, this.id(), event -> {
            if (event.ctx().getPackType() != PackType.CLIENT_RESOURCES) return;

            ModIntegration.addPreferenceToggle(event, api.preferences(), etfButtonPrefKey);
        });
    }
}
