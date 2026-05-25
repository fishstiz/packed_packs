package io.github.fishstiz.packed_packs.transform.mixin;

import io.github.fishstiz.packed_packs.transform.interfaces.ChildScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.nio.file.Path;
import java.util.List;

@Mixin(PackSelectionScreen.class)
public interface PackSelectionScreenAccessor extends ChildScreen {
    @Accessor("model")
    PackSelectionModel packed_packs$model();

    @Invoker("reload")
    void packed_packs$reload();

    @Invoker("closeWatcher")
    void packed_packs$closeWatcher();

    @Accessor("packDir")
    Path packed_packs$getPackDir();

    @Invoker("copyPacks")
    static void packed_packs$copyPacks(Minecraft minecraft, List<Path> files, Path targetDir) {
        throw new AssertionError();
    }
}
