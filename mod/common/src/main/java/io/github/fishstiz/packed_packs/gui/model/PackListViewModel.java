package io.github.fishstiz.packed_packs.gui.model;

import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.packed_packs.config.PackOptions;
import io.github.fishstiz.packed_packs.config.PackOverride;
import io.github.fishstiz.packed_packs.gui.intents.PackListIntent;
import io.github.fishstiz.packed_packs.gui.components.pack.PackListDevMenu;
import io.github.fishstiz.packed_packs.gui.components.pack.Query;
import io.github.fishstiz.packed_packs.gui.states.PackListState;
import io.github.fishstiz.packed_packs.impl.context.PackEntryContext;
import io.github.fishstiz.packed_packs.pack.PackAssetManager;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;

import java.util.function.Predicate;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIntBiConsumer;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.github.fishstiz.packed_packs.gui.model.PackListUtils.*;

public class PackListViewModel {
    protected final PackListKey target;
    protected final PackListContext ctx;
    protected final Supplier<PackListState> state;
    protected final Consumer<PackListIntent> dispatch;
    private final Map<Property, List<Runnable>> listeners = new EnumMap<>(Property.class);
    private final List<Runnable> internalListeners = new ObjectArrayList<>(1);
    private PackListState cachedState;

    public enum Property {
        ALL,
        PACKS,
        SELECTION,
        QUERY,
        FOLDER,
    }

    private PackListViewModel(PackListKey target, PackListContext ctx, Supplier<PackListState> state, Consumer<PackListIntent> dispatch) {
        this.target = target;
        this.ctx = ctx;
        this.dispatch = dispatch;
        this.state = state;
        this.cachedState = state.get();
    }

    public static PackListViewModel available(PackListContext ctx, Supplier<PackListState> state, Consumer<PackListIntent> dispatch) {
        return new PackListViewModel(PackListKey.available(), ctx, state, dispatch);
    }

    public static PackListViewModel enabled(PackListContext ctx, Supplier<PackListState> state, Consumer<PackListIntent> dispatch) {
        return new PackListViewModel(PackListKey.enabled(), ctx, state, dispatch);
    }

    private void notifyListeners(Property property) {
        this.listeners.getOrDefault(property, Collections.emptyList()).forEach(Runnable::run);
    }

    void onStateChanged() {
        PackListState prev = this.cachedState;
        PackListState state = this.state.get();
        if (prev == state) return;

        this.cachedState = state;

        this.notifyListeners(Property.ALL);
        if (prev.visiblePacks() != state.visiblePacks()) {
            this.notifyListeners(Property.PACKS);
        }
        if (prev.selectedPacks() != state.selectedPacks()) {
            this.notifyListeners(Property.SELECTION);
        }
        if (prev.query() != state.query()) {
            this.notifyListeners(Property.QUERY);
        }
        if (prev.folder() != state.folder()) {
            this.internalListeners.forEach(Runnable::run);
            this.notifyListeners(Property.FOLDER);
            // save on close or on folder change
            if (!this.locked() && prev.folder() != null && (state.folder() == null || !state.folder().pack().equals(prev.folder().pack()))) {
                this.ctx.folderSaver().accept(prev.folder().pack(), prev.folder().contents().packs());
            }
        }
        if (state.folder() == null) {
            this.internalListeners.clear();
        }
    }

    public PackListKey key() {
        return this.target;
    }

    public Runnable subscribe(Property property, Runnable listener) {
        this.listeners.computeIfAbsent(property, k -> new ObjectArrayList<>()).add(listener);
        return () -> this.unsubscribe(property, listener);
    }

    public void unsubscribe(Property property, Runnable listener) {
        List<Runnable> listeners = this.listeners.get(property);
        if (listeners != null) listeners.remove(listener);
    }

    public void forEachEntry(ObjectIntBiConsumer<Entry> consumer) {
        List<Pack> visiblePacks = this.state.get().visiblePacks();
        for (int i = 0; i < visiblePacks.size(); i++) {
            consumer.accept(new Entry(visiblePacks.get(i)), i);
        }
    }

    public boolean hasSelection() {
        return !this.state.get().selectedPacks().isEmpty();
    }

