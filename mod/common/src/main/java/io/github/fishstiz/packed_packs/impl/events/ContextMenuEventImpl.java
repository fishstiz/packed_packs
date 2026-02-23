package io.github.fishstiz.packed_packs.impl.events;

import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.MenuItem;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.MenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.GuiSprite;
import io.github.fishstiz.packed_packs.api.PreferenceRegistry;
import io.github.fishstiz.packed_packs.api.context.PackContext;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.api.events.ContextMenuEvent;
import io.github.fishstiz.packed_packs.impl.PackedPacksApiImpl;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class ContextMenuEventImpl<P extends Enum<P>> extends ContextMenuEvent<P> {
    private final Map<P, ContextMenuItemBuilder> menuBuilders;
    private final boolean preferences;

    ContextMenuEventImpl(ScreenContext context, Class<P> positions, boolean preferences) {
        super(context);
        this.menuBuilders = new EnumMap<>(positions);
        this.preferences = preferences;
    }

    ContextMenuEventImpl(ScreenContext context, Class<P> positions) {
        this(context, positions, false);
    }

    public static ContextMenuEventImpl<Screen.Pos> postScreen(ScreenContext context) {
        var delegate = new ContextMenuEventImpl<>(context, Screen.Pos.class);
        PackedPacksApiImpl.getInstance().eventBus().post(new Screen(delegate));
        return delegate;
    }

    public static ContextMenuEventImpl<Preferences.Pos> postPreferences(ScreenContext context) {
        var delegate = new ContextMenuEventImpl<>(context, Preferences.Pos.class, true);
        PreferenceRegistry registry = PackedPacksApiImpl.getInstance().preferences();
        PackedPacksApiImpl.getInstance().eventBus().post(new Preferences(registry, delegate));
        return delegate;
    }

    public static ContextMenuEventImpl<PackEntry.Pos> postPackEntry(ScreenContext context, PackContext packContext) {
        var delegate = new ContextMenuEventImpl<>(context, PackEntry.Pos.class);
        PackedPacksApiImpl.getInstance().eventBus().post(new PackEntry(delegate, packContext));
        return delegate;
    }

    @Override
    public void addItem(P pos, Consumer<Item> itemConsumer) {
        ContextMenuItemImpl itemWrapper = new ContextMenuItemImpl();
        itemConsumer.accept(itemWrapper);
        this.menuBuilders.computeIfAbsent(pos, k -> new ContextMenuItemBuilder())
                .when(itemWrapper.separatorAbove)
                .ifTrue(ContextMenuItemBuilder::separator)
                .add(itemWrapper.itemBuilder.build())
                .when(itemWrapper.separatorBelow)
                .ifTrue(ContextMenuItemBuilder::separator);
    }

    @Override
    public void addToggle(P pos, Component label, BooleanSupplier valueSupplier, BooleanConsumer onChange) {
        MenuItemBuilder builder = MenuItem.builder(label)
                .icon(() -> GuiConstants.getToggleIcon(valueSupplier.getAsBoolean()))
                .action(() -> onChange.accept(!valueSupplier.getAsBoolean()));
        if (this.preferences) builder.background(GuiConstants.DEVELOPER_MODE_ITEM_BACKGROUND).closeOnInteract(false);
        this.menuBuilders.computeIfAbsent(pos, k -> new ContextMenuItemBuilder()).add(builder.build());
    }

    public @Nullable List<MenuItem> getItems(P pos) {
        ContextMenuItemBuilder menuBuilder = this.menuBuilders.get(pos);
        if (menuBuilder == null) return null;
        return menuBuilder.build();
    }

    static final class ContextMenuItemImpl implements Item {
        final MenuItemBuilder itemBuilder;
        boolean separatorBelow;
        boolean separatorAbove;

        ContextMenuItemImpl() {
            this.itemBuilder = MenuItem.builder(CommonComponents.EMPTY);
        }

        @Override
        public Item setLabel(Component label) {
            this.itemBuilder.text(label);
            return this;
        }

        @Override
        public Item setAction(Runnable action) {
            this.itemBuilder.action(action);
            return null;
        }

        @Override
        public Item setIcon(Identifier sprite) {
            this.itemBuilder.icon(new GuiSprite(sprite, 16, 16));
            return this;
        }

        @Override
        public Item setTooltip(Tooltip tooltip) {
            this.itemBuilder.tooltip(tooltip);
            return this;
        }

        @Override
        public Item addChild(Consumer<Item> itemConsumer) {
            var item = new ContextMenuItemImpl();
            itemConsumer.accept(item);
            this.itemBuilder.addChild(item.itemBuilder.build());
            return this;
        }

        @Override
        public Item addSeparatorBelow() {
            this.separatorBelow = true;
            return this;
        }

        @Override
        public Item addSeparatorAbove() {
            this.separatorAbove = true;
            return this;
        }

        @Override
        public Item applyDevStyle() {
            this.itemBuilder.background(GuiConstants.DEVELOPER_MODE_ITEM_BACKGROUND);
            return this;
        }
    }
}
