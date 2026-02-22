package io.github.fishstiz.packed_packs.api.events;

import io.github.fishstiz.packed_packs.api.Event;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import net.minecraft.client.gui.components.AbstractWidget;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;

/**
 * Fired during the screen's layout initialization.
 * <p>
 * Used to inject custom widgets into specific screen regions.
 * <p>
 * Widgets added via this event are automatically positioned within their respective target areas.
 * <p>
 * The main widgets will automatically stretch or shrink to fill the remaining
 * layout space around injected widgets.
 */
public final class InitializeLayoutEvent extends ScreenEvent implements Event {
    private final Map<Pos, List<AbstractWidget>> widgets = new EnumMap<>(Pos.class);

    @ApiStatus.Internal
    public InitializeLayoutEvent(ScreenContext context) {
        super(context);
    }

    public enum Pos {
        /**
         * After the title in the header.
         */
        AFTER_TITLE,
        /**
         * At the very start of the footer, contained within the left column.
         */
        BEFORE_FOOTER,
        /**
         * After the 'Open Folder' button, contained within footer's left column.
         */
        AFTER_LEFT_FOOTER,
        /**
         * Before the 'Apply' button, contained within the footer's right column.
         */
        BEFORE_RIGHT_FOOTER,
        /**
         * Between the 'Apply' and 'Done' buttons, contained within the footer's right column.
         */
        BETWEEN_RIGHT_FOOTER,
        /**
         * At the very end of the footer, contained within the right column.
         */
        AFTER_FOOTER
    }

    /**
     * Adds an element into the layout at the specified position.
     * <p>
     * Elements are automatically positioned within their respective target areas.
     */
    public void addWidget(Pos position, AbstractWidget element) {
        this.widgets.computeIfAbsent(position, p -> new ArrayList<>()).add(element);
    }

    @ApiStatus.Internal
    public List<AbstractWidget> getPendingWidgets(Pos position) {
        return this.widgets.getOrDefault(position, Collections.emptyList());
    }
}
