package io.github.fishstiz.packed_packs.gui2;

import io.github.fishstiz.fidgetz.v0.gui.state.FZRef;
import io.github.fishstiz.packed_packs.gui.HistoryManager;
import io.github.fishstiz.packed_packs.gui2.intents.Intent;
import io.github.fishstiz.packed_packs.gui2.intents.PackListIntent;
import io.github.fishstiz.packed_packs.gui2.intents.ProfileIntent;
import io.github.fishstiz.packed_packs.gui2.mutations.Mutation;
import io.github.fishstiz.packed_packs.gui.states.PackedPacksState;
import net.minecraft.client.Minecraft;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class Store implements FZRef<PackedPacksState> {
    private final ConcurrentHashMap<String, Runnable> subscribers = new ConcurrentHashMap<>();
    private final HistoryManager<PackedPacksState> history;
    private PackedPacksState state = PackedPacksState.empty();

    public Store(Minecraft minecraft) {
        this.history = new HistoryManager<>(state);
    }

    @Override
    public PackedPacksState value() {
        return state;
    }

    @Override
    public <R> Runnable subscribe(String key, Function<PackedPacksState, R> selector, Consumer<R> callback) {
        MutableObject<R> last = new MutableObject<>(selector.apply(this.state));
        Runnable listener = () -> {
            R next = selector.apply(this.state);
            if (next != last.get()) {
                callback.accept(next);
                last.setValue(next);
            }
        };
        subscribers.put(key, listener);
        return () -> subscribers.remove(key);
    }

    public void dispatch(Intent intent) {
        switch (intent) { // todo effects/map to mutation
            case Intent.Reset reset -> {
            }
            case PackListIntent packListIntent -> {
            }
            case ProfileIntent profileIntent -> {
            }
        }
    }

    private synchronized void dispatch(Mutation mutation) {
        PackedPacksState prevState = this.state;
        PackedPacksState newState = Reducer.reduce(prevState, mutation);

        if (prevState != newState) {
            if (mutation.pushState()) {
                history.push(newState);
            } else if (mutation.resetHistory()) {
                history.reset(newState);
            }

            subscribers.values().forEach(Runnable::run);
        }
    }
}
