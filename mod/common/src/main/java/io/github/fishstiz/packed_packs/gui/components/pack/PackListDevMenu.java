package io.github.fishstiz.packed_packs.gui.components.pack;

import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuProvider;
import io.github.fishstiz.fidgetz.gui.renderables.RenderableRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.packed_packs.config.DevConfig;
import io.github.fishstiz.packed_packs.config.PackOverride;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.gui.model.PackListViewModel;
import io.github.fishstiz.packed_packs.pack.PackOptionsContext;
import io.github.fishstiz.packed_packs.pack.ProfileScope;
import io.github.fishstiz.packed_packs.transform.interfaces.ConfiguredPack;
import io.github.fishstiz.packed_packs.transform.interfaces.FilePack;
import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.Nullable;

import java.util.function.BiPredicate;

import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.*;
import static io.github.fishstiz.fidgetz.util.lang.ObjectsUtil.pick;

public class PackListDevMenu implements ContextMenuProvider, RenderableRect {
    private static final int DEV_SPRITE_SIZE = 16;
    private static final int DEV_SPRITE_MARGIN_RIGHT = 8;
    private static final Sprite EYE_SLASH_SPRITE = Sprite.of16(ResourceUtil.getIcon("eye_slash"));
    private static final Sprite X_SQUARE = Sprite.of16(ResourceUtil.getIcon("x_square"));
    private static final Sprite ARROW_UP_SPRITE = Sprite.of16(ResourceUtil.getIcon("arrow_up"));
    private static final Sprite ARROW_DOWN_SPRITE = Sprite.of16(ResourceUtil.getIcon("arrow_down"));
    private static final Sprite ARROWS_SPRITE = Sprite.of16(ResourceUtil.getIcon("arrows_vertical"));
    private static final Sprite RADIO_GLOBAL = Sprite.of16(ResourceUtil.getIcon("radio_globe"));
    private static final Sprite ALIAS_SPRITE = Sprite.of16(ResourceUtil.getIcon("alias"));
    private static final Component HIDDEN = overrideText("hidden");
    private static final Component REQUIRED = overrideText("required");
    private static final Component FIXED_POSITION = overrideText("fixed");
    private static final Component FIXED_TOP = overrideText("fixed.top");
    private static final Component FIXED_BOTTOM = overrideText("fixed.bottom");
    private static final Component REMOVE_OVERRIDES = overrideText("remove");
    private static final Tooltip REQUIRED_NO_DISABLED_INFO = Tooltip.create(overrideText("required.no.disabled.info"));
    private final Minecraft minecraft;
    private final PackOptionsContext options;
    private final DevConfig.Packs devConfig;
    private final PackListViewModel.Entry viewModel;

    public PackListDevMenu(Minecraft minecraft, DevConfig.Packs devConfig, PackOptionsContext options, PackListViewModel.Entry viewModel) {
        this.minecraft = minecraft;
        this.devConfig = devConfig;
        this.options = options;
        this.viewModel = viewModel;
    }

    private static Component overrideText(String keySuffix) {
        return ResourceUtil.getText("profile.override." + keySuffix);
    }

    private ProfileScope hasOverride(BiPredicate<Profile, Pack> option) {
        return this.options.hasOverride(this.pack(), option);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int top, int left, int width, int height, float partialTick) {
        int size = DEV_SPRITE_SIZE;
        int iconX = (left + width) - size - DEV_SPRITE_MARGIN_RIGHT;

        ProfileScope positionOverride = this.hasOverride(Profile::overridesPosition);
        if (positionOverride.exists()) {
            boolean unfixed = !this.options.isFixed(this.pack());
            boolean fixedTop = this.options.getPosition(this.pack()) == Pack.Position.TOP;
            guiGraphics.fill(iconX, top, iconX + size, top + size, getBackgroundColor(positionOverride));
            pick(unfixed, ARROWS_SPRITE, pick(fixedTop, ARROW_UP_SPRITE, ARROW_DOWN_SPRITE)).render(guiGraphics, iconX, top, size, size);
            iconX -= size;
        }
        ProfileScope requiredOverride = this.hasOverride(Profile::overridesRequired);
        if (requiredOverride.exists()) {
            boolean required = this.options.isRequired(this.pack());
            guiGraphics.fill(iconX, top, iconX + size, top + size, getBackgroundColor(requiredOverride));
            pick(required, LOCK_SPRITE_SMALL, UNLOCK_SPRITE_SMALL).render(guiGraphics, iconX, top, size, size);
            iconX -= size;
        }
        ProfileScope hiddenOverride = this.hasOverride(Profile::isHidden);
        if (hiddenOverride.exists()) {
            guiGraphics.fill(iconX, top, iconX + size, top + size, getBackgroundColor(hiddenOverride));
            EYE_SLASH_SPRITE.render(guiGraphics, iconX, top, size, size);
            iconX -= size;
        }
        ProfileScope included = this.hasOverride(Profile::includes);
        if (included.global() && !((ConfiguredPack) this.pack()).packed_packs$getMetadata().compatibility().isCompatible()) {
            guiGraphics.fill(iconX, top, iconX + size, top + size, Theme.RED_700.withAlpha(0.75f));
            X_SQUARE.render(guiGraphics, iconX, top, size, size);
            iconX -= size;
        }
        if (this.devConfig.hasAlias(this.pack().getId())) {
            guiGraphics.fill(iconX, top, iconX + size, top + size, Theme.GREEN_500.withAlpha(0.75f));
            ALIAS_SPRITE.render(guiGraphics, iconX, top, size, size);
        }
    }

