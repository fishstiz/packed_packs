package io.github.fishstiz.packed_packs.gui.components.contextmenu;

import io.github.fishstiz.fidgetz.gui.components.contextmenu.MenuItem;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

public record DirectoryMenuItem(Path directory, Component text) implements MenuItem {
    public DirectoryMenuItem(Path directory) {
        this(directory, Component.literal(directory.getFileName().toString()));
    }

    @Override
    public void run() {
        Util.getPlatform().openPath(this.directory);
    }
}