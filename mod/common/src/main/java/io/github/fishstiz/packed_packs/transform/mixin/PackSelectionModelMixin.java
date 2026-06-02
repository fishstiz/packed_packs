package io.github.fishstiz.packed_packs.transform.mixin;

import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@Mixin(PackSelectionModel.class)
public abstract class PackSelectionModelMixin implements PackSelectionModelAccessor {
    @Shadow
    @Final
    private List<Pack> unselected;

    @Shadow
    @Final
    private List<Pack> selected;

    @Shadow
    @Final
    private PackRepository repository;

    @Shadow
    @Final
    private Function<Pack, Identifier> iconGetter;

    @Shadow
    @Final
    private Consumer<PackSelectionModel.EntryBase> onListChanged;

    @Shadow
    @Final
    private Consumer<PackRepository> output;

    @Override
    public void packed_packs$reset() {
        // needs to be reset when packs are updated in PackedPacksScreen, and user returns to original screen

        PackSelectionModelAccessor model = (PackSelectionModelAccessor) new PackSelectionModel(
                this.onListChanged,
                this.iconGetter,
                this.repository,
                this.output
        );

        this.selected.clear();
        this.selected.addAll(model.getSelectedPacks());

        this.unselected.clear();
        this.unselected.addAll(model.getSelectedPacks());
    }
}
