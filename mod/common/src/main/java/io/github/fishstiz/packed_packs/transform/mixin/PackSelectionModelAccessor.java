package io.github.fishstiz.packed_packs.transform.mixin;

import io.github.fishstiz.packed_packs.transform.interfaces.FilterableModel;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.function.Consumer;

/**
 * fabric workaround
 *
 * @see
 * <a href="https://github.com/FabricMC/fabric/blob/1.21.1/fabric-resource-loader-v0/src/client/java/net/fabricmc/fabric/mixin/resource/loader/client/ResourcePackOrganizerMixin.java#L37">
 * ResourcePackOrganizerMixin
 * </a>
 */
@Mixin(PackSelectionModel.class)
public interface PackSelectionModelAccessor extends FilterableModel {
    @Accessor("selected")
    List<Pack> getSelectedPacks();

    @Accessor("unselected")
    List<Pack> getUnselectedPacks();

    @Accessor("repository")
    PackRepository packed_packs$getRepository();

    @Accessor("output")
    Consumer<PackRepository> packed_packs$getOutput();
}
