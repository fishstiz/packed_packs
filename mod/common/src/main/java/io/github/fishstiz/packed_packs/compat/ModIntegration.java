package io.github.fishstiz.packed_packs.compat;

import io.github.fishstiz.fidgetz.gui.components.contextmenu.MenuItem;
import io.github.fishstiz.packed_packs.api.PackedPacksInitializer;
import io.github.fishstiz.packed_packs.api.PreferenceRegistry;
import io.github.fishstiz.packed_packs.api.events.ScreenEvent;
import io.github.fishstiz.packed_packs.gui.components.ToggleableHelper;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public interface ModIntegration extends PackedPacksInitializer {
    ModContext mod();

    default @NonNull Identifier id() {
        return id(this.mod());
    }

    static Identifier id(ModContext mod) {
        return ResourceUtil.id(mod.getId());
    }

    static void addPreferenceToggle(
            ScreenEvent.OpenCtxMenu event,
            PreferenceRegistry preferences,
            PreferenceRegistry.Key<Boolean> key
    ) {
        event.getBuilder(ScreenEvent.OpenCtxMenu.Phase.PREFERENCES).add(MenuItem
                .builder(ResourceUtil.getText("preferences.widgets." + key.id().getPath()))
                .icon(() -> ToggleableHelper.getDefaultIcon(Boolean.TRUE.equals(preferences.get(key))))
                .background(GuiConstants.DEVELOPER_MODE_ITEM_BACKGROUND)
                .action(() -> preferences.set(key, !Boolean.TRUE.equals(preferences.get(key))))
                .build());
    }
}
