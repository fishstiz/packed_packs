package io.github.fishstiz.packed_packs.gui.model;

import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.config.ProfileManager;
import io.github.fishstiz.packed_packs.gui.intents.ProfileIntent;
import io.github.fishstiz.packed_packs.gui.states.PackedPacksState;
import io.github.fishstiz.packed_packs.gui.states.ProfilesState;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIntBiConsumer;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ProfilesViewModel {
    public static final Component TITLE_TEXT = ResourceUtil.getText("profile");
    public static final Component NO_PROFILE_TEXT = ResourceUtil.getText("profile.none");
    public static final Component UNNAMED_TEXT = ResourceUtil.getText("profile.unnamed");
    private final ProfileManager manager;
    private final Consumer<ProfileIntent> dispatch;
    private final Supplier<PackedPacksState> state;
    private final Map<Property, List<Runnable>> listeners = new EnumMap<>(Property.class);
    private ProfilesState cachedState;

    public enum Property {
        ALL,
        PROFILES,
        SELECTED,
    }

    public ProfilesViewModel(ProfileManager manager, Consumer<ProfileIntent> dispatch, Supplier<PackedPacksState> state) {
        this.manager = manager;
        this.dispatch = dispatch;
        this.state = state;
        this.cachedState = state.get().profiles();
    }

    private void notifyListeners(Property property) {
        this.listeners.getOrDefault(property, Collections.emptyList()).forEach(Runnable::run);
    }

    void onStateChanged() {
        ProfilesState prev = this.cachedState;
        ProfilesState state = this.state.get().profiles();
        if (prev == state) return;

        this.cachedState = state;

        this.notifyListeners(Property.ALL);

        // default profile is pushed to top of list
        if (prev.profiles() != state.profiles() || !Objects.equals(prev.defaultProfile(), state.defaultProfile())) {
            this.notifyListeners(Property.PROFILES);
        }

        if (!Objects.equals(prev.selectedProfile(), state.selectedProfile())) {
            this.notifyListeners(Property.SELECTED);
        }
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
        ProfilesState currentState = this.state.get().profiles();
        Profile defaultProfile = currentState.defaultProfile();
        List<Profile> profiles = currentState.profiles();

        int i = 0;
        if (defaultProfile != null) {
            consumer.accept(new Entry(defaultProfile), i++);
        }
        for (Profile profile : profiles) {
            if (!Objects.equals(defaultProfile, profile)) {
                consumer.accept(new Entry(profile), i++);
            }
        }
    }

    public @Nullable Profile selectedProfile() {
        return this.state.get().profiles().selectedProfile();
    }

    public @Nullable Profile defaultProfile() {
        return this.state.get().profiles().defaultProfile();
    }

    public void unselect() {
        this.dispatch.accept(new ProfileIntent.Select(null));
    }

    public void copySelected() {
        Profile selectedProfile = this.state.get().profiles().selectedProfile();
        Profile copiedProfile;

        if (selectedProfile == null) {
            copiedProfile = this.manager.create(NO_PROFILE_TEXT.getString());
        } else {
            selectedProfile.setPacks(this.state.get().enabled().packs());
            copiedProfile = this.manager.copy(selectedProfile);
        }

        this.dispatch.accept(new ProfileIntent.Add(copiedProfile));
    }

    public void renameSelected(String name) {
        String newName = name;
        if (name == null || name.isEmpty()) {
            newName = UNNAMED_TEXT.getString();
        }

        Profile selectedProfile = this.state.get().profiles().selectedProfile();
        if (selectedProfile != null) {
            this.dispatch.accept(new ProfileIntent.Rename(this.state.get().profiles().selectedProfile(), newName));
        }
    }

    public class Entry {
        private final Profile profile;

        protected Entry(Profile profile) {
            this.profile = profile;
        }

        public Component name() {
            return Component.literal(this.profile.getName());
        }

        public boolean isSelected() {
            return Objects.equals(this.profile, ProfilesViewModel.this.state.get().profiles().selectedProfile());
        }

        public boolean isDefault() {
            return Objects.equals(this.profile, ProfilesViewModel.this.state.get().profiles().defaultProfile());
        }

        public boolean isLocked() {
            return this.profile.isLocked();
        }

        public void delete() {
            ProfilesViewModel.this.dispatch.accept(new ProfileIntent.Delete(this.profile));
        }

        public void select() {
            ProfilesViewModel.this.dispatch.accept(new ProfileIntent.Select(this.profile));
        }

        public void toggleLock() {
            ProfilesViewModel.this.dispatch.accept(new ProfileIntent.ToggleLock(this.profile));
        }

        public void toggleDefault() {
            ProfilesViewModel.this.dispatch.accept(new ProfileIntent.SetDefault(this.isDefault() ? null : this.profile));
        }
    }
}
