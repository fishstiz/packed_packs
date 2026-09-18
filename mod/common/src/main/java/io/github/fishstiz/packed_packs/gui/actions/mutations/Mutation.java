package io.github.fishstiz.packed_packs.gui.actions.mutations;

import io.github.fishstiz.packed_packs.pack.PackSelection;

public sealed interface Mutation permits
        Mutation.PackRenaming,
        Mutation.Reset,
        PackListMutation,
        ProfileMutation {

    default boolean pushState() {
        return true;
    }

    default boolean resetHistory() {
        return false;
    }

    record Reset(PackSelection packs) implements Mutation {
        @Override
        public boolean pushState() {
            return false;
        }

        @Override
        public boolean resetHistory() {
            return true;
        }
    }

    record PackRenaming(boolean loading) implements Mutation {
        @Override
        public boolean pushState() {
            return false;
        }
    }
}
