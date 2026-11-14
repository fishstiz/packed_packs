package io.github.fishstiz.packed_packs.gui.layouts;

import io.github.fishstiz.fidgetz.v0.gui.components.*;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZFlexLayout;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZLayout;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZScrollableLayout;
import io.github.fishstiz.fidgetz.v0.gui.renderables.Renderables;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.gui.components.SwapFolderAction;
import io.github.fishstiz.packed_packs.util.Colors;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

import static io.github.fishstiz.packed_packs.util.GuiUtils.SPACING;

public class OptionsLayout extends WrappedLayout {
    private static final int WIDTH = 250;

    private OptionsLayout(FZLayout layout) {
        super(layout);
    }

    @Override
    public void arrangeElements() {
        fidgetz$setHeight(0);
        super.arrangeElements();
    }

    public static OptionsLayout create(@Nullable ScreenContext ctx, ContainerEventHandler container, Config.Packs... configs) {
        FZFlexLayout layout = FZFlexLayout.vertical().spacing(SPACING).maxWidth(WIDTH);
        layout.defaultChildSettings().flexCross();

        category(layout, Component.translatable("stat.generalButton"));
        generalOptions(ctx, layout);

        for (Config.Packs config : configs) {
            switch (config) {
                case Config.DataPacks dataPacks -> {
                    category(layout, Component.translatable("selectWorld.dataPacks"));
                    commonOptions(layout, dataPacks);
                }
                case Config.ResourcePacks resourcePacks -> {
                    category(layout, Component.translatable("packed_packs.resource_packs"));
                    commonOptions(layout, resourcePacks);
                    layout.child(toggle(
                            Component.translatable("packed_packs.options.apply_on_close"),
                            resourcePacks::setApplyOnClose,
                            resourcePacks::isApplyOnClose
                    ).build());
                }
            }
        }

        return new OptionsLayout(FZScrollableLayout.from(container, layout));
    }

    public static OptionsLayout create(ContainerEventHandler container, Config.Packs... configs) {
        return create(null, container, configs);
    }

    private static void category(FZFlexLayout layout, MutableComponent name) {
        layout.child(FZText.builder(name.withStyle(ChatFormatting.BOLD)).build());
    }

    private static void generalOptions(@Nullable ScreenContext ctx, FZFlexLayout layout) {
        FZFlexLayout maxWidthEntry = layout.child(FZFlexLayout.horizontal(), layout.flexChildHorizontalSettings());
        maxWidthEntry.defaultChildSettings().alignVerticallyMiddle();

        Component maxWidthMessage = Component.translatable("packed_packs.options.max_width");
        maxWidthEntry.child(
                FZIcon.builder(Renderables.fill(Colors.alpha(Colors.BLACK, 0.5f))
                                .then(Renderables.text(maxWidthMessage, true).shrink(SPACING)))
                        .message(maxWidthMessage)
                        .build(),
                maxWidthEntry.flexChildHorizontalSettings()
        );

        maxWidthEntry.child(FZTextField.builder()
                .width(44)
                .hint(Component.translatable("gui.none"))
                .text(String.valueOf(Config.get().getMaxWidth()))
                .filter(input -> input.codePoints().allMatch(Character::isDigit))
                .maxLength(4)
                .onChange(e -> {
                    String text = e.value();
                    int maxWidth = text.isEmpty() ? 0 : Integer.parseInt(text);
                    int previousMaxWidth = Config.get().getMaxWidth();
                    Config.get().setMaxWidth(maxWidth);
                    if (maxWidth != previousMaxWidth && ctx != null) {
                        ctx.rebuild();
                    }
                })
                .onBlur(e -> {
                    if (Config.get().getMaxWidth() <= 0) {
                        e.target().setValue("");
                    }
                })
                .build());
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
                Component.translatable("packed_packs.options.folders_module_by_default"),
                config::setFoldersModuleByDefault,
                config::areFoldersModuleByDefault
        ).build());

        layout.child(FZButton.builder()
                .bigWidth()
                .message(swapActionMessage(config.shouldSwapFolderActions()))
                .tooltip(Component.translatable("packed_packs.options.swap_folder_actions.info"))
                .onPress(e -> {
                    var options = SwapFolderAction.values();
                    SwapFolderAction next = options[(config.shouldSwapFolderActions().ordinal() + 1) % options.length];
                    config.setSwapFolderActions(next);
                    e.target().setMessage(swapActionMessage(next));
                })
                .build());

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

    private static Component swapActionMessage(SwapFolderAction action) {
        Component label = Component.translatable("packed_packs.options.swap_folder_actions");
        return switch (action) {
            case NO -> CommonComponents.optionStatus(label, false);
            case YES -> CommonComponents.optionStatus(label, true);
            case UNLOCKED_ONLY -> CommonComponents.optionNameValue(
                    label,
                    Component.translatable("packed_packs.options.swap_folder_actions.unlocked_only")
            );
        };
    }
}
