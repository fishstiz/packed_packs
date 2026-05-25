package io.github.fishstiz.testmod.tests;

import io.github.fishstiz.packed_packs.api.PackedPacksApi;
import io.github.fishstiz.packed_packs.api.events.InitializePackEntryEvent;
import io.github.fishstiz.testmod.TestFeature;
import io.github.fishstiz.testmod.TestMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class InitializePackEntryEventTest {
    private static Button createButton(String message) {
        return Button.builder(Component.literal(message), btn -> TestMod.LOGGER.info(message))
                .size(30, 20)
                .build();
    }

    private static <T> T add(List<T> list, T obj) {
        list.add(obj);
        return obj;
    }

    public static void initialize(PackedPacksApi api, List<Identifier> dependencies) {
        TestFeature feature = TestFeature.builder(api.preferences(), "init_entry", "Initialize Pack Entry")
                .rebuildOnChange()
                .after(dependencies)
                .register(api.eventBus());

        TestFeature topEntries = feature.registerSubFeature("top_entry", "Top Widgets");
        TestFeature centerEntries = feature.registerSubFeature("center_entry", "Center Widgets");
        TestFeature bottomEntries = feature.registerSubFeature("bottom-entry", "Bottom Widgets");
        TestFeature detachedEntries = feature.registerSubFeature("detached_entry", "Detached Widgets");
        TestFeature anchoredRenderer = feature.registerSubFeature("anchored_renderer", "Render Anchor Distance");

        api.eventBus().register(InitializePackEntryEvent.class, feature.id(), event -> {
            if (!feature.isEnabled()) return;

            String packId = event.packContext().pack().getId();

            switch (packId) {
                case "vanilla" -> {
                    List<AbstractWidget> widgets = new ArrayList<>();
                    if (detachedEntries.isEnabled()) {
                        event.addDetachedWidget(container -> {
                            var widget = new AbstractButton(0, 0, 60, 16, Component.literal("DETACHED WIDGET")) {
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
                            };
                            widgets.add(widget);
                            return widget;
                        });
                    }
                    if (topEntries.isEnabled()) {
                        event.anchorTopRight(8, 2, add(widgets, createButton("ATR 1")));
                        event.anchorTopRight(20, 1, add(widgets, createButton("ATR 2")));
                    }
                    if (centerEntries.isEnabled()) {
                        event.anchorCenterRight(8, 2, add(widgets, createButton("ACR 1")));
                        event.anchorCenterRight(20, 1, add(widgets, createButton("ACR 2")));
                    }
                    if (centerEntries.isEnabled()) {
                        event.anchorBottomRight(8, 2, add(widgets, createButton("ABR 1")));
                        event.anchorBottomRight(20, 1, add(widgets, createButton("ABR 2")));
                    }
                    if (!widgets.isEmpty() && anchoredRenderer.isEnabled()) {
                        event.addDetachedRenderable(c -> new AnchorRenderer(c, widgets));
                    }
                }
                case "programmer_art" -> {
                    List<AbstractWidget> widgets = new ArrayList<>();
                    if (topEntries.isEnabled()) {
                        event.addTopRight(add(widgets, createButton("TR 1")));
                        event.addTopRight(add(widgets, createButton("TR 2")));
                    }
                    if (centerEntries.isEnabled()) {
                        event.addCenterRight(add(widgets, createButton("CR 1")));
                        event.addCenterRight(add(widgets, createButton("CR 2")));
                    }
                    if (bottomEntries.isEnabled()) {
                        event.addBottomRight(add(widgets, createButton("BR 1")));
                        event.addBottomRight(add(widgets, createButton("BR 2")));
                    }
                    if (!widgets.isEmpty() && anchoredRenderer.isEnabled()) {
                        event.addDetachedRenderable(c -> new AnchorRenderer(c, widgets));
                    }
                }
                case "high_contrast" -> {
                    List<AbstractWidget> widgets = new ArrayList<>();
                    if (topEntries.isEnabled()) {
                        event.addTopRight(8, add(widgets, createButton("TR 1")));
                        event.addTopRight(12, add(widgets, createButton("TR 2")));
                    }
                    if (centerEntries.isEnabled()) {
                        event.addCenterRight(8, add(widgets, createButton("CR 1")));
                        event.addCenterRight(12, add(widgets, createButton("CR 2")));
                    }
                    if (bottomEntries.isEnabled()) {
                        event.addBottomRight(8, add(widgets, createButton("BR 1")));
                        event.addBottomRight(12, add(widgets, createButton("BR 2")));
                    }
                    if (!widgets.isEmpty() && anchoredRenderer.isEnabled()) {
                        event.addDetachedRenderable(c -> new AnchorRenderer(c, widgets));
                    }
                }
                case "fabric", "mod_resources" -> {
                    List<AbstractWidget> widgets = new ArrayList<>();
                    event.anchorCenterRight(8, add(widgets, createButton("CRA 1")));
                    event.anchorCenterRight(20, add(widgets, createButton("CRA 2")));
                    if (!widgets.isEmpty() && anchoredRenderer.isEnabled()) {
                        event.addDetachedRenderable(c -> new AnchorRenderer(c, widgets));
                    }
                }
            }
        });

        dependencies.add(feature.id());
    }

    private record AnchorRenderer(LayoutElement container, List<AbstractWidget> widgets) implements Renderable {
        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            for (AbstractWidget widget : widgets) {
                int wx = widget.getX();
                int wy = widget.getY();
                int ww = widget.getWidth();
                int wh = widget.getHeight();
                int wCenterX = wx + ww / 2;
                int wCenterY = wy + wh / 2;

                int containerTop = container.getY();
                int containerRight = container.getX() + container.getWidth();
                int containerBottom = container.getY() + container.getHeight();

                int topDist = wy - containerTop;

                Font font = Minecraft.getInstance().font;

                if (topDist > 0) {
                    graphics.vLine(wCenterX, containerTop, wy, CommonColors.RED);
                    graphics.drawString(font, String.valueOf(topDist), wCenterX, containerTop + topDist / 2, CommonColors.WHITE);
                }

                int rightDist = containerRight - (wx + ww);
                if (rightDist > 0) {
                    graphics.hLine(wx + ww, containerRight, wCenterY, CommonColors.GREEN);
                    graphics.drawString(font, String.valueOf(rightDist), wx + ww + rightDist / 2, wCenterY, CommonColors.WHITE);
                }

                int bottomDist = containerBottom - (wy + wh);
                if (bottomDist > 0) {
                    graphics.vLine(wCenterX, wy + wh, containerBottom, CommonColors.BLUE);
                    graphics.drawString(font, String.valueOf(bottomDist), wCenterX, wy + wh + bottomDist / 2, CommonColors.WHITE);
                }
            }
        }
    }

    private InitializePackEntryEventTest() {
    }
}
