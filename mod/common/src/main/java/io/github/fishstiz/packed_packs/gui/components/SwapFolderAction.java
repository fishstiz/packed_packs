package io.github.fishstiz.packed_packs.gui.components;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;

@JsonAdapter(SwapFolderAction.class)
public enum SwapFolderAction implements JsonDeserializer<SwapFolderAction> {
    NO,
    YES,
    UNLOCKED_ONLY;

    public static SwapFolderAction getOrDefault(String name) {
        try {
            if (name.equals("true")) {
                return SwapFolderAction.YES;
            } else if (name.equals("false")) {
                return SwapFolderAction.NO;
            }

            return SwapFolderAction.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return NO;
        }
    }

    @Override
    public SwapFolderAction deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            return SwapFolderAction.getOrDefault(json.getAsString());
        } catch (Exception e) {
            return SwapFolderAction.NO;
        }
    }
}