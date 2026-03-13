package io.github.fishstiz.packed_packs.api.context;

import io.github.fishstiz.packed_packs.api.Preference;
import io.github.fishstiz.packed_packs.api.gui.ContextMenuSink;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Provides contextual information to the current screen state.
 */
@ApiStatus.NonExtendable
public interface ScreenContext {
    /**
     * @return the currently displayed screen instance.
     */
    Screen screen();

    /**
     * <b>Note:</b> If the original screen is replaced, a new instance is created.
     *
     * @return the underlying vanilla pack selection screen.
     */
    PackSelectionScreen originalScreen();

    /**
     * @return the pack repository associated with this screen.
     */
    PackRepository packRepository();

    /**
     * @return the type of packs (e.g., Resource, Data) being managed.
     */
    PackType packType();

    /**
     * @return {@code true} if the current screen is managing resource packs.
     */
    default boolean isClientResources() {
        return this.packType() == PackType.CLIENT_RESOURCES;
    }

    /**
     * @return {@code true} if the current screen is managing data packs.
     */
    default boolean isServerData() {
        return this.packType() == PackType.SERVER_DATA;
    }

    /**
     * <b>Note:</b> Reflects the screen state; may not match with the pack repository.
     *
     * @return packs currently listed in the available column.
     */
    List<Pack> getAvailablePacks();

    /**
     * <b>Note:</b> Reflects the screen state; may not match with the pack repository.
     *
     * @return packs currently listed in the selected column.
     */
    List<Pack> getSelectedPacks();

    /**
     * Forces a reload of the underlying {@link PackRepository} to discover new or modified files.
     * <p>
     * Use this over {@link PackRepository#reload()} to update the GUI state.
     */
    void reload();

    /**
     * Applies the changes in the selected list and may trigger a resource/data reload.
     * <p>
     * <b>Note:</b> This closes the screen when data packs are being managed.
     */
    void commit();

    /**
     * @return {@code true} if the screen is in developer mode.
     */
    boolean devMode();

    /**
     * Rebuilds the {@link #screen}
     */
    void rebuild();

    /**
     * Adds a widget to the {@link #screen}
     */
    <T extends GuiEventListener & NarratableEntry> T addWidget(T widget);

    /**
     * Adds a renderable to the {@link #screen}
     */
    <T extends Renderable> T addRenderableOnly(T renderable);

    /**
     * Adds a renderable widget to the {@link #screen}
     */
    <T extends GuiEventListener & NarratableEntry & Renderable> T addRenderableWidget(T widget);

    /**
     * Removes a widget from the {@link #screen}
     */
    void removeWidget(GuiEventListener widget);

    /**
     * Returns the widget if the preference is enabled, or {@code null} if disabled.
     * <p>
     * When in {@link #devMode()}, always returns the widget wrapped with an overlay
     * indicating that it is toggleable, and a context menu item to toggle the preference
     * when right-clicked.
     *
     * @param key    the boolean preference key to check
     * @param widget the widget to wrap
     * @return the widget, a wrapped widget in dev mode, or {@code null} if disabled
     */
    @Nullable
    AbstractWidget wrapWidget(Preference<Boolean> key, Component label, @Nullable AbstractWidget widget);

    /**
     * Wraps a widget with a context menu that appears when it is right-clicked.
     * <p>
     * The provided {@code configurator} receives both the widget and the context menu sink,
     * allowing context menu items to be configured based on the widget's state.
     * <p>
     * <b>Note:</b> The widget must be a direct child of a non-injected {@link GuiEventListener},
     * or another {@code wrapWithContextMenu} wrapped widget.
     *
     * @param widget       the widget to wrap
     * @param configurator a consumer to configure the context menu, receiving the widget and the context menu sink
     * @return the wrapped widget
     */
    <T extends AbstractWidget> AbstractWidget wrapWithContextMenu(T widget, BiConsumer<T, ContextMenuSink> configurator);
}
