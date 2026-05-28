package io.github.fishstiz.testmod;

import com.google.common.collect.Lists;
import io.github.fishstiz.packed_packs.api.PackedPacksApi;
import io.github.fishstiz.packed_packs.api.PackedPacksInitializer;
import io.github.fishstiz.packed_packs.api.events.ContextMenuEvent;
import io.github.fishstiz.packed_packs.api.events.InitializeEvent;
import io.github.fishstiz.testmod.events.ContextMenuTestFeatureEvent;
import io.github.fishstiz.testmod.tests.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class PackedPacksApiTest implements PackedPacksInitializer {
    private static final Identifier ID = TestMod.id("packed_packs_test_integration");

    @Override
    public void onInitialize(PackedPacksApi api) {
        TestMod.LOGGER.info("PackedPacksApiTest initialization started.");
        long start = System.nanoTime();

        List<Identifier> dependencies = Lists.newArrayList(ID);
        EventBusTest.initialize(api, dependencies);
        PreferenceRegistryTest.initialize(api, dependencies);
        DebugScreenTest.initialize(api, dependencies);
        TestPacks.initialize(api, dependencies);
        ContextMenuEventTest.initialize(api, dependencies);
        InitializeLayoutEventTest.initialize(api, dependencies);
        InitializePackEntryEventTest.initialize(api, dependencies);

        api.eventBus().register(InitializeEvent.Pre.class, ID, event -> {
            long[] initStart = {System.nanoTime()};
            TestMod.LOGGER.info("Screen initialization started.");
            event.afterInit(post -> TestMod.LOGGER.info("Screen initialization took {}ms.", diffMs(initStart[0])));
        });

        api.eventBus().register(ContextMenuEvent.Screen.class, ID, event -> {
            event.addItem(ContextMenuEvent.Screen.Pos.TOP, item -> item
                    .label(Component.literal("Rebuild Screen"))
                    .action(event.screenContext()::rebuild)
                    .applyDevStyle()
                    .separatorBelow());
            event.addItem(ContextMenuEvent.Screen.Pos.TOP, item -> {
                ContextMenuTestFeatureEvent featureEvent = api.eventBus().post(new ContextMenuTestFeatureEvent(event.screenContext(), item));
                item.icon(Identifier.withDefaultNamespace("icon/unseen_notification"));
                item.active(featureEvent::hasChildren);
                item.label(featureEvent.hasChildren() ? Component.literal("Test Features") : Component.literal("No Test Features"));
                item.separatorAbove();
                item.applyDevStyle();
            });
        });

        TestMod.LOGGER.info("PackedPacksApiTest initialization took {}ms.", diffMs(start));
    }

    private static long diffMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
