package io.github.fishstiz.packed_packs.impl;

import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.api.PackedPacksApi;
import io.github.fishstiz.packed_packs.api.PackedPacksInitializer;
import io.github.fishstiz.packed_packs.platform.Services;

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
            PackedPacksApiImpl api = new PackedPacksApiImpl();

            List<PackedPacksInitializer> extensions = Services.PLATFORM.getModExtensions();
            for (PackedPacksInitializer extension : extensions) {
                try {
                    extension.onInitialize(api);
                } catch (Throwable e) {
                    PackedPacks.LOGGER.error(
                            "[packed_packs] PackedPacksInitializer implementation '{}' failed to initialize.",
                            extension.getClass().getSimpleName(), e
                    );
                }
            }

            api.eventBus.freeze();
            api.preferenceRegistry.freeze();

            INSTANCE = api;
        }
    }
}
