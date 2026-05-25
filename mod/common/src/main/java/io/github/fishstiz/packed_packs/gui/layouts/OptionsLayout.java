package io.github.fishstiz.packed_packs.gui.layouts;

import io.github.fishstiz.fidgetz.v0.gui.components.FZButton;
import io.github.fishstiz.fidgetz.v0.gui.components.FZText;
import io.github.fishstiz.fidgetz.v0.gui.layouts.*;
import io.github.fishstiz.packed_packs.config.Config;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

import static io.github.fishstiz.packed_packs.util.GuiUtils.SPACING;

public class OptionsLayout extends WrappedLayout {
    private OptionsLayout(FZLayout layout) {
        super(layout);
    }

    public static OptionsLayout create(ContainerEventHandler container, Config.Packs... configs) {
        FZFlexLayout root = FZFlexLayout.horizontal().spacing(SPACING);

        for (Config.Packs config : configs) {
            FZFlexLayout body = root.child(FZFlexLayout.vertical().spacing(SPACING), root.flexChildSettings());
            body.defaultChildSettings().flexCross();

            switch (config) {
                case Config.DataPacks dataPacks -> {
                    body.child(FZText.builder(Component.translatable("selectWorld.dataPacks")).build());

                    FZFlexLayout contents = FZFlexLayout.vertical().spacing(SPACING);
                    contents.defaultChildSettings().flexCross();
                    commonOptions(contents, dataPacks);

                    body.child(FZScrollableLayout.from(container, contents), body.flexChildSettings()).arrangeElements();
                }
                case Config.ResourcePacks resourcePacks -> {
                    body.child(FZText.builder(Component.translatable("packed_packs.resource_packs")).build());

                    FZFlexLayout contents = FZFlexLayout.vertical().spacing(SPACING);
                    contents.defaultChildSettings().flexCross();
                    commonOptions(contents, resourcePacks);
                    contents.child(toggle(
                            Component.translatable("packed_packs.options.apply_on_close"),
                            resourcePacks::setApplyOnClose,
                            resourcePacks::isApplyOnClose
                    ).build());

                    contents.arrangeElements();
                    body.child(FZScrollableLayout.from(container, contents), body.flexChildSettings()).arrangeElements();
                }
            }
        }

        return new OptionsLayout(root);
    }

    private static void commonOptions(FZFlexLayout layout, Config.Packs config) {
        layout.child(toggle(
                Component.translatable("packed_packs.options.replace_screen"),
                config::setReplaceOriginal,
                config::isReplaceOriginal
        ).build());

        layout.child(toggle(
                Component.translatable("packed_packs.options.hide_incompatible_warnings"),
                config::setHideIncompatibleWarnings,
                config::isIncompatibleWarningsHidden
        ).tooltip(Component.translatable("packed_packs.options.hide_incompatible_warnings.info")).build());

        layout.child(toggle(
                Component.translatable("packed_packs.options.remember_last_viewed_profile"),
                config::setRememberLastViewedProfile,
                config::isLastViewedProfileRemembered
        ).build());
    }

    private static FZButton.Builder toggle(Component name, BooleanConsumer setter, BooleanSupplier getter) {
        return FZButton.builder()
                .bigWidth()
                .message(CommonComponents.optionStatus(name, getter.getAsBoolean()))
                .onPress(e -> {
                    setter.accept(!getter.getAsBoolean());
                    e.target().setMessage(CommonComponents.optionStatus(name, getter.getAsBoolean()));
                });
    }
}
