package io.github.fishstiz.packed_packs.gui.screens;

import io.github.fishstiz.fidgetz.gui.components.Modal;
import io.github.fishstiz.fidgetz.gui.components.ToggleableDialog;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenu;
import io.github.fishstiz.fidgetz.gui.renderables.ColoredRect;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.gui.components.pack.*;
import io.github.fishstiz.packed_packs.gui.components.profile.ProfilesSidebar;
import io.github.fishstiz.packed_packs.gui.layouts.OptionsLayout;
import io.github.fishstiz.packed_packs.gui.layouts.PackLayout;
import io.github.fishstiz.packed_packs.gui.model.PackListType;
import io.github.fishstiz.packed_packs.gui.model.PackedPacksViewModel;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.SPACING;

/**
 * maybe hacky or overkill, but reduces first constructor call of {@link PackedPacksScreen} by ~25%.
 */
record Components(
        ProfilesSidebar profilesSidebar,
        PackLayout availableLayout,
        PackLayout enabledLayout,
        FileRenameModal renameModal,
        ContextMenu contextMenu,
        Modal<OptionsLayout> optionsModal,
        @Nullable PackAliasModal aliasModal
) {
    private static boolean warmed = false;

    private static <T> CompletableFuture<T> async(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, Util.backgroundExecutor());
    }

    static Components create(PackedPacksScreen screen, ScreenContext ctx, PackedPacksViewModel viewModel) {
        if (warmed) {
            return new Components(
                    new ProfilesSidebar(screen, viewModel.createProfilesSlice()),
                    new PackLayout(ctx, viewModel, PackListType.AVAILABLE),
                    new PackLayout(ctx, viewModel, PackListType.ENABLED),
                    new FileRenameModal(screen, viewModel),
                    buildMenu(screen),
                    buildOptionsModal(screen, screen::getMaxHeight, Config.packs(ctx.packType())),
                    buildAliasModal(screen, viewModel)
            );
        }

        var profiles = async(() -> new ProfilesSidebar(screen, viewModel.createProfilesSlice()));
        var available = async(() -> new PackLayout(ctx, viewModel, PackListType.AVAILABLE));
        var enabled = async(() -> new PackLayout(ctx, viewModel, PackListType.ENABLED));
        var rename = async(() -> new FileRenameModal(screen, viewModel));
        var menu = async(() -> buildMenu(screen));
        var alias = async(() -> buildAliasModal(screen, viewModel));
        var optionsModal = async(() -> buildOptionsModal(screen, screen::getMaxHeight, Config.packs(ctx.packType())));

        CompletableFuture.allOf(available, enabled, rename, menu, alias, profiles).join();

        warmed = true;

        return new Components(profiles.join(), available.join(), enabled.join(), rename.join(), menu.join(), optionsModal.join(), alias.join());
    }

    private static ContextMenu buildMenu(PackedPacksScreen screen) {
        return ContextMenu.builder(screen)
                .setSpacing(SPACING)
                .setBackground(Theme.GRAY_800.getARGB())
                .setBorderColor(Theme.GRAY_500.getARGB())
                .build();
    }

    private static @Nullable PackAliasModal buildAliasModal(PackedPacksScreen screen, PackedPacksViewModel viewModel) {
        if (!Config.get().isDevMode()) return null;
        return new PackAliasModal(screen, viewModel);
    }

    private static Modal<OptionsLayout> buildOptionsModal(PackedPacksScreen screen, IntSupplier maxHeight, Config.Packs config) {
        return Modal.builder(screen, new OptionsLayout(Minecraft.getInstance(), maxHeight, config))
                .setBackdrop(new ColoredRect(Theme.BLACK.withAlpha(0.5f)))
                .setCaptureFocus(true)
                .padding(SPACING)
                .build();
    }

    List<ToggleableDialog<?>> dialogs() {
        return this.aliasModal != null
                ? List.of(this.optionsModal, this.contextMenu, this.aliasModal, this.renameModal, this.profilesSidebar)
                : List.of(this.optionsModal, this.contextMenu, this.renameModal, this.profilesSidebar);
    }
}
