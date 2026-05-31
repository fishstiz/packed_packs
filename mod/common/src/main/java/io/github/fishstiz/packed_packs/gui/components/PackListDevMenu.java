package io.github.fishstiz.packed_packs.gui.components;

import io.github.fishstiz.fidgetz.v0.gui.components.FZContextMenu;
import io.github.fishstiz.fidgetz.v0.gui.components.WidgetElements;
import io.github.fishstiz.fidgetz.v0.utils.GuiGraphicsUtils;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.config.DevConfig;
import io.github.fishstiz.packed_packs.config.PackOverride;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.gui.model.PackListViewModel;
import io.github.fishstiz.packed_packs.pack.PackOptionsContext;
import io.github.fishstiz.packed_packs.pack.ProfileScope;
import io.github.fishstiz.packed_packs.transform.interfaces.ConfiguredPack;
import io.github.fishstiz.packed_packs.transform.interfaces.FilePack;
import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.packed_packs.util.Colors;
import io.github.fishstiz.packed_packs.util.GuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.Nullable;

import java.util.function.BiPredicate;
import java.util.function.Supplier;

import static io.github.fishstiz.packed_packs.util.GuiUtils.*;

public class PackListDevMenu {
    private static final int DEV_SPRITE_SIZE = 16;
    private static final int DEV_SPRITE_MARGIN_RIGHT = 8;
    private static final Identifier EYE_SLASH_SPRITE = PackedPacks.id("icon/eye_slash");
    private static final Identifier X_SQUARE = PackedPacks.id("icon/x_square");
    private static final WidgetSprites ARROW_SPRITES = new WidgetSprites(
            PackedPacks.id("icon/arrow_down"),
            PackedPacks.id("icon/arrows_vertical"),
            PackedPacks.id("icon/arrow_up")
    );
    private static final Identifier RADIO_GLOBAL = PackedPacks.id("icon/radio_globe");
    private static final Identifier ALIAS_SPRITE = PackedPacks.id("icon/alias");
    private static final Component HIDDEN = overrideText("hidden");
    private static final Component REQUIRED = overrideText("required");
    private static final Component FIXED_POSITION = overrideText("fixed");
    private static final Component FIXED_TOP = overrideText("fixed.top");
    private static final Component FIXED_BOTTOM = overrideText("fixed.bottom");
    private static final Component REMOVE_OVERRIDES = overrideText("remove");
    private static final Tooltip REQUIRED_NO_DISABLED_INFO = Tooltip.create(overrideText("required.no.disabled.info"));
    private final PackOptionsContext options;
    private final DevConfig.Packs devConfig;
    private final PackListViewModel.Entry model;

    public PackListDevMenu(DevConfig.Packs devConfig, PackOptionsContext options, PackListViewModel.Entry model) {
        this.devConfig = devConfig;
        this.options = options;
        this.model = model;
    }

    private static Component overrideText(String keySuffix) {
        return Component.translatable("packed_packs.profile.override." + keySuffix);
    }

    private ProfileScope hasOverride(BiPredicate<Profile, Pack> option) {
        return this.options.hasOverride(this.pack(), option);
    }

    public void render(GuiGraphicsExtractor graphics, int top, int left, int width) {
        int size = DEV_SPRITE_SIZE;
        int iconX = (left + width) - size - DEV_SPRITE_MARGIN_RIGHT;

        ProfileScope positionOverride = hasOverride(Profile::overridesPosition);
        if (positionOverride.exists()) {
            boolean fixed = options.isFixed(pack());
            boolean fixedTop = options.getPosition(pack()) == Pack.Position.TOP;
            graphics.fill(iconX, top, iconX + size, top + size, backgroundColor(positionOverride));
            GuiGraphicsUtils.sprite(graphics, ARROW_SPRITES.get(fixed, fixedTop), iconX, top, size, size);
            iconX -= size;
        }
        ProfileScope requiredOverride = this.hasOverride(Profile::overridesRequired);
        if (requiredOverride.exists()) {
            boolean required = this.options.isRequired(this.pack());
            graphics.fill(iconX, top, iconX + size, top + size, backgroundColor(requiredOverride));
            Identifier sprite = required ? LOCK_SPRITE_SMALL : UNLOCK_SPRITE_SMALL;
            GuiGraphicsUtils.sprite(graphics, sprite, iconX, top, size, size);
            iconX -= size;
        }
        ProfileScope hiddenOverride = this.hasOverride(Profile::isHidden);
        if (hiddenOverride.exists()) {
            graphics.fill(iconX, top, iconX + size, top + size, backgroundColor(hiddenOverride));
            GuiGraphicsUtils.sprite(graphics, EYE_SLASH_SPRITE, iconX, top, size, size);
            iconX -= size;
        }
        ProfileScope included = this.hasOverride(Profile::includes);
        if (included.global() && !((ConfiguredPack) this.pack()).packed_packs$getMetadata().compatibility().isCompatible()) {
            graphics.fill(iconX, top, iconX + size, top + size, Colors.alpha(Colors.RED_700, 0.75f));
            GuiGraphicsUtils.sprite(graphics, X_SQUARE, iconX, top, size, size);
            iconX -= size;
        }
        if (devConfig.hasAlias(this.pack().getId())) {
            graphics.fill(iconX, top, iconX + size, top + size, Colors.alpha(Colors.GREEN_500, 0.75f));
            GuiGraphicsUtils.sprite(graphics, ALIAS_SPRITE, iconX, top, size, size);
        }
    }

    private Pack pack() {
        return this.model.pack();
    }

    private void updateHidden(boolean hidden) {
        this.model.overrideHidden(hidden);
    }

    private void updateRequired(@Nullable Boolean required) {
        this.model.overrideRequire(required);
    }

