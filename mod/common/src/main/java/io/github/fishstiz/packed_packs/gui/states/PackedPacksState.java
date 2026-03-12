package io.github.fishstiz.packed_packs.gui.states;

import io.github.fishstiz.packed_packs.gui.model.PackListKey;
import io.github.fishstiz.packed_packs.gui.model.PackListType;
import org.jspecify.annotations.Nullable;

public record PackedPacksState(
        PackListState available,
        PackListState enabled,
        ProfilesState profiles,
        PackListKey lastTarget,
        @Nullable ActiveAction action
) {
    private static final PackedPacksState EMPTY = new PackedPacksState(PackListState.empty(), PackListState.empty(), ProfilesState.empty());

    public static PackedPacksState empty() {
        return EMPTY;
    }

    public PackedPacksState(PackListState available, PackListState enabled, ProfilesState profiles) {
        this(available, enabled, profiles, PackListKey.available(), null);
    }

    public PackedPacksState with(PackListState available, PackListState enabled, ProfilesState profiles) {
        return new PackedPacksState(available, enabled, profiles, this.lastTarget, null);
    }

    public PackedPacksState withAvailable(PackListState newAvailable, PackListKey target) {
        return new PackedPacksState(newAvailable, this.enabled, this.profiles, target, null);
    }

    public PackedPacksState withEnabled(PackListState newEnabled, PackListKey target) {
        return new PackedPacksState(this.available, newEnabled, this.profiles, target, null);
    }

    public PackedPacksState withPackLists(PackListState available, PackListState enabled, PackListKey target) {
        return new PackedPacksState(available, enabled, this.profiles, target, null);
    }

    public PackedPacksState withPackLists(PackListState available, PackListState enabled) {
        return new PackedPacksState(available, enabled, this.profiles, this.lastTarget, null);
    }

    public PackedPacksState withProfiles(ProfilesState newProfiles) {
        return new PackedPacksState(this.available, this.enabled, newProfiles, this.lastTarget, null);
    }

    public PackedPacksState withAction(@Nullable ActiveAction action) {
        return new PackedPacksState(this.available, this.enabled, this.profiles, action == null ? this.lastTarget : action.target(), action);
    }

    public ActiveAction.@Nullable RenamingPack renamingPack() {
        return this.action instanceof ActiveAction.RenamingPack renamingPack ? renamingPack : null;
    }

    public ActiveAction.@Nullable EditingAliases editingAliases() {
        return this.action instanceof ActiveAction.EditingAliases editingAliases ? editingAliases : null;
    }

    public ActiveAction.@Nullable Dragging dragging() {
        return this.action instanceof ActiveAction.Dragging dragging ? dragging : null;
    }

    private PackListState rootTargetList(PackListType type) {
        return switch (type) {
            case AVAILABLE -> this.available;
            case ENABLED -> this.enabled;
        };
    }

    private @Nullable PackListState targetList(PackListState state, PackListKey listKey, int depth) {
        if (listKey.depth() == depth) {
            return state;
        }
        if (state.folder() == null || listKey.depth() < 0) {
            return null;
        }
        return this.targetList(state.folder().contents(), listKey, depth + 1);
    }

    public @Nullable PackListState targetList(PackListKey key) {
        return this.targetList(this.rootTargetList(key.type()), key, 0);
    }

    private PackListKey deepestTarget(PackListState state, PackListKey key) {
        if (state.folder() == null) return key;
        return this.deepestTarget(state.folder().contents(), key.nest());
    }

    public PackListKey deepestTarget(PackListType type) {
        return this.deepestTarget(this.rootTargetList(type), PackListKey.root(type));
    }
}
