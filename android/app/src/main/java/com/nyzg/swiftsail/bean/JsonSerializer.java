package com.nyzg.swiftsail.bean;


import com.google.gson.Gson;

import java.util.Map;
import java.util.Optional;

public class JsonSerializer {
    private static final Gson GSON = new Gson();

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
