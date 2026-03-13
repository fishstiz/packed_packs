package io.github.fishstiz.packed_packs.impl.gui;

import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.MenuItem;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.MenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.GuiSprite;
import io.github.fishstiz.packed_packs.api.gui.ContextMenuItemSpec;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class ContextMenuItemSpecImpl implements ContextMenuItemSpec {
    private final boolean devStyle;
    private final MenuItemBuilder itemBuilder;
    private boolean separatorBelow;
    private boolean separatorAbove;

    public ContextMenuItemSpecImpl(boolean devStyle) {
        this.itemBuilder = MenuItem.builder(CommonComponents.EMPTY);
        this.devStyle = devStyle;
        if (devStyle) this.applyDevStyle();
    }

    @Override
    public ContextMenuItemSpec label(Component label) {
        this.itemBuilder.text(label);
        return this;
    }

    @Override
    public ContextMenuItemSpec action(Runnable action) {
        this.itemBuilder.action(action);
        return this;
    }

    @Override
    public ContextMenuItemSpec icon(Identifier guiSprite) {
        this.itemBuilder.icon(new GuiSprite(guiSprite, 16, 16));
        return this;
    }

    @Override
    public ContextMenuItemSpec tooltip(Tooltip tooltip) {
        this.itemBuilder.tooltip(tooltip);
        return this;
    }

    @Override
    public ContextMenuItemSpec active(BooleanSupplier active) {
        this.itemBuilder.activeWhen(active);
        return this;
    }

    @Override
    public ContextMenuItemSpec closeOnInteract(boolean closeOnInteract) {
        this.itemBuilder.closeOnInteract(closeOnInteract);
        return this;
    }

    @Override
    public ContextMenuItemSpec child(Consumer<ContextMenuItemSpec> configurator) {
        ContextMenuItemSpecImpl childBuilder = new ContextMenuItemSpecImpl(this.devStyle);
        configurator.accept(childBuilder);
        if (childBuilder.separatorAbove) this.itemBuilder.addChild(MenuItem.SEPARATOR);
        this.itemBuilder.addChild(childBuilder.itemBuilder.build());
        if (childBuilder.separatorBelow) this.itemBuilder.addChild(MenuItem.SEPARATOR);
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
        this.itemBuilder
                .icon(() -> GuiConstants.getToggleIcon(value.getAsBoolean()))
                .action(() -> onChange.accept(!value.getAsBoolean()));
        return this;
    }

    @Override
    public ContextMenuItemSpec applyDevStyle() {
        this.itemBuilder.background(GuiConstants.DEVELOPER_MODE_ITEM_BACKGROUND);
        return this;
    }

    public ContextMenuItemBuilder apply(ContextMenuItemBuilder builder) {
        return builder.when(this.separatorAbove)
                .ifTrue(b -> b.add(MenuItem.SEPARATOR))
                .add(this.itemBuilder.build())
                .when(this.separatorBelow)
                .ifTrue(b -> b.add(MenuItem.SEPARATOR));
    }
}
