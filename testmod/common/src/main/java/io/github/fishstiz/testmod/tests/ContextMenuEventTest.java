package io.github.fishstiz.testmod.tests;

import io.github.fishstiz.packed_packs.api.Event;
import io.github.fishstiz.packed_packs.api.PackedPacksApi;
import io.github.fishstiz.packed_packs.api.Preference;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.api.events.ContextMenuEvent;
import io.github.fishstiz.packed_packs.api.gui.ContextMenuItemSpec;
import io.github.fishstiz.testmod.TestFeature;
import io.github.fishstiz.testmod.TestMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.function.Consumer;

public final class ContextMenuEventTest {
    private ContextMenuEventTest() {
    }

    static class TestEvent extends ContextMenuEvent implements Event {
        private final ContextMenuItemSpec parent;

        public TestEvent(ScreenContext context, ContextMenuItemSpec parent) {
            super(context);
            this.parent = parent;
        }

        @Override
        public void addItem(Consumer<ContextMenuItemSpec> configurator) {
            this.parent.child(configurator);
        }
    }

    public static void initialize(PackedPacksApi api, List<Identifier> dependencies) {
        TestFeature feature = TestFeature.builder(api.preferences(), "context_menu", "Context Menu")
                .after(dependencies)
                .register(api.eventBus());

        TestFeature separators = feature.registerSubFeature("separators", "Separators");
        TestFeature nested = feature.registerSubFeature("nested", "Nested Items");
        TestFeature icons = feature.registerSubFeature("icons", "Icons");
        TestFeature preferences = feature.registerSubFeature("preferences", "Preferences");
        TestFeature packEntry = feature.registerSubFeature("pack_entry", "Pack Entry");


        api.eventBus().register(ContextMenuEvent.Screen.class, feature.id(), dependencies, event -> {
            if (!feature.isEnabled()) return;

            event.addItem(item -> {
                item.label(Component.literal("Context Menu Tests")).applyDevStyle().separatorAbove();
                api.eventBus().post(new TestEvent(event.screenContext(), item));
            });
        });

        api.eventBus().register(TestEvent.class, separators.id(), event -> {
            if (!separators.isEnabled()) return;

            event.addItem(item -> item.label(Component.literal("BEGIN SEPARATORS")).separatorAbove().applyDevStyle());

            event.addItem(item -> item.label(Component.literal("SEP BELOW")).separatorBelow());
            event.addItem(item -> item.label(Component.literal("SEP ABOVE")));
            event.addItem(item -> item.label(Component.literal("NO SEP")));
            event.addItem(item -> item.label(Component.literal("SEP BELOW")));
            event.addItem(item -> item.label(Component.literal("SEP ABOVE AND BELOW")).separatorAbove().separatorBelow());

            event.addItem(item -> item.label(Component.literal("SEP ABOVE PARENT"))
                    .child(child -> child.label(Component.literal("SEP BELOW")).separatorBelow())
                    .child(child -> child.label(Component.literal("SEP ABOVE")))
                    .child(child -> child.label(Component.literal("NO SEP")))
                    .child(child -> child.label(Component.literal("SEB BELOW")).separatorBelow())
                    .child(child -> child.label(Component.literal("SEP ABOVE AND BELOW")).separatorBelow().separatorAbove())
                    .child(child -> child.label(Component.literal("SEP ABOVE")).separatorAbove())
                    .child(child -> child.label(Component.literal("NO SEP")))
                    .child(child -> child.label(Component.literal("SEP BELOW")))
                    .child(child -> child.label(Component.literal("SEB ABOVE")).separatorAbove()));

            event.addItem(item -> item.label(Component.literal("END SEPARATORS")).separatorAbove().applyDevStyle());
        });

        api.eventBus().register(TestEvent.class, nested.id(), event -> {
            if (!nested.isEnabled()) return;

            event.addItem(item -> item.label(Component.literal("BEGIN NESTED")).separatorAbove().applyDevStyle());
            event.addItem(item -> item
                    .label(Component.literal("GRANDPARENT"))
                    .child(parent -> parent
                            .label(Component.literal("PARENT 1"))
                            .child(child -> child.label(Component.literal("CHILD 1")))
                            .child(child -> child.label(Component.literal("CHILD 2"))))
                    .child(parent -> parent
                            .label(Component.literal("PARENT 2"))
                            .child(child -> child.label(Component.literal("CHILD 1")))));
            event.addItem(item -> item.label(Component.literal("SUPER LONG NAME CONTEXT MENU ITEM PARENT"))
                    .child(child -> child.label(Component.literal("SUPER LONG NAME CONTEXT MENU ITEM NODE"))));

            event.addItem(item -> item.label(Component.literal("END NESTED")).separatorAbove().applyDevStyle());
        });

        api.eventBus().register(TestEvent.class, icons.id(), event -> {
            if (!icons.isEnabled()) return;

            event.addItem(item -> item.label(Component.literal("BEGIN ICONS")).separatorAbove().applyDevStyle());
            event.addItem(item -> item.label(Component.literal("ICON: LANGUAGE"))
                    .icon(Identifier.withDefaultNamespace("icon/language")));
            event.addItem(item -> item.label(Component.literal("ICON: ACCESSIBILITY"))
                    .icon(Identifier.withDefaultNamespace("icon/accessibility")));
            event.addItem(item -> item.label(Component.literal("ICON WITH CHILD"))
                    .icon(Identifier.withDefaultNamespace("icon/language"))
                    .child(child -> child.label(Component.literal("CHILD WITH ICON"))
                            .icon(Identifier.withDefaultNamespace("icon/accessibility"))));
            event.addItem(item -> item.label(Component.literal("END ICONS")).separatorAbove().applyDevStyle());
        });

        Preference<Boolean> boolPref = api.preferences().register(TestMod.id("context_menu_bool_pref"), true);
        Preference<Boolean> boolPref2 = api.preferences().register(TestMod.id("context_menu_bool_pref_2"), false);
        api.eventBus().register(ContextMenuEvent.Preferences.class, preferences.id(), dependencies, event -> {
            if (!preferences.isEnabled()) return;

            event.addToggle(boolPref, Component.literal("BOOL PREF 1"));
            event.addToggle(ContextMenuEvent.Preferences.Pos.TOP, boolPref2, Component.literal("BOOL PREF 2 (TOP)"));
            event.addItem(item -> event.applyToggle(item, boolPref)
                    .label(Component.literal("APPLY TOGGLE ON CHILD PARENT"))
                    .child(child -> event.applyToggle(child, boolPref2)
                            .label(Component.literal("APPLY TOGGLE ON CHILD"))));
        });

        api.eventBus().register(ContextMenuEvent.PackEntry.class, packEntry.id(), dependencies, event -> {
            if (!packEntry.isEnabled()) return;

            event.addItem(ContextMenuEvent.PackEntry.Pos.BEFORE_HEADER, item -> item
                    .label(Component.literal("BEFORE HEADER")));
            event.addItem(ContextMenuEvent.PackEntry.Pos.AFTER_HEADER, item -> item
                    .label(Component.literal("AFTER HEADER")));
            event.addItem(ContextMenuEvent.PackEntry.Pos.AFTER_DEV, item -> item
                    .label(Component.literal("AFTER DEV"))
                    .applyDevStyle());
            event.addItem(ContextMenuEvent.PackEntry.Pos.AFTER_PACK, item -> item
                    .label(Component.literal("AFTER PACK"))
                    .child(child -> child.label(Component.literal("CHILD"))));
        });

        dependencies.add(feature.id());
        dependencies.add(preferences.id());
        dependencies.add(packEntry.id());
    }
}
