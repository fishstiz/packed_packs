package io.github.fishstiz.packed_packs.gui.layouts;

import io.github.fishstiz.fidgetz.gui.components.FidgetzText;
import io.github.fishstiz.fidgetz.gui.components.ToggleButton;
import io.github.fishstiz.fidgetz.gui.layouts.Layouts;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.IntSupplier;

public class OptionsLayout implements Layout {
    private static final int CONTENT_WIDTH = 200;
    private final IntSupplier maxHeightSupplier;
    private final ScrollableLayout layout;

    public OptionsLayout(Minecraft minecraft, IntSupplier maxHeightSupplier, Config.Packs... configs) {
        this.maxHeightSupplier = maxHeightSupplier;
        final int spacing = GuiConstants.SPACING;
        final LinearLayout contentLayout = LinearLayout.vertical().spacing(spacing);
        final LayoutSettings titleSettings = LayoutSettings.defaults()
                .paddingTop(-(spacing / 2))
                .paddingBottom(-spacing);

        for (Config.Packs config : configs) {
            switch (config) {
                case Config.ResourcePacks resourcePacks -> {
                    contentLayout.addChild(
                            FidgetzText.<Void>builder()
                                    .setMessage(ResourceUtil.getText("resource_packs"))
                                    .setWidth(CONTENT_WIDTH)
                                    .build(),
                            titleSettings
                    );
                    addCommonOptions(contentLayout, resourcePacks);
                    contentLayout.addChild(
                            ToggleButton.<Void>builder()
                                    .setMessage(ResourceUtil.getText("options.apply_on_close"))
                                    .setValue(resourcePacks.isApplyOnClose())
                                    .setOnPress(() -> resourcePacks.setApplyOnClose(!resourcePacks.isApplyOnClose()))
                                    .setWidth(CONTENT_WIDTH)
                                    .build()
                    );
                }
                case Config.DataPacks dataPacks -> {
                    contentLayout.addChild(
                            FidgetzText.<Void>builder()
                                    .setMessage(Component.translatable("selectWorld.dataPacks"))
                                    .setWidth(CONTENT_WIDTH)
                                    .build(),
                            titleSettings
                    );
                    addCommonOptions(contentLayout, dataPacks);
                }
            }
        }

        this.layout = Layouts.unpaddedScrollableLayout(minecraft, contentLayout);
    }

    public OptionsLayout(Minecraft minecraft, IntSupplier maxHeightSupplier) {
        this(minecraft, maxHeightSupplier, Config.get().getResourcepacks(), Config.get().getDatapacks());
    }


    @Override
    public void visitWidgets(Consumer<AbstractWidget> visitor) {
        this.layout.visitWidgets(visitor);
    }

    @Override
    public void visitChildren(Consumer<LayoutElement> visitor) {
        this.layout.visitChildren(visitor);
    }

    @Override
    public void setX(int x) {
        this.layout.setX(x);
    }

    @Override
    public void setY(int y) {
        this.layout.setY(y);
    }

    @Override
    public int getX() {
        return this.layout.getX();
    }

    @Override
    public int getY() {
        return this.layout.getY();
    }

    @Override
    public int getWidth() {
        return this.layout.getWidth();
    }

    @Override
    public int getHeight() {
        return this.layout.getHeight();
    }

    @Override
    public void arrangeElements() {
        this.layout.setMaxHeight(this.maxHeightSupplier.getAsInt());
        this.layout.arrangeElements();
    }

    private static void addCommonOptions(LinearLayout linearLayout, Config.Packs config) {
        linearLayout.addChild(
                ToggleButton.<Void>builder()
                        .setMessage(ResourceUtil.getText("options.replace_screen"))
                        .setValue(config.isReplaceOriginal())
                        .setOnPress(() -> config.setReplaceOriginal(!config.isReplaceOriginal()))
                        .setWidth(CONTENT_WIDTH)
                        .build()
        );
        linearLayout.addChild(
                ToggleButton.<Void>builder()
                        .setMessage(ResourceUtil.getText("options.hide_incompatible_warnings"))
                        .setTooltip(Tooltip.create(ResourceUtil.getText("options.hide_incompatible_warnings.info")))
                        .setValue(config.isIncompatibleWarningsHidden())
                        .setOnPress(() -> config.setHideIncompatibleWarnings(!config.isIncompatibleWarningsHidden()))
                        .setWidth(CONTENT_WIDTH)
                        .build()
        );
        linearLayout.addChild(
                ToggleButton.<Void>builder()
                        .setMessage(ResourceUtil.getText("options.remember_last_viewed_profile"))
                        .setValue(config.isLastViewedProfileRemembered())
                        .setOnPress(() -> config.setRememberLastViewedProfile(!config.isLastViewedProfileRemembered()))
                        .setWidth(CONTENT_WIDTH)
                        .build()
        );
    }
}
