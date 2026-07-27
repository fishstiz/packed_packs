package io.github.fishstiz.packed_packs.compat.resourcify;

import dev.dediamondpro.resourcify.gui.injections.PackScreensAddition;
import dev.dediamondpro.resourcify.services.ProjectType;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ResourcifyButtons {
    private ResourcifyButtons() {
    }

    public static @Nullable List<? extends Button> getButtons(PackSelectionScreen packScreen) {
        ComponentContents contents = packScreen.getTitle().getContents();

        if (contents instanceof TranslatableContents translatable) {
            ProjectType type = PackScreensAddition.INSTANCE.getType(translatable.getKey());
            if (type != null) {
                return PackScreensAddition.INSTANCE.getButtons(packScreen, type);
            }
        }

        return null;
    }
}
