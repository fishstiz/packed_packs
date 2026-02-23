package io.github.fishstiz.packed_packs.compat;

import io.github.fishstiz.fidgetz.util.lang.FunctionsUtil;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.api.PackedPacksApi;
import io.github.fishstiz.packed_packs.api.PackedPacksInitializer;
import io.github.fishstiz.packed_packs.api.PreferenceRegistry;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Constructor;

public abstract class ModIntegration implements PackedPacksInitializer {
    protected abstract ModContext mod();

    protected abstract void onInitLoaded(PackedPacksApi api);

    protected @NonNull Identifier id() {
        return id(this.mod());
    }

    @Override
    public final void onInitialize(@NonNull PackedPacksApi api) {
        if (this.mod().isLoaded()) {
            this.onInitLoaded(api);
        }
    }

    public static Identifier id(ModContext mod) {
        return ResourceUtil.id(mod.getId());
    }

    public static Component getWidgetPrefText(PreferenceRegistry.Key<?> key) {
        return ResourceUtil.getText("preferences.widgets." + key.id().getPath());
    }

    public static Runnable createScreenSetter(String className, ScreenArg<?>... screenArgs) {
        try {
            Object[] args = new Object[screenArgs.length];
            Class<?>[] argTypes = new Class[screenArgs.length];
            for (int i = 0; i < screenArgs.length; i++) {
                args[i] = screenArgs[i].arg();
                argTypes[i] = screenArgs[i].type();
            }

            Constructor<? extends Screen> screenCtor = Class.forName(className)
                    .asSubclass(Screen.class)
                    .getConstructor(argTypes);

            return () -> {
                try {
                    Minecraft.getInstance().setScreen(screenCtor.newInstance(args));
                } catch (ReflectiveOperationException e) {
                    PackedPacks.LOGGER.error("[packed_packs] Error opening mod screen: '{}'", className, e);
                }
            };
        } catch (ReflectiveOperationException e) {
            PackedPacks.LOGGER.error("[packed_packs] Failed to create screen setter for mod screen: '{}'", className, e);
            return FunctionsUtil.nop();
        }
    }

    public record ScreenArg<T>(Class<T> type, T arg) {
        public static ScreenArg<Screen> parent(Screen parent) {
            return new ScreenArg<>(Screen.class, parent);
        }
    }
}
