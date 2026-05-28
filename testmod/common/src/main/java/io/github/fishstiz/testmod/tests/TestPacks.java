package io.github.fishstiz.testmod.tests;

import io.github.fishstiz.packed_packs.api.PackedPacksApi;
import io.github.fishstiz.packed_packs.api.events.ClosingEvent;
import io.github.fishstiz.testmod.TestFeature;
import io.github.fishstiz.testmod.config.Configuration;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class TestPacks {
    public static void initialize(PackedPacksApi api, List<Identifier> dependencies) {
        TestFeature feature = TestFeature.builder(api.preferences(), "test_packs", "Test Packs")
                .after(dependencies)
                .defaultEnabled(Configuration.getInstance().shouldAddTestPacks())
                .onToggle((value, ctx) -> {
                    boolean previous = Configuration.getInstance().shouldAddTestPacks();
                    Configuration.getInstance().setShouldAddTestPacks(value);
                    if (previous != value) ctx.reload();
                })
                .register(api.eventBus());

        feature.setEnabled(Configuration.getInstance().shouldAddTestPacks());

        api.eventBus().register(ClosingEvent.class, feature.id(), dependencies, event -> Configuration.getInstance().save());

        dependencies.add(feature.id());
    }

    private TestPacks() {
    }
}
