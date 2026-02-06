package com.nyzg.swiftsail.bean;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

public class MyJsonSerializer {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) -> {
                if (json == null || json.isJsonNull()) {
                    return null;
                }
                String text = json.getAsString().trim();
                if (text.isEmpty()) {
                    return null;
                }
                try {
                    return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                } catch (DateTimeParseException e) {
                    throw new JsonParseException("Unable to parse LocalDateTime: " + text, e);
                }
            })
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) -> {
                if (src == null) {
                    return JsonNull.INSTANCE;
                }
                return new JsonPrimitive(src.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            })
            .create();

    static public <T> String serialize(T obj) {
        if (obj == null) {
            return null;
        }
        return GSON.toJson(obj);
    }

    public static <T> Optional<T> mapToObject(Map<String, Object> map, Class<T> clazz) {
        if (map == null) {
            return Optional.empty();
        }
        try {
            String json = GSON.toJson(map);
            return Optional.of(GSON.fromJson(json, clazz));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    static public <T> T deSerialize(String json, Class<T> clazz) {
        if (json == null) {
            return null;
        }
        return GSON.fromJson(json, clazz);
    }
}
