package io.github.fishstiz.packed_packs.gui.screens;

import io.github.fishstiz.fidgetz.gui.components.FidgetzButton;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.gui.layouts.OptionsLayout;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;

public class OptionsScreen extends Screen {
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final Screen previous;
    private Layout body;

    public OptionsScreen(Screen previous) {
        super(GuiConstants.OPTIONS_TEXT);
        this.previous = previous;
    }

    @Override
    protected void init() {
        this.layout.addTitleHeader(this.title, this.font);
        this.body = this.layout.addToContents(new OptionsLayout(this.minecraft, this.layout::getContentHeight));
        this.layout.addToFooter(FidgetzButton.builder().setMessage(CommonComponents.GUI_DONE).setOnPress(this::onClose).build());
        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();

        if (this.body != null) {
            this.body.setY(this.layout.getHeaderHeight());
        }
    }

    @Override
    public void onClose() {
        Config.get().save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.previous);
        }
    }
}
