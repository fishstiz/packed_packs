package io.github.fishstiz.testmod.tests;

import io.github.fishstiz.packed_packs.api.Event;
import io.github.fishstiz.packed_packs.api.PackedPacksApi;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.api.events.ContextMenuEvent;
import io.github.fishstiz.packed_packs.api.gui.ContextMenuItemSpec;
import io.github.fishstiz.testmod.TestFeature;
import io.github.fishstiz.testmod.TestMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.function.Consumer;

public final class EventBusTest {
    private EventBusTest() {
    }

    static class TestEvent extends ContextMenuEvent implements Event {
        private final ContextMenuItemSpec parent;

        protected TestEvent(ScreenContext context, ContextMenuItemSpec parent) {
            super(context);
            this.parent = parent;
        }

        @Override
        public void addItem(Consumer<ContextMenuItemSpec> configurator) {
            parent.child(configurator);
        }
    }

    public static void initialize(PackedPacksApi api, List<Identifier> dependencies) {
        TestFeature feature = TestFeature.builder(api.preferences(), "event_bus", "Event Bus")
                .after(dependencies)
                .register(api.eventBus());

        TestFeature ordering = feature.registerSubFeature("ordering", "Dependency Ordering");

        var first = TestMod.id("ordering_a");
        var second = TestMod.id("ordering_b");
        var third = TestMod.id("ordering_c");
        var last = TestMod.id("ordering_d");

        api.eventBus().register(ContextMenuEvent.Screen.class, ordering.id(), dependencies, event -> {
            if (!ordering.isEnabled()) return;
            event.addItem(item -> {
                item.label(Component.literal("Dependency Test")).applyDevStyle().separators();
                api.eventBus().post(new TestEvent(event.screenContext(), item));
            });
        });

        api.eventBus().register(TestEvent.class, last, second, event -> {
            if (!ordering.isEnabled()) return;
            event.addItem(item -> item
                    .label(Component.literal("[4] LAST (after 2)"))
                    .applyDevStyle());
        });

        api.eventBus().register(TestEvent.class, third, List.of(first, second), event -> {
            if (!ordering.isEnabled()) return;
            event.addItem(item -> item
                    .label(Component.literal("[3] THIRD (after 1 & 2)"))
                    .applyDevStyle());
        });

        api.eventBus().register(TestEvent.class, second, first, event -> {
            if (!ordering.isEnabled()) return;
            event.addItem(item -> item
                    .label(Component.literal("[2] SECOND (after 1)"))
                    .applyDevStyle());
        });

        api.eventBus().register(TestEvent.class, first, event -> {
            if (!ordering.isEnabled()) return;
            event.addItem(item -> item
                    .label(Component.literal("[1] FIRST (no dependency)"))
                    .separatorBelow()
                    .applyDevStyle());
        });

        dependencies.add(ordering.id());
    }
}
