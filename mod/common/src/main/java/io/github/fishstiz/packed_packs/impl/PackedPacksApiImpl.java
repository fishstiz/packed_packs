package io.github.fishstiz.packed_packs.impl;

import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.api.PackedPacksApi;
import io.github.fishstiz.packed_packs.api.PackedPacksInitializer;
import io.github.fishstiz.packed_packs.platform.Services;
import net.minecraft.util.Util;

import java.util.List;

public final class PackedPacksApiImpl implements PackedPacksApi {
    private final EventBusImpl eventBus = new EventBusImpl();
    private final PreferenceRegistryImpl preferenceRegistry = new PreferenceRegistryImpl();

    private PackedPacksApiImpl() {
    }

    public static PackedPacksApiImpl getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public PreferenceRegistryImpl preferences() {
        return this.preferenceRegistry;
    }

    @Override
    public EventBusImpl eventBus() {
        return this.eventBus;
    }

    private static final class Holder {
        private static final PackedPacksApiImpl INSTANCE;

        private Holder() {
        }

        static {
            long start = Util.getNanos();

            PackedPacksApiImpl api = new PackedPacksApiImpl();

            long section = Util.getNanos();
            List<PackedPacksInitializer> extensions = Services.PLATFORM.getModExtensions();
            PackedPacks.LOGGER.info("[packed_packs] FETCH EXT TOOK {}ms", (Util.getNanos() - section) / 1_000_000);

            for (PackedPacksInitializer extension : extensions) {
                try {
                    section = Util.getNanos();
                    extension.onInitialize(api);
                    PackedPacks.LOGGER.info("[packed_packs] '{}' INIT TOOK {}ms", extension.getClass().getName(), (Util.getNanos() - section) / 1_000_000);
                } catch (Throwable e) {
                    PackedPacks.LOGGER.error(
                            "[packed_packs] PackedPacksInitializer implementation '{}' failed to initialize.",
                            extension.getClass().getSimpleName(), e
                    );
                }
            }

            section = Util.getNanos();
            api.eventBus.freeze();
            PackedPacks.LOGGER.info("[packed_packs] EVENT BUS FREEZE TOOK {}ms", (Util.getNanos() - section) / 1_000_000);

            section = Util.getNanos();
            api.preferenceRegistry.freeze();
            PackedPacks.LOGGER.info("[packed_packs] PREFS FREEZE TOOK {}ms", (Util.getNanos() - section) / 1_000_000);

            PackedPacks.LOGGER.info("[packed_packs] API INIT TOOK {}ms", (Util.getNanos() - start) / 1_000_000);

            INSTANCE = api;
        }
    }
}
