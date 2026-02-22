package io.github.fishstiz.packed_packs.compat.resourcify;

import io.github.fishstiz.packed_packs.api.PackedPacksApi;
import io.github.fishstiz.packed_packs.api.events.InitializeLayoutEvent;
import io.github.fishstiz.packed_packs.compat.ModIntegration;
import io.github.fishstiz.packed_packs.compat.Mod;
import io.github.fishstiz.packed_packs.compat.ModContext;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import net.minecraft.client.gui.components.Button;

import java.util.List;

public class ResourcifyIntegration extends ModIntegration {
    @Override
    public ModContext mod() {
        return Mod.RESOURCIFY;
    }

    @Override
    protected void onInitLoaded(PackedPacksApi api) {
        api.eventBus().register(
                InitializeLayoutEvent.class,
                this.id(),
                List.of(ModIntegration.id(Mod.ETF), ResourceUtil.id("vt_downloader")),
                event -> {
                    List<? extends Button> buttons = ResourcifyButtons.getButtons(event.screenContext().originalScreen());
                    if (buttons != null) {
                        for (Button button : buttons.reversed()) {
                            event.addWidget(InitializeLayoutEvent.Pos.AFTER_TITLE, button);
                        }
                    }
                }
        );
    }
}
