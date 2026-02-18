package io.github.fishstiz.packed_packs.compat.resourcify;

import io.github.fishstiz.packed_packs.api.PackedPacksApi;
import io.github.fishstiz.packed_packs.api.events.ScreenEvent;
import io.github.fishstiz.packed_packs.compat.ModIntegration;
import io.github.fishstiz.packed_packs.compat.Mod;
import io.github.fishstiz.packed_packs.compat.ModContext;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import net.minecraft.client.gui.components.Button;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class ResourcifyIntegration implements ModIntegration {
    @Override
    public ModContext mod() {
        return Mod.RESOURCIFY;
    }

    @Override
    public void onInitialize(@NonNull PackedPacksApi api) {
        if (!this.mod().isLoaded()) return;

        api.eventBus().register(
                ScreenEvent.InitLayout.class,
                this.id(),
                List.of(ModIntegration.id(Mod.ETF), ResourceUtil.id("vt_downloader")),
                event -> {
                    List<? extends Button> buttons = ResourcifyButtons.getButtons(event.ctx().getOriginalScreen());
                    if (buttons != null) {
                        for (Button button : buttons.reversed()) {
                            event.addElement(ScreenEvent.InitLayout.Phase.AFTER_HEADER_TITLE, button);
                        }
                    }
                }
        );
    }
}
