package io.github.fishstiz.packed_packs.config;

import com.google.gson.*;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.transform.interfaces.ConfiguredPack;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.Type;

public final class PackOverride implements Serializable {
    private static final String HIDDEN_SERIALIZED_NAME = "hidden";
    private static final String REQUIRED_SERIALIZED_NAME = "required";
    private static final String POSITION_SERIALIZED_NAME = "position";
    private @Nullable Boolean hidden;
    private @Nullable Boolean required;
    private @Nullable Position position;

    public PackOverride() {
    }

    public PackOverride(@Nullable Boolean hidden, @Nullable Boolean required, @Nullable PackOverride.Position position) {
        this.hidden = hidden;
        this.required = required;
        this.position = position;
    }

    public boolean hasOverride() {
        return Boolean.TRUE.equals(this.hidden) || this.required != null || this.position != null;
    }

    public @Nullable Boolean hidden() {
        return hidden;
    }

    void setHidden(@Nullable Boolean hidden) {
        this.hidden = hidden;
    }

    public @Nullable Boolean required() {
        return required;
    }

    void setRequired(@Nullable Boolean required) {
        this.required = required;
    }

    public @Nullable Position position() {
        return position;
    }

    void setPosition(@Nullable Position position) {
        this.position = position;
    }

    public enum Position {
        UNFIXED(null),
        TOP(Pack.Position.TOP),
        BOTTOM(Pack.Position.BOTTOM);

        private final Pack.Position position;

        Position(Pack.Position position) {
            this.position = position;
        }

        public boolean fixed() {
            return this != UNFIXED;
        }

        Pack.Position get(Pack pack) {
            return this.fixed() ? this.position : ((ConfiguredPack) pack).packed_packs$originalConfig().defaultPosition();
        }
    }

    static class Adapter implements JsonSerializer<PackOverride>, JsonDeserializer<PackOverride> {
        @Override
        public JsonElement serialize(PackOverride src, Type typeOfSrc, JsonSerializationContext context) {
            if (!src.hasOverride()) return null;

            JsonObject obj = new JsonObject();
            if (src.hidden() != null) obj.addProperty(HIDDEN_SERIALIZED_NAME, src.hidden());
            if (src.required() != null) obj.addProperty(REQUIRED_SERIALIZED_NAME, src.required());
            if (src.position() != null) obj.addProperty(POSITION_SERIALIZED_NAME, src.position().name());
            return obj;
        }

        @Override
        public PackOverride deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();

            Boolean hidden = obj.has(HIDDEN_SERIALIZED_NAME) ? obj.get(HIDDEN_SERIALIZED_NAME).getAsBoolean() : null;
            Boolean required = obj.has(REQUIRED_SERIALIZED_NAME) ? obj.get(REQUIRED_SERIALIZED_NAME).getAsBoolean() : null;
            Position position = null;

            if (obj.has(POSITION_SERIALIZED_NAME)) {
                try {
                    position = Position.valueOf(obj.get(POSITION_SERIALIZED_NAME).getAsString().toUpperCase());
                } catch (IllegalArgumentException e) {
                    PackedPacks.LOGGER.error("[packed_packs] Invalid value for key 'position': '{}'. Expected one of {}", position, Position.values());
                }
            }

            return new PackOverride(hidden, required, position);
        }
    }
}
