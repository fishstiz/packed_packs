package io.github.fishstiz.packed_packs.compat.respackopts;

import io.github.fishstiz.packed_packs.api.PackedPacksApi;
import io.github.fishstiz.packed_packs.api.Preference;
import io.github.fishstiz.packed_packs.api.events.*;
import io.github.fishstiz.packed_packs.compat.ModIntegration;
import io.github.fishstiz.packed_packs.compat.Mod;
import io.github.fishstiz.packed_packs.compat.ModContext;
import io.github.fishstiz.packed_packs.util.ResourceUtil;

public class RespackoptsIntegration extends ModIntegration {
    @Override
    public ModContext mod() {
        return Mod.RESPACKOPTS;
    }

    @Override
    protected void onInitLoaded(PackedPacksApi api) {
        Preference<Boolean> respackoptsButton = api.preferences().register(ResourceUtil.id("respackopts_button"), true);

        api.eventBus().register(InitializePackEntryEvent.class, this.id(), event -> {
            if (!event.screenContext().isClientResources()) return;
            if (event.screenContext().devMode() || respackoptsButton.get()) {
                event.addDetachedWidget(container -> RespackoptsWidget.create(respackoptsButton, container, event.packContext().pack()));
            }
        });

        api.eventBus().register(ContextMenuEvent.Preferences.class, this.id(), id(Mod.ETF), event -> {
            if (event.screenContext().isClientResources()) {
                event.addToggle(respackoptsButton, getWidgetPrefText(respackoptsButton));
            }
        });

        api.eventBus().register(ClosingEvent.class, this.id(), event -> {
            if (event.screenContext().isClientResources() && RespackoptsUtil.isForceReload()) {
                event.commit();
            }
        });

        api.eventBus().register(WatchEvent.class, this.id(), event -> {
            if (event.screenContext().isClientResources() && RespackoptsUtil.isRespackOptsFile(event.getPath())) {
                event.cancel();
            }
        });
    }
}
