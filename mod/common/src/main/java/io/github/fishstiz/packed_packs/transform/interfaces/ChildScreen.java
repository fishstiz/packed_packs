package io.github.fishstiz.packed_packs.transform.interfaces;

import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public interface ChildScreen {
    void packed_packs$setPrevious(Screen previous);

    @Nullable Screen packed_packs$getPrevious();
}
