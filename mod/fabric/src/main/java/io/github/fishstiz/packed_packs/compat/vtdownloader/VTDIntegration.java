package io.github.fishstiz.packed_packs.compat.vtdownloader;

import io.github.fishstiz.fidgetz.gui.components.FidgetzButton;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.packed_packs.api.PackedPacksApi;
import io.github.fishstiz.packed_packs.api.PreferenceRegistry;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.api.events.ContextMenuEvent;
import io.github.fishstiz.packed_packs.api.events.InitializeLayoutEvent;
import io.github.fishstiz.packed_packs.api.events.InitializePackEntryEvent;
import io.github.fishstiz.packed_packs.compat.*;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class VTDIntegration extends ModIntegration {
    private static final int PENCIL_MARGIN_RIGHT = 1;

    @Override
    public ModContext mod() {
        return FabricMod.VTD;
    }

    @Override
    protected void onInitLoaded(PackedPacksApi api) {
        PreferenceRegistry.Key<Boolean> vtdButtonPrefKey = api.preferences().register(ResourceUtil.id("vtd_button"), true);
        PreferenceRegistry.Key<Boolean> vtdEditButtonPrefKey = api.preferences().register(ResourceUtil.id("vtd_edit_button"), true);

        api.eventBus().register(InitializeLayoutEvent.class, this.id(), ModIntegration.id(Mod.ETF), event -> {
            ScreenContext ctx = event.screenContext();
            if (ctx.isClientResources()) {
                var button = ctx.bindPreference(vtdButtonPrefKey, this.createButton(ctx.screen()));
                if (button != null) event.addWidget(InitializeLayoutEvent.Pos.AFTER_TITLE, button);
            }
        });

        api.eventBus().register(InitializePackEntryEvent.class, this.id(), event -> {
            ScreenContext ctx = event.screenContext();
            if (ctx.isClientResources()) {
                var button = ctx.bindPreference(vtdEditButtonPrefKey, VTDEditButtonWidget.create(
                        vtdEditButtonPrefKey,
                        ctx.screen(),
                        event.packContext().pack(),
                        event.packContext()::fileModifiable
                ));
                if (button != null) event.addBottomRight(PENCIL_MARGIN_RIGHT, button);
            }
        });

        api.eventBus().register(ContextMenuEvent.Preferences.class, this.id(), List.of(id(Mod.RESPACKOPTS), id(Mod.ETF)), event -> {
            if (event.screenContext().isClientResources()) {
                event.addToggle(vtdButtonPrefKey, getWidgetPrefText(vtdButtonPrefKey));
                event.addToggle(vtdEditButtonPrefKey, getWidgetPrefText(vtdEditButtonPrefKey));
            }
        });
    }

    private Button createButton(Screen parent) {
        return FidgetzButton.<Void>builder()
                .makeSquare()
                .setTooltip(Tooltip.create(Component.translatable("vtd.resourcePack.button")))
                .setSprite(Sprite.of32(Identifier.fromNamespaceAndPath("vt_downloader", "icon.png")))
                .setFocusedBorder(Theme.WHITE.getARGB())
                .setOnPress(createVTDScreenSetter(parent, null))
                .build();
    }

    static Runnable createVTDScreenSetter(Screen parent, @Nullable Pack pack) {
        String screenName = "me.bymartrixx.vtd.gui.VTDownloadScreen";
        ScreenArg<Screen> parentArg = ScreenArg.parent(parent);
        ScreenArg<Component> subtitleArg = new ScreenArg<>(Component.class, Component.translatable("vtd.resourcePack.subtitle"));

        if (pack == null) {
            return createScreenSetter(screenName, parentArg, subtitleArg);
        }

        return createScreenSetter(screenName, parentArg, subtitleArg, new ScreenArg<>(
                PackSelectionModel.Entry.class,
                new PackWrapperDelegatorAbstractionEpicModelEntry(pack)
        ));
    }
}
