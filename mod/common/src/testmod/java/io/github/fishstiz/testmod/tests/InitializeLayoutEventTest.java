package io.github.fishstiz.testmod.tests;

import io.github.fishstiz.packed_packs.api.PackedPacksApi;
import io.github.fishstiz.packed_packs.api.events.InitializeLayoutEvent;
import io.github.fishstiz.testmod.TestFeature;
import io.github.fishstiz.testmod.TestMod;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class InitializeLayoutEventTest {
    private InitializeLayoutEventTest() {
    }

    private static AbstractWidget btn(Component message) {
        return Button.builder(message, btn -> TestMod.LOGGER.info(message.getString())).size(30, 20).build();
    }

    private static void addFeatureButton(String message, TestFeature feature, InitializeLayoutEvent event, InitializeLayoutEvent.Pos pos) {
        Component component = Component.literal(message);
        AbstractWidget btn = event.screenContext().wrapWidget(feature.preference(), feature.label(), btn(component));
        if (btn != null) event.addWidget(pos, btn);
    }

    public static void initialize(PackedPacksApi api, List<Identifier> dependencies) {
        TestFeature feature = TestFeature.builder(api.preferences(), "init_layout", "Initialize Layout")
                .rebuildOnChange()
                .after(dependencies)
                .register(api.eventBus());

        TestFeature afterTitle = feature.registerSubFeature("after_title", "After Title");
        TestFeature beforeFooter = feature.registerSubFeature("before_footer", "Before Footer");
        TestFeature afterLeftFooter = feature.registerSubFeature("after_left_footer", "After Left Footer");
        TestFeature beforeRightFooter = feature.registerSubFeature("before_right_footer", "Before Right Footer");
        TestFeature betweenRightFooter = feature.registerSubFeature("between_right_footer", "Between Right Footer");
        TestFeature afterFooter = feature.registerSubFeature("after_footer", "After Footer");

        api.eventBus().register(InitializeLayoutEvent.class, feature.id(), dependencies, event -> {
            if (!feature.isEnabled()) return;

            addFeatureButton("AT", afterTitle, event, InitializeLayoutEvent.Pos.AFTER_TITLE);
            addFeatureButton("BF", beforeFooter, event, InitializeLayoutEvent.Pos.BEFORE_FOOTER);
            addFeatureButton("ALF", afterLeftFooter, event, InitializeLayoutEvent.Pos.AFTER_LEFT_FOOTER);
            addFeatureButton("BRF", beforeRightFooter, event, InitializeLayoutEvent.Pos.BEFORE_RIGHT_FOOTER);
            addFeatureButton("BWRF", betweenRightFooter, event, InitializeLayoutEvent.Pos.BETWEEN_RIGHT_FOOTER);
            addFeatureButton("AF", afterFooter, event, InitializeLayoutEvent.Pos.AFTER_FOOTER);
        });

        dependencies.add(feature.id());
    }
}
