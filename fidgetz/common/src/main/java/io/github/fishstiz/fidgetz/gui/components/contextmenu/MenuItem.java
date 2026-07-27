package io.github.fishstiz.fidgetz.gui.components.contextmenu;

import io.github.fishstiz.fidgetz.gui.renderables.RenderableRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.util.ARGBColor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public interface MenuItem extends Runnable {
    MenuItem SEPARATOR = builder(CommonComponents.EMPTY)
            .closeOnInteract(false)
            .autoSeparate(false)
            .build();

    Component text();

    default @Nullable RenderableRect background() {
        return null;
    }

    default @Nullable Sprite icon() {
        return null;
    }

    default @Nullable Tooltip tooltip() {
        return null;
    }

    default boolean shouldAutoSeparate() {
        return true;
    }

    default boolean shouldCloseOnInteract() {
        return true;
    }

    default boolean active() {
        return true;
    }

    default int textColor() {
        return this.active() ? ARGBColor.WHITE : ContextMenu.DEFAULT_TEXT_INACTIVE_COLOR;
    }

    default List<? extends MenuItem> children() {
        return Collections.emptyList();
    }

    default boolean parent() {
        return !this.children().isEmpty();
    }

    static MenuItemBuilder builder(Component text) {
        return new MenuItemBuilder(text);
    }
}
