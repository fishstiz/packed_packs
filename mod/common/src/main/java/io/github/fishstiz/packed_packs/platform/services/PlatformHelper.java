package io.github.fishstiz.packed_packs.platform.services;

import com.google.common.base.Predicates;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public interface PlatformHelper {
    String getPlatform();

    Path getConfigDir();

    boolean isModLoaded(String id);

    boolean isDev();

    default <T> List<T> getServices(String key, Class<T> type) {
        return Collections.emptyList();
    }

    default <T> Predicate<T> getPredicate(Class<T> type) {
        return Predicates.alwaysFalse();
    }
}
