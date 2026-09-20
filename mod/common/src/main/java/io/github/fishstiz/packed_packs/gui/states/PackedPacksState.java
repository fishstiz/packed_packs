package io.github.fishstiz.packed_packs.gui.states;

import org.jspecify.annotations.Nullable;

public record PackedPacksState(
        PackListState available,
        PackListState enabled,
        ProfilesState profiles,
        PackListKey lastTarget,
        @Nullable ActiveAction action,
        boolean devMode
) {
    private static final PackedPacksState EMPTY = new PackedPacksState(
            PackListState.empty(),
            PackListState.empty(),
            ProfilesState.empty(),
            PackListKey.available(),
            null,
            false
    );

    public static PackedPacksState empty() {
        return EMPTY;
    }

    public PackedPacksState with(PackListState available, PackListState enabled, ProfilesState profiles) {
        return new PackedPacksState(available, enabled, profiles, lastTarget, null, devMode);
    }

    public PackedPacksState withAvailable(PackListState newAvailable, PackListKey target) {
        return new PackedPacksState(newAvailable, enabled, profiles, target, null, devMode);
    }

    public PackedPacksState withEnabled(PackListState newEnabled, PackListKey target) {
        return new PackedPacksState(available, newEnabled, profiles, target, null, devMode);
    }

    public PackedPacksState withPackLists(PackListState available, PackListState enabled, PackListKey target) {
        return new PackedPacksState(available, enabled, profiles, target, null, devMode);
    }

    public PackedPacksState withPackLists(PackListState available, PackListState enabled) {
        return new PackedPacksState(available, enabled, profiles, lastTarget, null, devMode);
    }

    public PackedPacksState withProfiles(ProfilesState newProfiles) {
        return new PackedPacksState(available, enabled, newProfiles, lastTarget, null, devMode);
    }

    public PackedPacksState withAction(@Nullable ActiveAction action) {
        return new PackedPacksState(available, enabled, profiles, action == null ? lastTarget : action.src(), action, devMode);
    }

    public PackedPacksState withDevMode(boolean devMode) {
        return new PackedPacksState(available, enabled, profiles, lastTarget, action, devMode);
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

    public PackListState getHeadList(PackListType type) {
        return switch (type) {
            case AVAILABLE -> this.available;
            case ENABLED -> this.enabled;
        };
    }

    private @Nullable PackListState getList(PackListState state, PackListKey listKey, int depth) {
        if (listKey.depth() == depth) {
            return state;
        }
        if (state.folder() == null || listKey.depth() < 0) {
            return null;
        }
        return this.getList(state.folder(), listKey, depth + 1);
    }

    public @Nullable PackListState getList(PackListKey key) {
        return this.getList(this.getHeadList(key.type()), key, 0);
    }

    private PackListKey getTailKey(PackListState state, PackListKey key) {
        if (state.folder() == null) return key;
        return this.getTailKey(state.folder(), key.nest());
    }

    public PackListKey getTailKey(PackListType type) {
        return this.getTailKey(this.getHeadList(type), PackListKey.head(type));
    }

    public PackListState getTailList(PackListType type) {
        return getList(getTailKey(type));
    }
}
