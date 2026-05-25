package io.github.fishstiz.packed_packs.impl.gui;

import io.github.fishstiz.fidgetz.v0.gui.components.FZContextMenuEntry;
import io.github.fishstiz.packed_packs.api.gui.ContextMenuItemSpec;
import io.github.fishstiz.packed_packs.util.GuiUtils;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class ContextMenuItemSpecImpl implements ContextMenuItemSpec {
    private final boolean devStyle;
    private final FZContextMenuEntry.Builder itemBuilder = FZContextMenuEntry.builder();
    private boolean separatorBelow;
    private boolean separatorAbove;

    public ContextMenuItemSpecImpl(boolean devStyle) {
        this.devStyle = devStyle;
        if (devStyle) this.applyDevStyle();
    }

    @Override
    public ContextMenuItemSpec label(Component label) {
        this.itemBuilder.message(label);
        return this;
    }

    @Override
    public ContextMenuItemSpec action(Runnable action) {
        this.itemBuilder.onPress(action);
        return this;
    }

    @Override
    public ContextMenuItemSpec icon(Identifier guiSprite) {
        this.itemBuilder.icon(GuiUtils.padded16Sprite(guiSprite));
        return this;
    }

    @Override
    public ContextMenuItemSpec tooltip(Tooltip tooltip) {
        this.itemBuilder.tooltip(tooltip);
        return this;
    }

    @Override
    public ContextMenuItemSpec active(BooleanSupplier active) {
        this.itemBuilder.active(active);
        return this;
    }

    @Override
    public ContextMenuItemSpec closeOnInteract(boolean closeOnInteract) {
        this.itemBuilder.closeOnInteraction(closeOnInteract);
        return this;
    }

    @Override
    public ContextMenuItemSpec child(Consumer<ContextMenuItemSpec> configurator) {
        ContextMenuItemSpecImpl childBuilder = new ContextMenuItemSpecImpl(this.devStyle);
        configurator.accept(childBuilder);
        if (childBuilder.separatorAbove) itemBuilder.nextSection();
        itemBuilder.child(childBuilder.itemBuilder.build());
        if (childBuilder.separatorBelow) itemBuilder.nextSection();
        return this;
    }

    @Override
    public ContextMenuItemSpec separatorBelow() {
        this.separatorBelow = true;
        return this;
    }

    @Override
    public ContextMenuItemSpec separatorAbove() {
        this.separatorAbove = true;
        return this;
    }

    @Override
    public ContextMenuItemSpec asToggle(BooleanSupplier value, BooleanConsumer onChange) {
        this.itemBuilder.icon(GuiUtils.toggleRect(value)).onPress(() -> onChange.accept(!value.getAsBoolean()));
        return this;
    }

    @Override
    public ContextMenuItemSpec applyDevStyle() {
        GuiUtils.buildDevEntry(this.itemBuilder);
        return this;
    }

    public void apply(FZContextMenuEntry.Collector collector) {
        if (separatorAbove) {
            collector.nextSection();
        }
        collector.addEntry(itemBuilder.build());
        if (separatorBelow) {
            collector.nextSection();
        }
    }
}
