package io.github.fishstiz.packed_packs.models;

import java.util.List;

public record PackEntryLists(List<PackEntry> disabled, List<PackEntry> enabled) {
}
