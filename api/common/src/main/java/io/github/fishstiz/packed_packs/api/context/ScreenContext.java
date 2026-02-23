package io.github.fishstiz.packed_packs.api.context;

import io.github.fishstiz.packed_packs.api.PreferenceRegistry;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.List;

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
     * Binds a widget to a {@code boolean} preference.
     * <p>
     * When in {@link #devMode()} wraps the widget with a colored overlay that reflects the preference value.
     * Otherwise, it returns the widget if the preference is enabled, and {@code null} if it is disabled.
     *
     * @param key    the preference key to check
     * @param widget the widget to bind
     * @return the bound widget, or {@code null} if it should be hidden
     */
    @Nullable
    AbstractWidget bindPreference(PreferenceRegistry.Key<Boolean> key, @Nullable AbstractWidget widget);
}