    private void updatePosition(PackOverride.@Nullable Position position) {
        this.model.overridePosition(position);
    }

    private void resetOverrides() {
        this.model.removeOverrides();
    }

    private Identifier getIcon(boolean active, BiPredicate<Profile, Pack> defaultOption) {
        return hasOverride(defaultOption) == ProfileScope.GLOBAL ? RADIO_GLOBAL : toggleIcon(active);
    }

    private boolean canDisableRequired() {
        return this.options.isDefaultProfile() && !PackUtil.isEssential(this.pack());
    }

    private static WidgetElements createIcon(Supplier<Identifier> iconSupplier) {
        return new WidgetElements(GuiUtils.createRect(iconSupplier), 8, 8);
    }

    private void updateNonOverrideEntries(FZContextMenu.Collector collector) {
        collector.addEntry(builder -> buildDevEntry(builder)
                .message(CommonComponents.GUI_COPY_TO_CLIPBOARD)
                .onPress(() -> Minecraft.getInstance().keyboardHandler.setClipboard(pack().getId())));

        collector.addEntry(builder -> buildDevEntry(builder)
                .message(Component.translatable("packed_packs.aliases.edit"))
                .onPress(model::editAliases));
    }

    public void updateContextEntries(FZContextMenu.Collector collector) {
        Profile profile = options.getProfile().orElse(null);
        if (profile == null) {
            updateNonOverrideEntries(collector.nextSection());
            return;
        }

        collector.addEntry(builder -> buildDevEntry(builder)
                .message(HIDDEN)
                .icon(createIcon(() -> getIcon(profile.isHidden(pack()), Profile::isHidden)))
                .active(() -> hasOverride(Profile::isHidden) != ProfileScope.GLOBAL)
                .closeOnInteraction(false)
                .onPress(() -> updateHidden(!profile.isHidden(pack()))));

        collector.addEntry(builder -> buildDevEntry(builder)
                .message(REQUIRED)
                .icon(createIcon(() -> options.isLocked() ? LOCK_SPRITE_SMALL : getIcon(profile.isRequired(pack()), Profile::isRequired)))
                .closeOnInteraction(false)
                .active(() -> !options.isLocked() &&
                              hasOverride(Profile::overridesRequired) != ProfileScope.GLOBAL &&
                              !((FilePack) pack()).packed_packs$nestedPack())
                .child(child -> buildDevEntry(child)
                        .message(CommonComponents.OPTION_OFF)
                        .icon(GuiUtils.toggleRect(() -> !profile.overridesRequired(pack())))
                        .closeOnInteraction(false)
                        .onPress(() -> updateRequired(null)))
                .child(child -> buildDevEntry(child)
                        .message(CommonComponents.GUI_NO)
                        .icon(GuiUtils.toggleRect(() -> profile.overridesRequired(pack()) && !profile.isRequired(pack())))
                        .active(this::canDisableRequired)
                        .tooltip(() -> !canDisableRequired() && !PackUtil.isEssential(pack()) ? REQUIRED_NO_DISABLED_INFO : null)
                        .closeOnInteraction(false)
                        .onPress(() -> updateRequired(false)))
                .child(child -> buildDevEntry(child)
                        .message(CommonComponents.GUI_YES)
                        .icon(GuiUtils.toggleRect(() -> profile.isRequired(pack())))
                        .closeOnInteraction(false)
                        .onPress(() -> updateRequired(true))));

        collector.addEntry(builder -> buildDevEntry(builder)
                .message(FIXED_POSITION)
                .icon(createIcon(() -> getIcon(profile.overridesPosition(pack()), Profile::overridesPosition)))
                .active(() -> hasOverride(Profile::overridesPosition) != ProfileScope.GLOBAL)
                .closeOnInteraction(false)
                .child(child -> buildDevEntry(child)
                        .message(CommonComponents.OPTION_OFF)
                        .icon(GuiUtils.toggleRect(() -> !profile.overridesPosition(pack())))
                        .closeOnInteraction(false)
                        .onPress(() -> updatePosition(null)))
                .child(child -> buildDevEntry(child)
                        .message(CommonComponents.GUI_NO)
                        .icon(GuiUtils.toggleRect(() -> profile.overridesPosition(pack()) && !profile.isFixed(pack())))
                        .closeOnInteraction(false)
                        .onPress(() -> updatePosition(PackOverride.Position.UNFIXED)))
                .child(child -> buildDevEntry(child)
                        .message(FIXED_TOP)
                        .icon(GuiUtils.toggleRect(() -> profile.getPositionOverride(pack()) == PackOverride.Position.TOP))
                        .closeOnInteraction(false)
                        .onPress(() -> updatePosition(PackOverride.Position.TOP)))
                .child(child -> buildDevEntry(child)
                        .message(FIXED_BOTTOM)
                        .icon(GuiUtils.toggleRect(() -> profile.getPositionOverride(pack()) == PackOverride.Position.BOTTOM))
                        .closeOnInteraction(false)
                        .onPress(() -> updatePosition(PackOverride.Position.BOTTOM))));

        collector.addEntry(builder -> buildDevEntry(builder)
                .message(REMOVE_OVERRIDES)
                .onPress(this::resetOverrides));

        updateNonOverrideEntries(collector.nextSection());
    }

    private static int backgroundColor(ProfileScope overrideScope) {
        return switch (overrideScope) {
            case NONE -> Colors.alpha(Colors.WHITE, 0);
            case LOCAL -> Colors.alpha(Colors.BLACK, 0.75f);
            case GLOBAL -> Colors.alpha(Colors.BLUE_500, 0.75f);
            case COMPOSITE -> Colors.alpha(Colors.PURPLE_500, 0.75f);
        };
    }
}