    private Pack pack() {
        return this.viewModel.pack();
    }

    private void updateHidden(boolean hidden) {
        this.viewModel.overrideHidden(hidden);
    }

    private void updateRequired(@Nullable Boolean required) {
        this.viewModel.overrideRequire(required);
    }

    private void updatePosition(PackOverride.@Nullable Position position) {
        this.viewModel.overridePosition(position);
    }

    private void resetOverrides() {
        this.viewModel.removeOverrides();
    }

    private Sprite getIcon(boolean active, BiPredicate<Profile, Pack> defaultOption) {
        return this.hasOverride(defaultOption) == ProfileScope.GLOBAL ? RADIO_GLOBAL : getToggleIcon(active);
    }

    private boolean canDisableRequired() {
        return this.options.isDefaultProfile() && !PackUtil.isEssential(this.pack());
    }

    private void buildNonOverrideOptions(ContextMenuItemBuilder builder) {
        builder.add(devItem(CommonComponents.GUI_COPY_TO_CLIPBOARD)
                .action(() -> this.minecraft.keyboardHandler.setClipboard(this.pack().getId()))
                .build()
        ).separator();

        builder.add(devItem(ResourceUtil.getText("aliases.edit"))
                .action(this.viewModel::editAliases)
                .build()
        ).separator();
    }

    @Override
    public void buildItems(ContextMenuItemBuilder builder, int mouseX, int mouseY) {
        Profile profile = this.options.getProfile().orElse(null);
        if (profile == null) {
            this.buildNonOverrideOptions(builder);
            return;
        }

        builder.add(devItem(HIDDEN)
                .icon(() -> this.getIcon(profile.isHidden(this.pack()), Profile::isHidden))
                .activeWhen(() -> this.hasOverride(Profile::isHidden) != ProfileScope.GLOBAL)
                .action(() -> this.updateHidden(!profile.isHidden(this.pack())))
                .closeOnInteract(false)
                .build());

        builder.add(devItem(REQUIRED)
                .icon(() -> this.options.isLocked()
                        ? LOCK_SPRITE_SMALL
                        : this.getIcon(profile.overridesRequired(this.pack()), Profile::overridesRequired))
                .activeWhen(() -> !this.options.isLocked() &&
                                  this.hasOverride(Profile::overridesRequired) != ProfileScope.GLOBAL &&
                                  !((FilePack) this.pack()).packed_packs$nestedPack())
                .closeOnInteract(false)
                .addChild(devItem(CommonComponents.OPTION_OFF)
                        .icon(() -> getToggleIcon(!profile.overridesRequired(this.pack())))
                        .action(() -> this.updateRequired(null))
                        .closeOnInteract(false)
                        .build())
                .addChild(devItem(CommonComponents.GUI_NO)
                        .icon(() -> getToggleIcon(profile.overridesRequired(this.pack()) && !profile.isRequired(this.pack())))
                        .activeWhen(this::canDisableRequired)
                        .tooltip(() -> !this.canDisableRequired() && !PackUtil.isEssential(this.pack()) ? REQUIRED_NO_DISABLED_INFO : null)
                        .action(() -> this.updateRequired(false))
                        .closeOnInteract(false)
                        .build())
                .addChild(devItem(CommonComponents.GUI_YES)
                        .icon(() -> getToggleIcon(profile.isRequired(this.pack())))
                        .action(() -> this.updateRequired(true))
                        .closeOnInteract(false)
                        .build())
                .build());

        builder.add(devItem(FIXED_POSITION)
                .icon(() -> this.getIcon(profile.overridesPosition(this.pack()), Profile::overridesPosition))
                .activeWhen(() -> this.hasOverride(Profile::overridesPosition) != ProfileScope.GLOBAL)
                .closeOnInteract(false)
                .addChild(devItem(CommonComponents.OPTION_OFF)
                        .icon(() -> getToggleIcon(!profile.overridesPosition(this.pack())))
                        .action(() -> this.updatePosition(null))
                        .closeOnInteract(false)
                        .build())
                .addChild(devItem(CommonComponents.GUI_NO)
                        .icon(() -> getToggleIcon(profile.overridesPosition(this.pack()) && !profile.isFixed(this.pack())))
                        .action(() -> this.updatePosition(PackOverride.Position.UNFIXED))
                        .closeOnInteract(false)
                        .build())
                .addChild(devItem(FIXED_TOP)
                        .icon(() -> getToggleIcon(profile.getPositionOverride(this.pack()) == PackOverride.Position.TOP))
                        .action(() -> this.updatePosition(PackOverride.Position.TOP))
                        .closeOnInteract(false)
                        .build())
                .addChild(devItem(FIXED_BOTTOM)
                        .icon(() -> getToggleIcon(profile.getPositionOverride(this.pack()) == PackOverride.Position.BOTTOM))
                        .action(() -> this.updatePosition(PackOverride.Position.BOTTOM))
                        .closeOnInteract(false)
                        .build())
                .build());

        builder.add(devItem(REMOVE_OVERRIDES).action(this::resetOverrides).build()).separator();

        this.buildNonOverrideOptions(builder);
    }

    private static int getBackgroundColor(ProfileScope overrideScope) {
        return switch (overrideScope) {
            case NONE -> Theme.WHITE.withAlpha(0);
            case LOCAL -> Theme.BLACK.withAlpha(0.75f);
            case GLOBAL -> Theme.BLUE_500.withAlpha(0.75f);
            case COMPOSITE -> Theme.PURPLE_500.withAlpha(0.75f);
        };
    }
}
