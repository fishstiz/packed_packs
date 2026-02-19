package io.github.fishstiz.packed_packs.compat;

import io.github.fishstiz.packed_packs.api.PackedPacksInitializer;
import io.github.fishstiz.packed_packs.api.PreferenceRegistry;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import net.minecraft.network.chat.Component;
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

    static Component getWidgetPrefText(PreferenceRegistry.Key<?> key) {
        return ResourceUtil.getText("preferences.widgets." + key.id().getPath());
    }
}
