package io.github.fishstiz.packed_packs.gui2.mutations;

import io.github.fishstiz.packed_packs.gui2.models.PackEntryLists;

public sealed interface Mutation permits
        Mutation.PackRenameFailed,
        Mutation.PackRenameLoading,
        Mutation.Reset,
        PackListMutation,
        ProfileMutation {

    default boolean pushState() {
        return true;
    }

    default boolean resetHistory() {
        return false;
    }

    record Reset(PackEntryLists packs) implements Mutation {
    }

    record PackRenameLoading() implements Mutation {
        @Override
        public boolean pushState() {
            return false;
        }
    }

    record PackRenameFailed() implements Mutation {
    }
}
