package io.github.fishstiz.packed_packs.gui2.states;

public record ProfileEntry(
        String id,
        String name,
        boolean locked,
        PackOverrides overrides
) {
    public ProfileEntry withName(String name) {
        return new ProfileEntry(id, name, locked, overrides);
    }

    public ProfileEntry withLocked(boolean locked) {
        return new ProfileEntry(id, name, locked, overrides);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof ProfileEntry other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
