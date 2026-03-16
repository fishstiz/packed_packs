package io.github.fishstiz.testmod.tests;

import io.github.fishstiz.packed_packs.api.PackedPacksApi;
import io.github.fishstiz.packed_packs.api.events.InitializeEvent;
import io.github.fishstiz.testmod.TestFeature;
import io.github.fishstiz.testmod.gui.FocusPathRenderer;
import io.github.fishstiz.testmod.gui.HoveredElementRenderer;
import io.github.fishstiz.testmod.gui.TestFeatureRendererCoordinator;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class DebugScreenTest {
    private DebugScreenTest() {
    }

    public static void initialize(PackedPacksApi api, List<Identifier> dependencies) {
        TestFeature feature = TestFeature.builder(api.preferences(), "debug_screen", "Debug Screen")
                .defaultEnabled(false)
                .after(dependencies)
                .register(api.eventBus());

        TestFeature hoveredElement = feature.registerSubFeature("hovered_element", "Hovered Element");
        TestFeature focusPath = feature.registerSubFeature("focus_path", "Focus Path");

        api.eventBus().register(InitializeEvent.Pre.class, feature.id(), dependencies, event -> event.afterInit(post -> {
            TestFeatureRendererCoordinator coordinator = new TestFeatureRendererCoordinator();
            coordinator.add(new FocusPathRenderer(focusPath));
            coordinator.add(new HoveredElementRenderer(hoveredElement));
            post.screenContext().addRenderableOnly(coordinator);
        }));

        dependencies.add(feature.id());
    }
}