    public Query query() {
        return this.state.get().query();
    }

    public boolean isFolderOpened() {
        return this.state.get().folder() != null;
    }

    public boolean locked() {
        return this.ctx.options().isLocked();
    }

    Optional<PackListIntent> createTransferIntent(PackEntryContext pack, SequencedCollection<Pack> payload) {
        if (this.target.depth() > 0) return Optional.empty();
        return Optional.of(switch (this.target.type()) {
            case AVAILABLE -> new PackListIntent.Enable(this.target, pack, payload, 0);
            case ENABLED -> new PackListIntent.Disable(this.target, pack, payload);
        });
    }

    public boolean supportsReordering() {
        return this.target.depth() > 0 || this.target.type().enabled();
    }

    public boolean canEnable(Pack pack) {
        return !this.locked() && this.target.depth() == 0 && this.target.type().available();
    }

    public boolean canDisable(Pack pack) {
        return !this.locked() && this.target.depth() == 0 && this.target.type().enabled() && !this.ctx.options().isRequired(pack);
    }

    public boolean canTransfer(Pack pack) {
        return switch (this.target.type()) {
            case AVAILABLE -> this.canEnable(pack);
            case ENABLED -> this.canDisable(pack);
        };
    }

    public boolean canDrag(Pack pack) {
        return !this.locked() && PackListUtils.canDrag(this.target, pack, this.ctx.options());
    }

    public boolean canMoveUp(Pack pack) {
        if (this.locked() || this.target.depth() == 0 && this.target.type().available()) {
            return false;
        }
        PackListState currentState = this.state.get();
        PackOptions options = this.ctx.options();
        if (currentState.query().hasQuery() || options.isFixed(pack)) {
            return false;
        }
        if (currentState.selectedPacks().contains(pack)) {
            List<Pack> selection = sortByOrderOf(currentState.visiblePacks(), currentState.selectedPacks());
            if (selection.size() > 1) {
                int index = currentState.packs().indexOf(selection.getFirst());
                int moveIndex = index > -1 ? getMoveUpIndex(currentState.packs(), pack, options) : -1;
                return index > 0 && moveIndex > -1 && !options.isFixed(currentState.packs().get(moveIndex));
            }
        }
        int index = currentState.packs().indexOf(pack);
        int moveIndex = getMoveUpIndex(currentState.packs(), pack, options);
        return index > 0 && moveIndex > -1 && !options.isFixed(currentState.packs().get(moveIndex));
    }

    public boolean canMoveDown(Pack pack) {
        if (this.locked() || this.target.depth() == 0 && this.target.type().available()) {
            return false;
        }
        PackListState currentState = this.state.get();
        PackOptions options = this.ctx.options();
        if (currentState.query().hasQuery() || options.isFixed(pack)) {
            return false;
        }
        int size = currentState.packs().size();
        if (currentState.selectedPacks().contains(pack)) {
            List<Pack> selection = sortByOrderOf(currentState.visiblePacks(), currentState.selectedPacks());
            if (selection.size() > 1) {
                int index = currentState.packs().indexOf(selection.getLast());
                int moveIndex = index > -1 ? getMoveDownIndex(currentState.packs(), pack, options) : -1;
                return index > -1 && index < size - 1 && moveIndex > -1 && !options.isFixed(currentState.packs().get(moveIndex));
            }
        }
        int index = currentState.packs().indexOf(pack);
        int moveIndex = getMoveDownIndex(currentState.packs(), pack, options);
        return index > -1 && index < size - 1 && moveIndex > -1 && !options.isFixed(currentState.packs().get(moveIndex));
    }

    public boolean canDrop(PackListKey source, Pack pack, SequencedCollection<Pack> payload, int index) {
        return !this.locked() && PackListUtils.canDrop(source, pack, payload, this.target, this.state.get(), index, this.ctx.options());
    }

    public void cancelDrop(PackListKey source, PackEntryContext pack, SequencedCollection<Pack> payload) {
        this.dispatch.accept(new PackListIntent.Drop(source, pack, payload, null, 0));
    }

