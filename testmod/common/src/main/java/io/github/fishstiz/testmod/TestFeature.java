package io.github.fishstiz.testmod;

import io.github.fishstiz.packed_packs.api.EventBus;
import io.github.fishstiz.packed_packs.api.Preference;
import io.github.fishstiz.packed_packs.api.PreferenceRegistry;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.api.gui.ContextMenuItemSpec;
import io.github.fishstiz.testmod.events.ContextMenuTestFeatureEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public final class TestFeature {
    private final PreferenceRegistry registry;
    private final Preference<Boolean> preference;
    private final Component label;
    private final List<TestFeature> subFeatures = new ArrayList<>();
    private final @Nullable TestFeature parent;
    private final boolean defaultEnabled;

    private TestFeature(PreferenceRegistry registry, String preference, String label, @Nullable TestFeature parent, boolean defaultEnabled) {
        this.registry = registry;
        this.preference = registry.register(TestMod.id(preference), defaultEnabled);
        this.label = Component.literal(label);
        this.parent = parent;
        this.defaultEnabled = defaultEnabled;
    }

    public TestFeature registerSubFeature(String key, String label) {
        TestFeature sub = new TestFeature(this.registry, this.id().getPath() + "." + key, label, this, this.defaultEnabled);
        subFeatures.add(sub);
        return sub;
    }

    public Preference<Boolean> preference() {
        return preference;
    }

    public Identifier id() {
        return preference.id();
    }

    public Component label() {
        return label;
    }

    public boolean isEnabled() {
        return preference.get() && (parent == null || parent.isEnabled());
    }

    public void setEnabled(boolean enabled) {
        preference.set(enabled);
    }

    public static Builder builder(PreferenceRegistry registry, String key, String label) {
        return new Builder(registry, key, label);
    }

    public static final class Builder {
        private final PreferenceRegistry registry;
        private final String key;
        private final String label;
        private boolean defaultEnabled = true;
        private boolean rebuildScreenOnChange = false;
        private BiConsumer<Boolean, ScreenContext> toggleHandler;
        private final List<Identifier> dependencies = new ArrayList<>();

        private Builder(PreferenceRegistry registry, String key, String label) {
            this.registry = registry;
            this.key = key;
            this.label = label;
        }

        public Builder onToggle(BiConsumer<Boolean, ScreenContext> toggleHandler) {
            this.toggleHandler = toggleHandler;
            return this;
        }

        public Builder defaultEnabled(boolean defaultEnabled) {
            this.defaultEnabled = defaultEnabled;
            return this;
        }

        public Builder rebuildOnChange() {
            this.rebuildScreenOnChange = true;
            return this;
        }

        public Builder after(Identifier dependency) {
            this.dependencies.add(dependency);
            return this;
        }

        public Builder after(List<Identifier> dependencies) {
            this.dependencies.addAll(dependencies);
            return this;
        }

        public TestFeature register(EventBus eventBus) {
            TestFeature feature = new TestFeature(registry, key, label, null, defaultEnabled);
            eventBus.register(ContextMenuTestFeatureEvent.class, feature.preference.id(), dependencies, event ->
                    event.addItem(item -> buildItem(feature, item, event.screenContext())));
            return feature;
        }

        private void buildItem(TestFeature feature, ContextMenuItemSpec item, ScreenContext ctx) {
            item.applyDevStyle();
            item.label(feature.label());
            item.closeOnInteract(false);
            item.asToggle(feature::isEnabled, value -> {
                feature.setEnabled(value);
                if (rebuildScreenOnChange) ctx.rebuild();
                if (toggleHandler != null) toggleHandler.accept(value, ctx);
            });
            buildSubFeatures(feature, item, ctx);
        }

        private void buildSubFeatures(TestFeature feature, ContextMenuItemSpec item, ScreenContext ctx) {
            if (feature.subFeatures.isEmpty()) return;
            item.children(feature.subFeatures, (sub, child) -> {
                child.applyDevStyle()
                        .label(sub.label())
                        .closeOnInteract(false)
                        .active(feature::isEnabled)
                        .asToggle(sub::isEnabled, value -> {
                            sub.setEnabled(value);
                            if (rebuildScreenOnChange) ctx.rebuild();
                            if (toggleHandler != null) toggleHandler.accept(value, ctx);
                        });

                buildSubFeatures(sub, child, ctx);
            });
        }
    }
}
