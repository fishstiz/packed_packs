package io.github.fishstiz.packed_packs.transform.mixin;

import io.github.fishstiz.packed_packs.transform.interfaces.ChildScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.nio.file.Path;

@Mixin(PackSelectionScreen.class)
public interface PackSelectionScreenAccessor extends ChildScreen {
    @Accessor("model")
    PackSelectionModel getModel();

    @Invoker("reload")
    void invokeReload();

    @Invoker("closeWatcher")
    void invokeCloseWatcher();

    @Accessor("packDir")
    Path packed_packs$getPackDir();
}
