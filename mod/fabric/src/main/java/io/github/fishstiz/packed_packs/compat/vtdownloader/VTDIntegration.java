package io.github.fishstiz.packed_packs.compat.vtdownloader;

import io.github.fishstiz.fidgetz.v0.gui.components.FZIconButton;
import io.github.fishstiz.fidgetz.v0.gui.components.WidgetRenderables;
import io.github.fishstiz.fidgetz.v0.gui.renderables.RenderableRectangle;
import io.github.fishstiz.fidgetz.v0.gui.renderables.Renderables;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.api.PackedPacksApi;
import io.github.fishstiz.packed_packs.api.Preference;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.api.events.ContextMenuEvent;
import io.github.fishstiz.packed_packs.api.events.InitializeLayoutEvent;
import io.github.fishstiz.packed_packs.api.events.InitializePackEntryEvent;
import io.github.fishstiz.packed_packs.compat.*;
import io.github.fishstiz.packed_packs.util.Colors;
import net.minecraft.client.gui.components.Button;
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
        Preference<Boolean> vtdButton = api.preferences().register(PackedPacks.id("vtd_button"), true);
        Preference<Boolean> vtdEditButton = api.preferences().register(PackedPacks.id("vtd_edit_button"), true);

        api.eventBus().register(InitializeLayoutEvent.class, this.id(), ModIntegration.id(Mod.ETF), event -> {
            ScreenContext ctx = event.screenContext();
            if (ctx.isClientResources()) {
                var button = ctx.wrapWidget(vtdButton, getWidgetPrefText(vtdButton), this.createButton(ctx.screen()));
                if (button != null) event.addWidget(InitializeLayoutEvent.Pos.AFTER_TITLE, button);
            }
        });

        api.eventBus().register(InitializePackEntryEvent.class, this.id(), event -> {
            ScreenContext ctx = event.screenContext();
            if (ctx.isClientResources() && (ctx.devMode() || vtdEditButton.get())) {
                var button = VTDEditButtonWidget.create(
                        vtdEditButton,
                        ctx.screen(),
                        event.packContext().pack(),
                        event.packContext()::fileModifiable
                );
                if (button != null) event.addBottomRight(PENCIL_MARGIN_RIGHT, button);
            }
        });

        api.eventBus().register(ContextMenuEvent.Preferences.class, this.id(), List.of(id(Mod.RESPACKOPTS), id(Mod.ETF)), event -> {
            if (event.screenContext().isClientResources()) {
                event.addToggle(vtdButton, getWidgetPrefText(vtdButton));
                event.addToggle(vtdEditButton, getWidgetPrefText(vtdEditButton));
            }
        });
    }

    private Button createButton(Screen parent) {
        RenderableRectangle icon = Renderables.texture(Identifier.fromNamespaceAndPath("vt_downloader", "icon.png"), 32, 32);
        WidgetRenderables renderables = new WidgetRenderables(icon, icon.then(Renderables.outline(Colors.WHITE)));
        return FZIconButton.builder(renderables)
                .square()
                .tooltip(Component.translatable("vtd.resourcePack.button"))
                .onPress(createVTDScreenSetter(parent, null))
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
