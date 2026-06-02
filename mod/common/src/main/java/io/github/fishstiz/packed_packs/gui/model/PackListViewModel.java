package io.github.fishstiz.packed_packs.gui.model;

import io.github.fishstiz.fidgetz.v0.utils.CollectionUtils;
import io.github.fishstiz.packed_packs.config.PackOptions;
import io.github.fishstiz.packed_packs.config.PackOverride;
import io.github.fishstiz.packed_packs.gui.components.PackListDevMenu;
import io.github.fishstiz.packed_packs.gui.intents.PackListIntent;
import io.github.fishstiz.packed_packs.gui.states.PackListState;
import io.github.fishstiz.packed_packs.api.context.PackContext;
import io.github.fishstiz.packed_packs.pack.PackIconManager;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIntBiConsumer;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
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

        notifyListeners(Property.ALL);
        if (prev.visiblePacks() != state.visiblePacks()) {
            notifyListeners(Property.PACKS);
        }
        if (prev.selectedPacks() != state.selectedPacks()) {
            notifyListeners(Property.SELECTION);
        }
        if (prev.query() != state.query()) {
            notifyListeners(Property.QUERY);
        }
        if (prev.folder() != state.folder()) {
            internalListeners.forEach(Runnable::run);
            notifyListeners(Property.FOLDER);
            // save on close or on folder change
            if (!locked() && prev.folder() != null && (state.folder() == null || !state.folder().pack().equals(prev.folder().pack()))) {
                ctx.folderSaver().accept(prev.folder().pack(), prev.folder().contents().packs());
            }
        }
        if (state.folder() == null) {
            internalListeners.clear();
        }
    }

    public PackListKey key() {
        return target;
    }

    public Runnable subscribe(Property property, Runnable listener) {
        listeners.computeIfAbsent(property, k -> new ObjectArrayList<>()).add(listener);
        return () -> unsubscribe(property, listener);
    }

    public void unsubscribe(Property property, Runnable listener) {
        List<Runnable> listeners = this.listeners.get(property);
        if (listeners != null) listeners.remove(listener);
    }

    public void forEachEntry(ObjectIntBiConsumer<Entry> consumer) {
        List<Pack> visiblePacks = state.get().visiblePacks();
        for (int i = 0; i < visiblePacks.size(); i++) {
            consumer.accept(new Entry(visiblePacks.get(i)), i);
        }
    }

    public boolean hasSelection() {
        return !state.get().selectedPacks().isEmpty();
    }

    public Query query() {
        return state.get().query();
    }

    public boolean isFolderOpened() {
        return state.get().folder() != null;
    }

    public boolean locked() {
        return ctx.options().isLocked();
    }

    Optional<PackListIntent> createTransferIntent(PackContext pack, SequencedCollection<Pack> payload) {
        if (this.target.depth() > 0) return Optional.empty();
        return Optional.of(switch (target.type()) {
            case AVAILABLE -> new PackListIntent.Enable(target, pack, payload, 0);
            case ENABLED -> new PackListIntent.Disable(target, pack, payload);
        });
    }

    public boolean supportsTransferring() {
        return target.depth() == 0;
    }

    public boolean supportsReordering() {
        return target.depth() > 0 || target.type().enabled();
    }

    public boolean canEnable(Pack pack) {
        return !locked() && target.depth() == 0 && target.type().available();
    }

    public boolean canDisable(Pack pack) {
        return !locked() && target.depth() == 0 && target.type().enabled() && !ctx.options().isRequired(pack);
    }

    public boolean canTransfer(Pack pack) {
        return switch (target.type()) {
            case AVAILABLE -> canEnable(pack);
            case ENABLED -> canDisable(pack);
        };
    }

    public boolean canDrag(Pack pack) {
        return !locked() && PackListUtils.canDrag(target, pack, ctx.options());
    }

    public boolean canMoveUp(Pack pack) {
        if (locked() || target.depth() == 0 && target.type().available()) {
            return false;
        }
        PackListState currentState = state.get();
        PackOptions options = ctx.options();
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
        if (locked() || target.depth() == 0 && target.type().available()) {
            return false;
        }
        PackListState currentState = state.get();
        PackOptions options = ctx.options();
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
        return !locked() && PackListUtils.canDrop(source, pack, payload, target, state.get(), index, ctx.options());
    }

    public void cancelDrop(PackListKey source, PackContext pack, SequencedCollection<Pack> payload) {
        dispatch.accept(new PackListIntent.Drop(source, pack, payload, null, 0));
    }

    public void applyDrop(PackListKey source, PackContext pack, SequencedCollection<Pack> payload, int index) {
        if (canDrop(source, pack.pack(), payload, index)) {
            dispatch.accept(new PackListIntent.Drop(source, pack, payload, target, index));
        } else {
            cancelDrop(source, pack, payload);
        }
    }

    public void search(String query) {
        dispatch.accept(new PackListIntent.Search(target, query));
    }

    public void hideIncompatible(boolean hide) {
        dispatch.accept(new PackListIntent.HideIncompatible(target, hide));
    }

    public void sort(Query.SortOption sort) {
        dispatch.accept(new PackListIntent.Sort(target, sort));
    }

    public void transferAll() {
        if (!locked()) {
            List<Pack> packs = state.get().visiblePacks();
            List<Pack> payload = new ObjectArrayList<>(packs.size());
            for (Pack pack : packs) {
                if (canTransfer(pack)) {
                    payload.add(pack);
                }
            }
            if (!payload.isEmpty()) {
                List<Pack> orderedPayload = sortByOrderOf(packs, payload).reversed();
                createTransferIntent(new Entry(orderedPayload.getFirst()), orderedPayload).ifPresent(dispatch);
            }
        }
    }

    public void selectAll() {
        if (!locked()) {
            SequencedCollection<Pack> selectedPacks = state.get().selectedPacks();
            dispatch.accept(new PackListIntent.SelectAll(target, selectedPacks.isEmpty() ? null : new Entry(selectedPacks.getLast())));
        }
    }

    public Module createFolderSlice() {
        Module module = new Module(target.nest(), ctx, () -> state.get().folder(), dispatch);
        internalListeners.add(module::onStateChanged);
        return module;
    }

    public class Entry implements PackContext {
        private final Pack pack;

        protected Entry(Pack pack) {
            this.pack = pack;
        }

        @Override
        public Pack pack() {
            return pack;
        }

        @Override
        public Identifier icon() {
            return ctx.iconFactory().apply(pack);
        }

        @Override
        public boolean fileModifiable() {
            return !locked() && ctx.fileModifiable().test(pack);
        }

        public boolean selected() {
            return state.get().selectedPacks().contains(pack);
        }

        public boolean selectedLast() {
            SequencedCollection<Pack> selection = state.get().selectedPacks();
            return !selection.isEmpty() && selection.getLast().equals(this.pack);
        }

        public boolean selectedExclusive() {
            return this.selected() && state.get().selectedPacks().size() == 1;
        }

        public boolean incompatibleWarningsHidden() {
            return ctx.configs().user().isIncompatibleWarningsHidden();
        }

        public boolean canEnable() {
            return PackListViewModel.this.canEnable(this.pack);
        }

        public boolean canDisable() {
            return PackListViewModel.this.canDisable(this.pack);
        }

        public boolean unfixed() {
            return !ctx.options().isLocked() && !state.get().query().hasQuery() && !ctx.options().isFixed(pack());
        }

        public boolean canMoveUp() {
            return unfixed() && PackListViewModel.this.canMoveUp(this.pack);
        }

        public boolean canMoveDown() {
            return unfixed() && PackListViewModel.this.canMoveDown(this.pack);
        }

        public Optional<FolderPack> folder() {
            if (pack instanceof FolderPack folderPack) {
                return Optional.of(folderPack);
            }
            return Optional.empty();
        }

        public void select() {
            if (locked()) return;
            dispatch.accept(new PackListIntent.Select(target, this));
        }

        public void selectToggle() {
            if (locked()) return;
            dispatch.accept(new PackListIntent.SelectToggle(target, this));
        }

        public void selectRange() {
            if (locked()) return;
            dispatch.accept(new PackListIntent.SelectRange(target, this));
        }

        public void selectExclusive() {
            if (locked()) return;
            dispatch.accept(new PackListIntent.SelectExclusive(target, this));
        }

        private List<Pack> createPayload(Predicate<Pack> filter) {
            SequencedCollection<Pack> selection = state.get().selectedPacks();
            if (!selection.contains(pack)) {
                return filter.test(pack) ? List.of(pack) : Collections.emptyList();
            }

            return CollectionUtils.addIf(new ObjectArrayList<>(selection.size()), selection, filter);
        }

        private List<Pack> createPayload() {
            return selected() ? List.copyOf(state.get().selectedPacks()) : List.of(pack);
        }

        public void transfer() {
            if (locked()) return;

            List<Pack> payload = createPayload(PackListViewModel.this::canTransfer);
            if (!payload.isEmpty()) {
                List<Pack> orderedPayload = sortByOrderOf(state.get().visiblePacks(), payload).reversed();
                createTransferIntent(this, orderedPayload).ifPresent(dispatch);
            }
        }

        public void enable() {
            if (locked()) return;

            List<Pack> payload = createPayload(PackListViewModel.this::canEnable);
            if (!payload.isEmpty()) {
                List<Pack> orderedPayload = sortByOrderOf(state.get().visiblePacks(), payload).reversed();
                dispatch.accept(new PackListIntent.Enable(target, this, orderedPayload));
            }
        }

        public void disable() {
            if (locked()) return;

            List<Pack> payload = createPayload(PackListViewModel.this::canDisable);
            if (!payload.isEmpty()) {
                List<Pack> orderedPayload = sortByOrderOf(state.get().visiblePacks(), payload).reversed();
                dispatch.accept(new PackListIntent.Disable(target, this, orderedPayload));
            }
        }

        public void moveUp() {
            if (locked()) return;

            List<Pack> payload = createPayload();
            if (!payload.isEmpty()) {
                dispatch.accept(new PackListIntent.MoveUp(target, this, payload));
            }
        }

        public void moveDown() {
            if (locked()) return;

            List<Pack> payload = createPayload();
            if (!payload.isEmpty()) {
                dispatch.accept(new PackListIntent.MoveDown(target, this, payload));
            }
        }

        public void drag() {
            if (!canDrag(pack) || locked()) return;

            List<Pack> payload = createPayload();
            if (!payload.isEmpty()) {
                List<Pack> orderedPayload = sortByOrderOf(state.get().visiblePacks(), payload);
                dispatch.accept(new PackListIntent.Drag(target, this, new ObjectLinkedOpenHashSet<>(orderedPayload)));
            }
        }

        public void openRename() {
            if (fileModifiable()) {
                dispatch.accept(new PackListIntent.OpenRename(target, this));
            }
        }

        public void delete() {
            if (fileModifiable()) {
                dispatch.accept(new PackListIntent.Delete(target, this));
            }
        }

        public void openFolder() {
            folder().ifPresent(folder -> dispatch.accept(new PackListIntent.OpenFolder(target, this, folder, folder.contents())));
        }

        public void overrideHidden(boolean hidden) {
            dispatch.accept(new PackListIntent.Hide(target, this, createPayload(), hidden));
        }

        public void overrideRequire(@Nullable Boolean required) {
            dispatch.accept(new PackListIntent.Require(target, this, createPayload(), required));
        }

        public void overridePosition(PackOverride.@Nullable Position position) {
            dispatch.accept(new PackListIntent.FixPosition(target, this, createPayload(), position));
        }

        public void removeOverrides() {
            dispatch.accept(new PackListIntent.RemoveOverrides(target, this, createPayload()));
        }

        public void editAliases() {
            dispatch.accept(new PackListIntent.EditAliases(target, this, ctx.configs().dev().getAliases(pack.getId())));
        }

        public PackListDevMenu createDevMenu() {
            return new PackListDevMenu(ctx.configs().dev(), ctx.options(), this);
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
            PackListState.Folder state = folderState.get();

            if (prev != state) {
                this.cachedFolderState = state;
                if ((prev == null || state == null) || (!prev.pack().equals(state.pack()))) {
                    listeners.forEach(Runnable::run);
                }
            }
        }

        public Runnable subscribeToCurrentFolder(Runnable listener) {
            listeners.add(listener);
            return () -> unsubscribeToCurrentFolder(listener);
        }

        public void unsubscribeToCurrentFolder(Runnable listener) {
            listeners.remove(listener);
        }

        public boolean isOpened() {
            return folderState.get() != null;
        }

        public @Nullable FolderPack currentFolder() {
            PackListState.Folder folder = folderState.get();
            return folder == null ? null : folder.pack();
        }

        public Identifier icon() {
            FolderPack folder = currentFolder();
            return folder != null ? ctx.iconFactory().apply(folder) : PackIconManager.DEFAULT_ICON;
        }

        public boolean fileModifiable() {
            FolderPack folder = currentFolder();
            return folder != null && !locked() && ctx.fileModifiable().test(folder);
        }

        public void openRename() {
            if (fileModifiable()) {
                dispatch.accept(new PackListIntent.OpenRename(target, new Entry(currentFolder())));
            }
        }

        public void delete() {
            if (fileModifiable()) {
                dispatch.accept(new PackListIntent.Delete(target, new Entry(currentFolder())));
            }
        }

        public void close() {
            dispatch.accept(new PackListIntent.CloseFolder(target.unnest()));
        }
    }
}
