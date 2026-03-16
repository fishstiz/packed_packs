package io.github.fishstiz.testmod.tests;

import io.github.fishstiz.packed_packs.api.PackedPacksApi;
import io.github.fishstiz.packed_packs.api.events.InitializePackEntryEvent;
import io.github.fishstiz.testmod.TestFeature;
import io.github.fishstiz.testmod.TestMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

import java.util.List;

public final class InitializePackEntryEventTest {
    private static Button createButton(String message) {
        return Button.builder(Component.literal(message), btn -> TestMod.LOGGER.info(message))
                .size(30, 20)
                .build();
    }

    public static void initialize(PackedPacksApi api, List<Identifier> dependencies) {
        TestFeature feature = TestFeature.builder(api.preferences(), "init_entry", "Initialize Pack Entry")
                .rebuildOnChange()
                .after(dependencies)
                .register(api.eventBus());

        api.eventBus().register(InitializePackEntryEvent.class, feature.id(), event -> {
            if (!feature.isEnabled()) return;

            String packId = event.packContext().pack().getId();

            switch (packId) {
                case "vanilla" -> event.addDetachedWidget(container -> new AbstractButton(0, 0, 60, 16, Component.literal("DETACHED WIDGET")) {
                    @Override
                    protected void renderContents(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                        int height = getHeight();
                        int x = container.getX() + 36;
                        int y = (container.getY() + container.getHeight()) - height;
                        setPosition(x, y);
                        renderDefaultSprite(guiGraphics);
                        renderDefaultLabel(guiGraphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE));
                    }

                    @Override
                    protected void updateWidgetNarration(@NonNull NarrationElementOutput narrationElementOutput) {
                        defaultButtonNarrationText(narrationElementOutput);
                    }

                    @Override
                    public void onPress(@NonNull InputWithModifiers inputWithModifiers) {
                        TestMod.LOGGER.info(this.getMessage().getString());
                    }
                });
                case "programmer_art" -> {
                    event.addCenterRight(createButton("CR 1"));
                    event.addCenterRight(createButton("CR 2"));
                }
                case "high_contrast" -> {
                    event.addTopRight(createButton("TR 1"));
                    event.addTopRight(createButton("TR 2"));
                }
                case "fabric", "mod_resources" -> {
                    event.anchorCenterRight(8, createButton("CRA 1"));
                    event.anchorCenterRight(20, createButton("CRA 2"));
                }
            }
        });

        dependencies.add(feature.id());
    }

    private InitializePackEntryEventTest() {
    }
}
