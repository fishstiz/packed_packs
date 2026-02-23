package io.github.fishstiz.packed_packs.gui.model;

import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.packed_packs.config.PackConfigs;
import io.github.fishstiz.packed_packs.pack.PackOptionsContext;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import net.minecraft.server.packs.repository.Pack;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public record PackListContext(
        PackConfigs configs,
        PackOptionsContext options,
        BiConsumer<FolderPack, List<Pack>> folderSaver,
        Predicate<Pack> fileModifiable,
        Function<Pack, Sprite> iconFactory
) {
}
