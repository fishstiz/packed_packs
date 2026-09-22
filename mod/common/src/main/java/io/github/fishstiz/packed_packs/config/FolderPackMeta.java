package io.github.fishstiz.packed_packs.config;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import io.github.fishstiz.fidgetz.v0.utils.CollectionUtils;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

@JsonAdapter(FolderPackMeta.Deserializer.class)
public record FolderPackMeta(boolean module, List<String> packIds) {
    public static final String FILENAME = "packed_packs.folderpack.json";

    public FolderPackMeta {
        packIds = List.copyOf(packIds);
    }

    public FolderPackMeta(boolean module) {
        this(module, Collections.emptyList());
    }

    public FolderPackMeta withModule(boolean module) {
        return new FolderPackMeta(module, packIds);
    }

    static class Deserializer implements JsonDeserializer<FolderPackMeta> {
        @Override
        public FolderPackMeta deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (!json.isJsonObject()) {
                return new FolderPackMeta(false, Collections.emptyList());
            }

            JsonObject jsonObject = json.getAsJsonObject();

            // folders were all modules before the property was added
            boolean module = !jsonObject.has("module")
                             || !jsonObject.get("module").isJsonPrimitive()
                             || jsonObject.get("module").getAsBoolean();

            List<String> packIds = jsonObject.has("packIds") && jsonObject.get("packIds").isJsonArray()
                    ? CollectionUtils.map(jsonObject.getAsJsonArray("packIds").asList(), JsonElement::getAsString)
                    : Collections.emptyList();

            return new FolderPackMeta(module, packIds);
        }
    }
}
