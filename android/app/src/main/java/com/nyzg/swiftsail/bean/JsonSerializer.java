package com.nyzg.swiftsail.bean;


import com.google.gson.Gson;

public class JsonSerializer {
    static public <T> String serialize(T obj) {
        if (obj == null) {
            return null;
        }
        return new Gson().toJson(obj);
    }

    static public <T> T deSerialize(String json, Class<T> clazz) {
        if (json == null) {
            return null;
        }
        return new Gson().fromJson(json, clazz);
    }
}
