package io.github.fishstiz.packed_packs.api.events;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Supplier;

/**
 * Provides access to the current screen state and environment details.
 */
public final class ScreenContext {
    private final Screen screen;
    private final Supplier<PackSelectionScreen> originalScreen;
    private final PackRepository repository;
    private final PackType packType;
    private final boolean devMode;

    @ApiStatus.Internal
    public ScreenContext(
            Screen screen,
            Supplier<PackSelectionScreen> originalScreen,
            PackRepository repository,
            PackType packType,
            boolean devMode
    ) {
        this.screen = screen;
        this.originalScreen = originalScreen;
        this.repository = repository;
        this.packType = packType;
        this.devMode = devMode;
    }

    /**
     * @return the currently displayed screen instance.
     */
    public Screen getScreen() {
        return this.screen;
    }

    /**
     * @return the pack repository associated with this screen.
     */
    public PackRepository getRepository() {
        return this.repository;
    }

    /**
     * <b>Note:</b> If the original screen is replaced,
     * a new instance is created solely to satisfy contracts.
     * Cache this value if a consistent reference is required.
     *
     * @return the underlying vanilla pack selection screen.
     */
    public PackSelectionScreen getOriginalScreen() {
        return this.originalScreen.get();
    }

    /**
     * @return the type of packs (e.g., Resource, Data) being managed.
     */
    public PackType getPackType() {
        return this.packType;
    }

    /**
     * @return {@code true} if the screen is in developer mode.
     */
    public boolean isDevMode() {
        return this.devMode;
    }
}
