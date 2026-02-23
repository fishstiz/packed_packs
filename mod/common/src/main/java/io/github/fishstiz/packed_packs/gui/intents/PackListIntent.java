package io.github.fishstiz.packed_packs.gui.intents;

import io.github.fishstiz.packed_packs.config.PackOverride;
import io.github.fishstiz.packed_packs.gui.Intent;
import io.github.fishstiz.packed_packs.gui.components.pack.Query;
import io.github.fishstiz.packed_packs.gui.model.PackListKey;
import io.github.fishstiz.packed_packs.impl.context.PackEntryContext;
import io.github.fishstiz.packed_packs.pack.PackGroup;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.SequencedCollection;

public sealed interface PackListIntent extends Intent {
    PackListKey target();

    sealed interface Entry extends PackListIntent {
        PackEntryContext ctx();
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

    record Select(PackListKey target, PackEntryContext ctx) implements ListScoped, Entry {
    }

    record SelectExclusive(PackListKey target, PackEntryContext ctx) implements ListScoped, Entry {
    }

    record SelectToggle(PackListKey target, PackEntryContext ctx) implements ListScoped, Entry {
    }

    record SelectRange(PackListKey target, PackEntryContext ctx) implements ListScoped, Entry {
    }

    record SelectAll(PackListKey target, @Nullable PackEntryContext ctx) implements ListScoped, Entry {
    }

    record Enable(
            PackListKey target,
            PackEntryContext ctx,
            SequencedCollection<Pack> payload,
            int index
    ) implements ScreenScoped, Entry {
        public Enable(PackListKey target, PackEntryContext ctx, SequencedCollection<Pack> payload) {
            this(target, ctx, payload, 0);
        }

        @Override
        public boolean pushState() {
            return true;
        }
    }

    record Disable(
            PackListKey target,
            PackEntryContext ctx,
            SequencedCollection<Pack> payload
    ) implements ScreenScoped, Entry {
        @Override
        public boolean pushState() {
            return true;
        }
    }

    record Move(
            PackListKey target,
            PackEntryContext ctx,
            SequencedCollection<Pack> payload,
            int index
    ) implements ListScoped, Entry {
    }

    record MoveUp(
            PackListKey target,
            PackEntryContext ctx,
            SequencedCollection<Pack> payload
    ) implements ListScoped, Entry {
    }

    record MoveDown(
            PackListKey target,
            PackEntryContext ctx,
            SequencedCollection<Pack> payload
    ) implements ListScoped, Entry {
    }

    record Drag(
            PackListKey target,
            PackEntryContext ctx,
            SequencedCollection<Pack> payload
    ) implements ScreenScoped, Entry {
    }

    record Drop(
            PackListKey target,
            PackEntryContext ctx,
            SequencedCollection<Pack> payload,
            @Nullable PackListKey destination,
            int index
    ) implements ScreenScoped, Entry {
        @Override
        public boolean pushState() {
            return this.destination != null;
        }
    }

    record OpenRename(PackListKey target, PackEntryContext ctx) implements ScreenScoped, Entry {
        @Override
        public boolean pushState() {
            return true;
        }
    }

    record CloseRename(PackListKey target, PackEntryContext ctx) implements ScreenScoped, Entry {
        @Override
        public boolean pushState() {
            return true;
        }
    }

    record OpenFolder(
            PackListKey target,
            PackEntryContext ctx,
            FolderPack folderPack,
            List<Pack> contents
    ) implements ListScoped, Entry {
    }

    record CloseFolder(PackListKey target) implements ListScoped {
    }

    record Rename(
            PackListKey target,
            PackEntryContext ctx,
            String newName,
            Status status
    ) implements ScreenScoped, Operation {
        public Rename(PackListKey target, PackEntryContext ctx, String newName) {
            this(target, ctx, newName, Status.LOADING);
        }

        public Rename withFail() {
            return new Rename(this.target, this.ctx, this.newName, Status.FAILURE);
        }

        public Rename withSuccess() {
            return new Rename(this.target, this.ctx, this.newName, Status.SUCCESS);
        }
    }

    record Delete(PackListKey target, PackEntryContext ctx, Status status) implements ListScoped, Operation, Entry {
        public Delete(PackListKey target, PackEntryContext ctx) {
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
            PackEntryContext ctx,
            SequencedCollection<Pack> payload,
            boolean hidden
    ) implements ListScoped, Entry {
    }

    record Require(
            PackListKey target,
            PackEntryContext ctx,
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
            PackEntryContext ctx,
            SequencedCollection<Pack> payload,
            PackOverride.@Nullable Position position
    ) implements ListScoped, Entry {
    }

    record RemoveOverrides(
            PackListKey target,
            PackEntryContext ctx,
            SequencedCollection<Pack> payload
    ) implements ListScoped, Entry {
    }

    record EditAliases(PackListKey target, PackEntryContext ctx, List<String> aliases) implements ScreenScoped, Entry {
    }

    record CloseAliases(PackListKey target, PackEntryContext ctx, List<String> aliases) implements ScreenScoped, Entry {
    }
}
