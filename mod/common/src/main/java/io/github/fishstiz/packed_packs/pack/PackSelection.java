package io.github.fishstiz.packed_packs.pack;

import java.util.List;

public record PackSelection(List<PackNode> disabled, List<PackNode> enabled) {
}
