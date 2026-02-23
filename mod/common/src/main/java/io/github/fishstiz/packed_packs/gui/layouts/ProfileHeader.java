package io.github.fishstiz.packed_packs.gui.layouts;

import io.github.fishstiz.fidgetz.gui.components.FidgetzButton;
import io.github.fishstiz.fidgetz.gui.components.ToggleableEditBox;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayoutElement;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.ButtonSprites;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.config.ProfileManager;
import io.github.fishstiz.packed_packs.gui.model.ProfilesViewModel;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

import static io.github.fishstiz.packed_packs.gui.model.ProfilesViewModel.NO_PROFILE_TEXT;
import static io.github.fishstiz.packed_packs.gui.model.ProfilesViewModel.UNNAMED_TEXT;
import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.*;

public class ProfileHeader implements Layout, FlexLayoutElement {
    private final FlexLayout layout = FlexLayout.horizontal().spacing(SPACING);
    private final ProfilesViewModel viewModel;
    private final ToggleableEditBox<Void> nameField;
    private final FidgetzButton<Void> toggleNameButton;
    private final ButtonSprites cachedToggleSprites = new ButtonSprites(
            Sprite.of16(ResourceUtil.getIcon("edit")),
            Sprite.of16(ResourceUtil.getIcon("edit_inactive"))
    );

    public ProfileHeader(ProfilesViewModel viewModel, Consumer<String> onRename) {
        this.viewModel = viewModel;
        this.nameField = ToggleableEditBox.<Void>builder()
                .setHint(UNNAMED_TEXT)
                .setMaxLength(ProfileManager.getNameMaxLength())
                .setFilter(value -> value != null && (value.isEmpty() || !value.isBlank()))
                .addListener(this.viewModel::renameSelected)
                .addListener(onRename)
                .build();
        this.toggleNameButton = FidgetzButton.<Void>builder()
                .makeSquare()
                .setTooltip(Tooltip.create(ResourceUtil.getText("profile.edit")))
                .setSprite(this.cachedToggleSprites)
                .setOnPress(this.nameField::toggle)
                .build();

        this.layout.addChild(this.toggleNameButton);
        this.layout.addFlexChild(this.nameField);

        this.viewModel.subscribe(ProfilesViewModel.Property.ALL, this::refresh);
    }

    private void refresh() {
        Profile selectedProfile = this.viewModel.selectedProfile();
        boolean hasProfile = selectedProfile != null;
        this.nameField.setEditable(false);
        this.nameField.setHint(hasProfile ? UNNAMED_TEXT : NO_PROFILE_TEXT);
        this.nameField.setValueSilently(hasProfile ? selectedProfile.getName() : "");
        this.nameField.visible = hasProfile;
        this.nameField.active = hasProfile;
        this.toggleNameButton.visible = hasProfile;
        this.toggleNameButton.active = hasProfile && !selectedProfile.isLocked();
        this.toggleNameButton.setSprites(this.resolveToggleSprites(selectedProfile));
    }

    private ButtonSprites resolveToggleSprites(@Nullable Profile profile) {
        if (profile == null || !profile.isLocked()) return this.cachedToggleSprites;
        return Objects.equals(profile, this.viewModel.defaultProfile())
                ? ButtonSprites.of(STAR_SPRITE)
                : ButtonSprites.unclamp(LOCK_SPRITE);
    }

    @Override
    public void setWidth(int width) {
        this.layout.setWidth(width);
    }

    @Override
    public void setHeight(int height) {
        this.layout.setHeight(height);
    }

    @Override
    public void setX(int x) {
        this.layout.setX(x);
    }

    @Override
    public void setY(int y) {
        this.layout.setY(y);
    }

    @Override
    public int getX() {
        return this.layout.getX();
    }

    @Override
    public int getY() {
        return this.layout.getY();
    }

    @Override
    public int getWidth() {
        return this.layout.getWidth();
    }

    @Override
    public int getHeight() {
        return this.layout.getHeight();
    }

    @Override
    public void visitChildren(@NonNull Consumer<LayoutElement> visitor) {
        this.layout.visitChildren(visitor);
    }

    @Override
    public void visitWidgets(@NonNull Consumer<AbstractWidget> visitor) {
        this.layout.visitWidgets(visitor);
    }

    @Override
    public void arrangeElements() {
        this.layout.arrangeElements();
    }
}
