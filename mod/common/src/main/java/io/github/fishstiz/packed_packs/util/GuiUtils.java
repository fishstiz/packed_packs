package io.github.fishstiz.packed_packs.util;

import io.github.fishstiz.fidgetz.v0.gui.components.FZPopoverMenuItem;
import io.github.fishstiz.fidgetz.v0.gui.components.WidgetElements;
import io.github.fishstiz.fidgetz.v0.gui.components.WidgetRenderables;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZFlexLayout;
import io.github.fishstiz.fidgetz.v0.gui.renderables.RenderableRectangle;
import io.github.fishstiz.fidgetz.v0.gui.renderables.Renderables;
import io.github.fishstiz.fidgetz.v0.utils.GuiGraphicsUtils;
import io.github.fishstiz.packed_packs.PackedPacks;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class GuiUtils {
    public static final int SPACING = 8;
    public static final RenderableRectangle WHITE_OVERLAY = Renderables.fill(Colors.alpha(Colors.WHITE, 0.25f));
    public static final RenderableRectangle BLACK_OVERLAY = Renderables.fill(Colors.alpha(Colors.BLACK, 0.25f));
    public static final WidgetRenderables DEV_MODE_ENTRY_BACKGROUND = new WidgetRenderables(
            BLACK_OVERLAY,
            BLACK_OVERLAY,
            BLACK_OVERLAY.then(Renderables.fill(Colors.alpha(Colors.WHITE, 0.1f)))
    );
    public static final RenderableRectangle HAMBURGER_RECT = Renderables.sprite(PackedPacks.id("icon/hamburger"));
    public static final ResourceLocation ARROW_UP_SPRITE = PackedPacks.id("icon/arrow_up");
    public static final ResourceLocation CROSS_SPRITE = PackedPacks.id("icon/cross");
    public static final ResourceLocation LOCK_SPRITE = ResourceLocation.withDefaultNamespace("widget/locked_button");
    public static final ResourceLocation LOCK_SPRITE_DISABLED = ResourceLocation.withDefaultNamespace("widget/locked_button_disabled");
    public static final ResourceLocation LOCK_SPRITE_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("widget/locked_button_highlighted");
    public static final ResourceLocation UNLOCK_SPRITE = ResourceLocation.withDefaultNamespace("widget/unlocked_button");
    public static final ResourceLocation UNLOCK_SPRITE_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("widget/unlocked_button_highlighted");
    public static final ResourceLocation UNLOCK_SPRITE_SMALL = PackedPacks.id("icon/unlock");
    public static final ResourceLocation LOCK_SPRITE_SMALL = PackedPacks.id("icon/lock");
    public static final ResourceLocation STAR_SPRITE = PackedPacks.id("icon/star");
    public static final ResourceLocation TRASH_SPRITE = PackedPacks.id("icon/trash");
    public static final ResourceLocation RADIO_OFF_SPRITE = PackedPacks.id("icon/radio_off");
    public static final ResourceLocation RADIO_ON_SPRITE = PackedPacks.id("icon/radio_on");
    public static final WidgetSprites LOCK_SPRITES = new WidgetSprites(LOCK_SPRITE, LOCK_SPRITE_DISABLED, LOCK_SPRITE_HIGHLIGHTED);
    public static final WidgetSprites UNLOCK_SPRITES = new WidgetSprites(UNLOCK_SPRITE, UNLOCK_SPRITE_HIGHLIGHTED);
    public static final Component OPTIONS_TEXT = Component.translatable("packed_packs.options.title");
    public static final Component OPEN_FILE_TEXT = Component.translatable("packed_packs.file.open");
    public static final Component OPEN_PARENT_TEXT = Component.translatable("packed_packs.file.parent.open");
    public static final Component RENAME_FILE_TEXT = Component.translatable("packed_packs.file.rename");
    public static final Component DELETE_FILE_TEXT = Component.translatable("packed_packs.file.delete");
    public static final Component FOLDER_OPEN_TEXT = Component.translatable("packed_packs.folder.open");
    public static final Component PROFILE_TITLE_TEXT = Component.translatable("packed_packs.profile");
    public static final Component NO_PROFILE_TEXT = Component.translatable("packed_packs.profile.none");
    public static final int DROP_DISABLED_COLOR = Colors.RED_700;
    public static final int DROP_DISABLED_FILL = Colors.alpha(DROP_DISABLED_COLOR, 0.25f);
    public static final int DROP_ENABLED_COLOR = Colors.GREEN_500;
    public static final int DROP_ENABLED_FROM_COLOR = Colors.alpha(DROP_ENABLED_COLOR, 0.75f);
    public static final int DROP_ENABLED_TO_COLOR = Colors.alpha(DROP_ENABLED_COLOR, 0);

    public static FZFlexLayout vertical() {
        return FZFlexLayout.vertical().spacing(SPACING);
    }

    public static FZFlexLayout horizontal() {
        return FZFlexLayout.horizontal().spacing(SPACING);
    }

    public static ResourceLocation toggleIcon(boolean toggled) {
        return toggled ? RADIO_ON_SPRITE : RADIO_OFF_SPRITE;
    }

    public static FZPopoverMenuItem.Builder buildDevEntry(FZPopoverMenuItem.Builder builder) {
        return builder.background(DEV_MODE_ENTRY_BACKGROUND);
    }

    public static RenderableRectangle lazySprite(Supplier<ResourceLocation> spriteGetter) {
        return (graphics, left, top, width, height, ignoredMx, ignoredMy, ignoredD) ->
                GuiGraphicsUtils.sprite(graphics, spriteGetter.get(), left, top, width, height);
    }

    public static RenderableRectangle lazyTexture(Supplier<ResourceLocation> textureGetter, int textureWidth, int textureHeight) {
        return (graphics, left, top, width, height, ignoredMx, ignoredMy, ignoredD) ->
                GuiGraphicsUtils.texture(graphics, textureGetter.get(), left, top, width, height, textureWidth, textureHeight);
    }

    public static WidgetElements padded16Rect(RenderableRectangle rect) {
        return new WidgetElements(rect, 16, 16).marginLeft(-4);
    }

    public static WidgetElements padded16Sprite(ResourceLocation sprite) {
        return padded16Rect(Renderables.sprite(sprite));
    }

    public static WidgetElements toggleRect(BooleanSupplier toggled) {
        return new WidgetElements(lazySprite(() -> toggleIcon(toggled.getAsBoolean())), 8, 8);
    }

    public static void renderDropToDisabledZone(AbstractWidget widget, GuiGraphics graphics) {
        int x = widget.getX();
        int y = widget.getY();
        int width = widget.getWidth();
        int height = widget.getHeight();
        int right = widget.getRight();
        int bottom = widget.getBottom();

        if (widget.isHovered()) {
            graphics.fill(x, y, right, bottom, DROP_DISABLED_FILL);
        }

        graphics.renderOutline(x, y, width, height, DROP_DISABLED_COLOR);
    }

    public static void playButtonClickSound(SoundManager soundManager) {
        soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private GuiUtils() {
    }
}
