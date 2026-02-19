package io.github.fishstiz.packed_packs.compat.vtdownloader;

import io.github.fishstiz.packed_packs.api.PackedPacksApi;
import io.github.fishstiz.packed_packs.api.PreferenceRegistry;
import io.github.fishstiz.packed_packs.api.events.ScreenEvent;
import io.github.fishstiz.packed_packs.compat.FabricMod;
import io.github.fishstiz.packed_packs.compat.Mod;
import io.github.fishstiz.packed_packs.compat.ModContext;
import io.github.fishstiz.packed_packs.compat.ModIntegration;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import net.minecraft.server.packs.PackType;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class VTDIntegration implements ModIntegration {
    @Override
    public ModContext mod() {
        return FabricMod.VTD;
    }

    @Override
    public void onInitialize(@NonNull PackedPacksApi api) {
        if (!this.mod().isLoaded()) return;

        PreferenceRegistry.Key<Boolean> vtdButtonPrefKey = api.preferences().register(ResourceUtil.id("vtd_button"), true);
        PreferenceRegistry.Key<Boolean> vtdEditButtonPrefKey = api.preferences().register(ResourceUtil.id("vtd_edit_button"), true);

        api.eventBus().register(ScreenEvent.InitLayout.class, this.id(), ModIntegration.id(Mod.ETF), event -> {
            if (event.ctx().getPackType() != PackType.CLIENT_RESOURCES) return;

            if (event.ctx().isDevMode() || Boolean.TRUE.equals(api.preferences().get(vtdButtonPrefKey))) {
                event.addElement(
                        ScreenEvent.InitLayout.Phase.AFTER_HEADER_TITLE,
                        VTDButtonFactory.create(vtdButtonPrefKey, event.ctx().getScreen())
                );
            }
        });

        api.eventBus().register(ScreenEvent.InitPackEntry.class, this.id(), event -> {
            if (event.ctx().getPackType() != PackType.CLIENT_RESOURCES) return;

            if (event.ctx().isDevMode() || Boolean.TRUE.equals(api.preferences().get(vtdEditButtonPrefKey))) {
                VTDEditButtonWidget widget = VTDEditButtonWidget.create(
                        vtdEditButtonPrefKey,
                        event.ctx().getScreen(),
                        event.getContainer(),
                        event.getPack(),
                        !event.isFileLocked()
                );
                if (widget != null) event.addWidget(widget);
            }
        });

        api.eventBus().register(
                ScreenEvent.OpenCtxMenu.class,
                this.id(),
                List.of(ModIntegration.id(Mod.RESPACKOPTS), ModIntegration.id(Mod.ETF)),
                event -> {
                    event.addPreferenceToggle(api.preferences(), vtdButtonPrefKey, ModIntegration.getWidgetPrefText(vtdButtonPrefKey));
                    event.addPreferenceToggle(api.preferences(), vtdEditButtonPrefKey, ModIntegration.getWidgetPrefText(vtdEditButtonPrefKey));
                }
        );
    }
}
