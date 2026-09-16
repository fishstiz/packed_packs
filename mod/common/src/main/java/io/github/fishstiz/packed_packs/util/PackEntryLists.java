package io.github.fishstiz.packed_packs.util;

import io.github.fishstiz.packed_packs.pack.PackEntry;

import java.util.List;

public record PackEntryLists(List<PackEntry> disabled, List<PackEntry> enabled) {
}
