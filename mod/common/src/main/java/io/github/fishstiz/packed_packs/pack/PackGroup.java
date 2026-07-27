package io.github.fishstiz.packed_packs.pack;

import net.minecraft.server.packs.repository.Pack;

import java.util.List;

public record PackGroup(List<Pack> selected, List<Pack> unselected) {
}