package io.github.fishstiz.packed_packs.impl.events;

import io.github.fishstiz.fidgetz.v0.gui.components.FZPopoverMenuItem;
import io.github.fishstiz.packed_packs.api.Preference;
import io.github.fishstiz.packed_packs.api.context.PackContext;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.api.events.ContextMenuEvent;
import io.github.fishstiz.packed_packs.api.gui.ContextMenuItemSpec;
import io.github.fishstiz.packed_packs.impl.PackedPacksApiImpl;
import io.github.fishstiz.packed_packs.impl.gui.ContextMenuItemSpecImpl;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.*;
import java.util.function.Consumer;

public final class ContextMenuEventImpl<P extends Enum<P>> extends ContextMenuEvent.Positioned<P> {
    private final Map<P, List<FZPopoverMenuItem>> menuBuilders;
    private final boolean preferenceEvent;
    private Set<Preference<?>> preferences = Collections.emptySet();

    ContextMenuEventImpl(ScreenContext context, Class<P> positions, boolean preferenceEvent) {
        super(context);
        this.menuBuilders = new EnumMap<>(positions);
        this.preferenceEvent = preferenceEvent;
    }

    ContextMenuEventImpl(ScreenContext context, Class<P> positions) {
        this(context, positions, false);
    }

    public static ContextMenuEventImpl<Screen.Pos> postScreen(ScreenContext context) {
        ContextMenuEventImpl<Screen.Pos> delegate = new ContextMenuEventImpl<>(context, Screen.Pos.class);
        PackedPacksApiImpl.getInstance().eventBus().post(new Screen(delegate));
        return delegate;
    }

    public static ContextMenuEventImpl<Preferences.Pos> postPreferences(ScreenContext context) {
        ContextMenuEventImpl<Preferences.Pos> delegate = new ContextMenuEventImpl<>(context, Preferences.Pos.class, true);
        delegate.preferences = new ObjectOpenHashSet<>();
        PackedPacksApiImpl.getInstance().eventBus().post(new Preferences(delegate, delegate.preferences::add));
        return delegate;
    }

    public static ContextMenuEventImpl<PackEntry.Pos> postPackEntry(ScreenContext context, PackContext packContext) {
        ContextMenuEventImpl<PackEntry.Pos> delegate = new ContextMenuEventImpl<>(context, PackEntry.Pos.class);
        PackedPacksApiImpl.getInstance().eventBus().post(new PackEntry(delegate, packContext));
        return delegate;
    }

    @Override
    public void addItem(P pos, Consumer<ContextMenuItemSpec> configurator) {
        ContextMenuItemSpecImpl itemSpec = new ContextMenuItemSpecImpl(this.preferenceEvent);
        configurator.accept(itemSpec);
        itemSpec.apply(menuBuilders.computeIfAbsent(pos, ignored -> new ArrayList<>())::add);
    }

    public List<FZPopoverMenuItem> entries(P pos) {
        return menuBuilders.getOrDefault(pos, Collections.emptyList());
    }

    public Set<Preference<?>> getPreferences() {
        return this.preferences;
    }

    @Override
    protected P defaultPosition() {
        throw new UnsupportedOperationException("defaultPosition called from ContextMenuEventImpl, which should not happen");
    }
}
