package io.github.fishstiz.packed_packs.platform;

import io.github.fishstiz.packed_packs.platform.services.PlatformHelper;

import java.util.ServiceLoader;

public class Services {
    public static final PlatformHelper PLATFORM = load(PlatformHelper.class);

    private Services() {
    }

    public static <T> T load(Class<T> clazz) {
        return ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
    }
}
