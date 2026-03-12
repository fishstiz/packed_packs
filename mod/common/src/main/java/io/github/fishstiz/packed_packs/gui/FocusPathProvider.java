package io.github.fishstiz.packed_packs.gui;

import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jspecify.annotations.Nullable;

public interface FocusPathProvider extends GuiEventListener {
    @Nullable ComponentPath getFocusPath(FocusTarget target);
}
