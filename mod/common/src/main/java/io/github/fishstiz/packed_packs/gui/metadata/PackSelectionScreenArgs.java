package io.github.fishstiz.packed_packs.gui.metadata;

import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionModelAccessor;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;

import java.nio.file.Path;
import java.util.function.Consumer;

public record PackSelectionScreenArgs(
        PackRepository repository,
        Consumer<PackRepository> output,
        Path packDir,
        Component title
) {
    public static PackSelectionScreenArgs extract(PackSelectionScreen screen) {
        PackSelectionScreenAccessor packScreen = (PackSelectionScreenAccessor) screen;
        PackSelectionModelAccessor model = (PackSelectionModelAccessor) packScreen.getModel();
        return new PackSelectionScreenArgs(
                model.packed_packs$getRepository(),
                model.packed_packs$getOutput(),
                packScreen.packed_packs$getPackDir(),
                screen.getTitle()
        );
    }

    public PackType packType() {
        return this.repository == Minecraft.getInstance().getResourcePackRepository()
                ? PackType.CLIENT_RESOURCES
                : PackType.SERVER_DATA;
    }

    public PackSelectionScreen createScreen() {
        return new PackSelectionScreen(this.repository, this.output, this.packDir, this.title);
    }

    public PackSelectionScreen createScreen(Screen previous) {
        PackSelectionScreen packScreen = this.createScreen();
        ((PackSelectionScreenAccessor) packScreen).packed_packs$setPrevious(previous);
        return packScreen;
    }

    public PackSelectionScreen createDummy() {
        PackSelectionScreen packScreen = this.createScreen();
        ((PackSelectionScreenAccessor) packScreen).invokeCloseWatcher();
        return packScreen;
    }
}