    public void applyDrop(PackListKey source, PackEntryContext pack, SequencedCollection<Pack> payload, int index) {
        if (this.canDrop(source, pack.pack(), payload, index)) {
            this.dispatch.accept(new PackListIntent.Drop(source, pack, payload, this.target, index));
        } else {
            this.cancelDrop(source, pack, payload);
        }
    }

    public void search(String query) {
        this.dispatch.accept(new PackListIntent.Search(this.target, query));
    }

    public void hideIncompatible(boolean hide) {
        this.dispatch.accept(new PackListIntent.HideIncompatible(this.target, hide));
    }

    public void sort(Query.SortOption sort) {
        this.dispatch.accept(new PackListIntent.Sort(this.target, sort));
    }

    public void transferAll() {
        if (!this.locked()) {
            List<Pack> packs = this.state.get().visiblePacks();
            List<Pack> payload = new ObjectArrayList<>(packs.size());
            for (Pack pack : packs) {
                if (this.canTransfer(pack)) {
                    payload.add(pack);
                }
            }
            if (!payload.isEmpty()) {
                List<Pack> orderedPayload = sortByOrderOf(packs, payload).reversed();
                this.createTransferIntent(new Entry(orderedPayload.getFirst()), orderedPayload).ifPresent(this.dispatch);
            }
        }
    }

    public void selectAll() {
        if (!this.locked()) {
            SequencedCollection<Pack> selectedPacks = this.state.get().selectedPacks();
            this.dispatch.accept(new PackListIntent.SelectAll(this.target, selectedPacks.isEmpty() ? null : new Entry(selectedPacks.getLast())));
        }
    }

    public Module createFolderSlice() {
        Module module = new Module(this.target.nest(), this.ctx, () -> this.state.get().folder(), this.dispatch);
        this.internalListeners.add(module::onStateChanged);
        return module;
    }

    public class Entry implements PackEntryContext {
        private final Pack pack;

        protected Entry(Pack pack) {
            this.pack = pack;
        }

        @Override
        public Pack pack() {
            return this.pack;
        }

        @Override
        public Sprite sprite() {
            return PackListViewModel.this.ctx.iconFactory().apply(this.pack);
        }

        @Override
        public boolean fileModifiable() {
            return !PackListViewModel.this.locked() && PackListViewModel.this.ctx.fileModifiable().test(this.pack);
        }

        public boolean selected() {
            return PackListViewModel.this.state.get().selectedPacks().contains(this.pack);
        }

        public boolean selectedLast() {
            SequencedCollection<Pack> selection = PackListViewModel.this.state.get().selectedPacks();
            return !selection.isEmpty() && selection.getLast().equals(this.pack);
        }

        public boolean selectedExclusive() {
            return this.selected() && PackListViewModel.this.state.get().selectedPacks().size() == 1;
        }

        public boolean incompatibleWarningsHidden() {
            return PackListViewModel.this.ctx.configs().user().isIncompatibleWarningsHidden();
        }

        public boolean canEnable() {
            return PackListViewModel.this.canEnable(this.pack);
        }

        public boolean canDisable() {
            return PackListViewModel.this.canDisable(this.pack);
        }

        public boolean canTransfer() {
            return PackListViewModel.this.canTransfer(this.pack);
        }

        public boolean unfixed() {
            return !PackListViewModel.this.ctx.options().isLocked() &&
                   !PackListViewModel.this.state.get().query().hasQuery() &&
                   !PackListViewModel.this.ctx.options().isFixed(this.pack());
        }

        public boolean canMoveUp() {
            return this.unfixed() && PackListViewModel.this.canMoveUp(this.pack);
        }

        public boolean canMoveDown() {
            return this.unfixed() && PackListViewModel.this.canMoveDown(this.pack);
        }

        public Optional<FolderPack> folder() {
            if (this.pack instanceof FolderPack folderPack) {
                return Optional.of(folderPack);
            }
            return Optional.empty();
        }

        public void select() {
            if (PackListViewModel.this.locked()) return;
            PackListViewModel.this.dispatch.accept(new PackListIntent.Select(PackListViewModel.this.target, this));
        }

