package io.github.fishstiz.packed_packs.gui.intents;

import io.github.fishstiz.packed_packs.config.PackOverride;
import io.github.fishstiz.packed_packs.gui.Intent;
import io.github.fishstiz.packed_packs.gui.model.Query;
import io.github.fishstiz.packed_packs.gui.model.PackListKey;
import io.github.fishstiz.packed_packs.api.context.PackContext;
import io.github.fishstiz.packed_packs.pack.PackGroup;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.SequencedCollection;
// everything should be id-based/or not for optimization so we dont have to keep mapping
// should remove some payload that can be gotten from state
public sealed interface PackListIntent extends Intent {
    PackListKey target();

    sealed interface Entry extends PackListIntent {
        PackContext ctx();
    }

    sealed interface ListScoped extends PackListIntent {
        @Override
        default boolean pushState() {
            return true;
        }
    }

    sealed interface ScreenScoped extends PackListIntent {
        @Override
        default boolean pushState() {
            return false;
        }
    }

    record Reset(PackListKey target, PackGroup packs) implements ScreenScoped {
        @Override
        public boolean pushState() {
            return true;
        }
    }

    record Search(PackListKey target, String query) implements ListScoped {
    }

    record Sort(PackListKey target, Query.SortOption sort) implements ListScoped {
    }

    record HideIncompatible(PackListKey target, boolean hide) implements ListScoped {
    }

    record Select(PackListKey target, PackContext ctx) implements ListScoped, Entry {
    }

    record SelectExclusive(PackListKey target, PackContext ctx) implements ListScoped, Entry {
    }

    record SelectToggle(PackListKey target, PackContext ctx) implements ListScoped, Entry {
    }

    record SelectRange(PackListKey target, PackContext ctx) implements ListScoped, Entry {
    }

    record SelectAll(PackListKey target, @Nullable PackContext ctx) implements ListScoped, Entry {
    }

    record Enable(
            PackListKey target,
            PackContext ctx,
            SequencedCollection<Pack> payload,
            int index
    ) implements ScreenScoped, Entry {
        public Enable(PackListKey target, PackContext ctx, SequencedCollection<Pack> payload) {
            this(target, ctx, payload, 0);
        }

        @Override
        public boolean pushState() {
            return true;
        }
    }

    record Disable(
            PackListKey target,
            PackContext ctx,
            SequencedCollection<Pack> payload
    ) implements ScreenScoped, Entry {
        @Override
        public boolean pushState() {
            return true;
        }
    }

    record Move(
            PackListKey target,
            PackContext ctx,
            SequencedCollection<Pack> payload,
            int index
    ) implements ListScoped, Entry {
    }

    record MoveUp(
            PackListKey target,
            PackContext ctx,
            SequencedCollection<Pack> payload
    ) implements ListScoped, Entry {
    }

    record MoveDown(
            PackListKey target,
            PackContext ctx,
            SequencedCollection<Pack> payload
    ) implements ListScoped, Entry {
    }

    record Drag(
            PackListKey target,
            PackContext ctx,
            SequencedCollection<Pack> payload
    ) implements ScreenScoped, Entry {
    }

    record Drop(
            PackListKey target,
            PackContext ctx,
            SequencedCollection<Pack> payload, // payload can be removed
            @Nullable PackListKey destination,
            int index
    ) implements ScreenScoped, Entry {
        @Override
        public boolean pushState() {
            return this.destination != null;
        }
    }

    record OpenRename(PackListKey target, PackContext ctx) implements ScreenScoped, Entry {
        @Override
        public boolean pushState() {
            return true;
        }
    }

    record CloseRename(PackListKey target, PackContext ctx) implements ScreenScoped, Entry {
        @Override
        public boolean pushState() {
            return true;
        }
    }

    record OpenFolder(
            PackListKey target,
            PackContext ctx,
            FolderPack folderPack,
            List<Pack> contents // todo remove this, should
    ) implements ListScoped, Entry {
    }

    record CloseFolder(PackListKey target) implements ListScoped {
    }

    record Rename(
            PackListKey target,
            PackContext ctx,
            String newName,
            Status status
    ) implements ScreenScoped, Operation {
        public Rename(PackListKey target, PackContext ctx, String newName) {
            this(target, ctx, newName, Status.LOADING);
        }

        public Rename withFail() {
            return new Rename(this.target, this.ctx, this.newName, Status.FAILURE);
        }

        public Rename withSuccess() {
            return new Rename(this.target, this.ctx, this.newName, Status.SUCCESS);
        }
    }

    record Delete(PackListKey target, PackContext ctx, Status status) implements ListScoped, Operation, Entry {
        public Delete(PackListKey target, PackContext ctx) {
            this(target, ctx, Status.LOADING);
        }

        public Delete withFail() {
            return new Delete(this.target, this.ctx, Status.FAILURE);
        }

        public Delete withSuccess() {
            return new Delete(this.target, this.ctx, Status.SUCCESS);
        }
    }

    record Hide(
            PackListKey target,
            PackContext ctx,
            SequencedCollection<Pack> payload,
            boolean hidden
    ) implements ListScoped, Entry {
    }

    record Require(
            PackListKey target,
            PackContext ctx,
            SequencedCollection<Pack> payload,
            @Nullable Boolean required
    ) implements ScreenScoped, Entry {
        @Override
        public boolean resetHistory() {
            return Boolean.TRUE.equals(this.required) && this.target.type().available();
        }
    }

    record FixPosition(
            PackListKey target,
            PackContext ctx,
            SequencedCollection<Pack> payload,
            PackOverride.@Nullable Position position
    ) implements ListScoped, Entry {
    }

    record RemoveOverrides(
            PackListKey target,
            PackContext ctx,
            SequencedCollection<Pack> payload
    ) implements ListScoped, Entry {
    }

    record EditAliases(PackListKey target, PackContext ctx, List<String> aliases) implements ScreenScoped, Entry {
    }

    record CloseAliases(PackListKey target, PackContext ctx, List<String> aliases) implements ScreenScoped, Entry {
    }
}
