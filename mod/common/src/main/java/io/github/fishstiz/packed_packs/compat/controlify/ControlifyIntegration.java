package io.github.fishstiz.packed_packs.compat.controlify;

import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.api.entrypoint.ControlifyEntrypoint;
import dev.isxander.controlify.api.entrypoint.InitContext;
import dev.isxander.controlify.api.entrypoint.PreInitContext;
import dev.isxander.controlify.screenop.ComponentProcessorProvider;
import dev.isxander.controlify.screenop.ScreenProcessorProvider;
import io.github.fishstiz.packed_packs.gui.components.PackList;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksScreen;

public class ControlifyIntegration implements ControlifyEntrypoint {
    @Override
    public void onControllersDiscovered(ControlifyApi controlify) {
    }

    @Override
    public void onControlifyInit(InitContext context) {
    }

    @Override
    public void onControlifyPreInit(PreInitContext context) {
        ScreenProcessorProvider.registerProvider(PackedPacksScreen.class, PackedPacksScreenProcessor::new);
        ComponentProcessorProvider.REGISTRY.register(PackList.Entry.class, PackEntryProcessor::new);
    }
}