        public void selectToggle() {
            if (PackListViewModel.this.locked()) return;
            PackListViewModel.this.dispatch.accept(new PackListIntent.SelectToggle(PackListViewModel.this.target, this));
        }

        public void selectRange() {
            if (PackListViewModel.this.locked()) return;
            PackListViewModel.this.dispatch.accept(new PackListIntent.SelectRange(PackListViewModel.this.target, this));
        }

        public void selectExclusive() {
            if (PackListViewModel.this.locked()) return;
            PackListViewModel.this.dispatch.accept(new PackListIntent.SelectExclusive(PackListViewModel.this.target, this));
        }

        private List<Pack> createPayload(Predicate<Pack> filter) {
            if (!filter.test(this.pack)) {
                return Collections.emptyList();
            }

            if (!this.selected()) {
                return List.of(this.pack);
            }

            SequencedCollection<Pack> selection = PackListViewModel.this.state.get().selectedPacks();
            List<Pack> payload = new ObjectArrayList<>(selection.size());
            for (Pack pack : selection) {
                if (filter.test(pack)) {
                    payload.add(pack);
                }
            }

            return List.copyOf(payload);
        }

        private List<Pack> createPayload() {
            return this.selected() ? List.copyOf(PackListViewModel.this.state.get().selectedPacks()) : List.of(this.pack);
        }

        public void transfer() {
            if (PackListViewModel.this.locked()) return;

            List<Pack> payload = this.createPayload(PackListViewModel.this::canTransfer);
            if (!payload.isEmpty()) {
                List<Pack> orderedPayload = sortByOrderOf(PackListViewModel.this.state.get().visiblePacks(), payload).reversed();
                PackListViewModel.this.createTransferIntent(this, orderedPayload).ifPresent(PackListViewModel.this.dispatch);
            }
        }

        public void enable() {
            if (PackListViewModel.this.locked()) return;

            List<Pack> payload = this.createPayload(PackListViewModel.this::canEnable);
            if (!payload.isEmpty()) {
                List<Pack> orderedPayload = sortByOrderOf(PackListViewModel.this.state.get().visiblePacks(), payload).reversed();
                PackListViewModel.this.dispatch.accept(new PackListIntent.Enable(PackListViewModel.this.target, this, orderedPayload));
            }
        }

        public void disable() {
            if (PackListViewModel.this.locked()) return;

            List<Pack> payload = this.createPayload(PackListViewModel.this::canDisable);
            if (!payload.isEmpty()) {
                List<Pack> orderedPayload = sortByOrderOf(PackListViewModel.this.state.get().visiblePacks(), payload).reversed();
                PackListViewModel.this.dispatch.accept(new PackListIntent.Disable(PackListViewModel.this.target, this, orderedPayload));
            }
        }

        public void moveUp() {
            if (PackListViewModel.this.locked()) return;

            List<Pack> payload = this.createPayload();
            if (!payload.isEmpty()) {
                PackListViewModel.this.dispatch.accept(new PackListIntent.MoveUp(PackListViewModel.this.target, this, payload));
            }
        }

        public void moveDown() {
            if (PackListViewModel.this.locked()) return;

            List<Pack> payload = this.createPayload();
            if (!payload.isEmpty()) {
                PackListViewModel.this.dispatch.accept(new PackListIntent.MoveDown(PackListViewModel.this.target, this, payload));
            }
        }

        public void drag() {
            if (!PackListViewModel.this.canDrag(this.pack) || PackListViewModel.this.locked()) return;

            List<Pack> payload = this.createPayload();
            if (!payload.isEmpty()) {
                List<Pack> orderedPayload = sortByOrderOf(PackListViewModel.this.state.get().visiblePacks(), payload).reversed();
                PackListViewModel.this.dispatch.accept(new PackListIntent.Drag(PackListViewModel.this.target, this, new ObjectLinkedOpenHashSet<>(orderedPayload)));
            }
        }

        public void openRename() {
            if (this.fileModifiable()) {
                PackListViewModel.this.dispatch.accept(new PackListIntent.OpenRename(PackListViewModel.this.target, this));
            }
        }

        public void delete() {
            if (this.fileModifiable()) {
                PackListViewModel.this.dispatch.accept(new PackListIntent.Delete(PackListViewModel.this.target, this));
            }
        }

