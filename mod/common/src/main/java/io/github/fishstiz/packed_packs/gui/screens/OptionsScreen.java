package io.github.fishstiz.packed_packs.gui.screens;

import io.github.fishstiz.fidgetz.v0.gui.components.FZButton;
import io.github.fishstiz.fidgetz.v0.gui.components.FZText;
import io.github.fishstiz.fidgetz.v0.gui.components.GuiComponentCollector;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZComposedLayout;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZFlexLayout;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZLayout;
import io.github.fishstiz.fidgetz.v0.gui.screens.FZScreen;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.gui.layouts.OptionsLayout;
import io.github.fishstiz.packed_packs.util.GuiUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import org.jetbrains.annotations.Nullable;

public class OptionsScreen extends FZScreen {
    private final Screen previous;
    private @Nullable FZLayout root;

    public OptionsScreen(Screen previous) {
        super(GuiUtils.OPTIONS_TEXT);
        this.previous = previous;
    }

    @Override
    protected void collectChildren(GuiComponentCollector collector) {
        root = FZComposedLayout.contain(this, FZFlexLayout.vertical(this).spacing(GuiUtils.SPACING).also(root -> {
            root.defaultChildSettings().alignHorizontallyCenter();
            root.child(FZText.builder(title).build());
            root.child(OptionsLayout.create(this, Config.get().getResourcepacks(), Config.get().getDatapacks()), root.flexChildVerticalSettings());
            root.child(FZButton.builder().bigWidth().message(CommonComponents.GUI_DONE).onPress(this::onClose).build());
        })).padding(GuiUtils.SPACING).center().clamp().arrange().visitWidgets(collector::renderableWidget).get();
    }

    @Override
    protected void repositionElements() {
        if (root == null) {
            super.repositionElements();
        } else {
            root.fidgetz$setSize(width, height);
            root.arrangeElements();
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
