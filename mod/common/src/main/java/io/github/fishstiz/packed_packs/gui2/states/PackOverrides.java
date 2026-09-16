package io.github.fishstiz.packed_packs.gui2.states;

import io.github.fishstiz.packed_packs.pack.ProfileScope;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.Nullable;

public record PackOverrides( // todo remove?
        Scoped<Boolean> hidden,
        Scoped<@Nullable Boolean> required,
        Scoped<Pack.@Nullable Position> position
) {
    record Scoped<T>(T value, ProfileScope scope) {
    }
}