        public void openFolder() {
            this.folder().ifPresent(folder -> PackListViewModel.this.dispatch.accept(new PackListIntent.OpenFolder(PackListViewModel.this.target, this, folder, folder.contents())));
        }

        public void overrideHidden(boolean hidden) {
            PackListViewModel.this.dispatch.accept(new PackListIntent.Hide(PackListViewModel.this.target, this, this.createPayload(), hidden));
        }

        public void overrideRequire(@Nullable Boolean required) {
            PackListViewModel.this.dispatch.accept(new PackListIntent.Require(PackListViewModel.this.target, this, this.createPayload(), required));
        }

        public void overridePosition(PackOverride.@Nullable Position position) {
            PackListViewModel.this.dispatch.accept(new PackListIntent.FixPosition(PackListViewModel.this.target, this, this.createPayload(), position));
        }

        public void removeOverrides() {
            PackListViewModel.this.dispatch.accept(new PackListIntent.RemoveOverrides(PackListViewModel.this.target, this, this.createPayload()));
        }

        public void editAliases() {
            PackListViewModel.this.dispatch.accept(new PackListIntent.EditAliases(PackListViewModel.this.target, this, PackListViewModel.this.ctx.configs().dev().getAliases(this.pack.getId())));
        }

        public PackListDevMenu devMenu(Minecraft minecraft) {
            return new PackListDevMenu(minecraft, PackListViewModel.this.ctx.configs().dev(), PackListViewModel.this.ctx.options(), this);
        }
    }

    public static class Module extends PackListViewModel {
        private final Supplier<PackListState.@Nullable Folder> folderState;
        private final List<Runnable> listeners = new ObjectArrayList<>();
        private PackListState.@Nullable Folder cachedFolderState;

        Module(PackListKey target, PackListContext ctx, Supplier<PackListState.@Nullable Folder> folderState, Consumer<PackListIntent> dispatch) {
            super(target, ctx, contentSupplier(folderState), dispatch);
            this.folderState = folderState;
            this.cachedFolderState = folderState.get();
        }

        private static Supplier<PackListState> contentSupplier(Supplier<PackListState.@Nullable Folder> folderState) {
            return () -> {
                PackListState.Folder state = folderState.get();
                return state == null ? PackListState.empty() : state.contents();
            };
        }

        @Override
        void onStateChanged() {
            super.onStateChanged();
            PackListState.Folder prev = this.cachedFolderState;
            PackListState.Folder state = this.folderState.get();

            if (prev != state) {
                this.cachedFolderState = state;
                if ((prev == null || state == null) || (!prev.pack().equals(state.pack()))) {
                    this.listeners.forEach(Runnable::run);
                }
            }
        }

        public Runnable subscribeToCurrentFolder(Runnable listener) {
            this.listeners.add(listener);
            return () -> this.unsubscribeToCurrentFolder(listener);
        }

        public void unsubscribeToCurrentFolder(Runnable listener) {
            this.listeners.remove(listener);
        }

        public boolean isOpened() {
            return this.folderState.get() != null;
        }

        public @Nullable FolderPack currentFolder() {
            PackListState.Folder folder = this.folderState.get();
            return folder == null ? null : folder.pack();
        }

        public Sprite sprite() {
            FolderPack folder = this.currentFolder();
            return folder != null ? this.ctx.iconFactory().apply(folder) : PackAssetManager.DEFAULT_ICON;
        }

        public boolean fileModifiable() {
            FolderPack folder = this.currentFolder();
            return folder != null && !this.locked() && this.ctx.fileModifiable().test(folder);
        }

        public void openRename() {
            if (this.fileModifiable()) {
                this.dispatch.accept(new PackListIntent.OpenRename(this.target, new Entry(this.currentFolder())));
            }
        }

        public void delete() {
            if (this.fileModifiable()) {
                this.dispatch.accept(new PackListIntent.Delete(this.target, new Entry(this.currentFolder())));
            }
        }

        public void close() {
            this.dispatch.accept(new PackListIntent.CloseFolder(this.target.unnest()));
        }
    }
}
