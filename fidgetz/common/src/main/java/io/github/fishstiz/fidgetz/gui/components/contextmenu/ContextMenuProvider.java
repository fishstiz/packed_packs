package io.github.fishstiz.fidgetz.gui.components.contextmenu;

public interface ContextMenuProvider {
    void buildItems(ContextMenuItemBuilder builder, int mouseX, int mouseY);

    default ContextMenuItemBuilder buildItems(int mouseX, int mouseY) {
        ContextMenuItemBuilder builder = new ContextMenuItemBuilder();
        this.buildItems(builder, mouseX, mouseY);
        return builder;
    }
}
