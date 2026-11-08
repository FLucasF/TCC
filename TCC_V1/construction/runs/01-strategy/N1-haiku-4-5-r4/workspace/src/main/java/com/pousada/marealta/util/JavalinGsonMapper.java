package com.pousada.marealta.util;

import com.google.gson.Gson;
import io.javalin.json.JsonMapper;

import java.io.IOException;
import java.lang.reflect.Type;

public class JavalinGsonMapper implements JsonMapper {
    private final Gson gson;

    public JavalinGsonMapper() {
        this.gson = GsonConfig.createGson();
    }

    @Override
    public String toJsonString(Object obj, Type type) {
        return gson.toJson(obj, type);
    }

    @Override
    public Object fromJsonString(String json, Type type) {
        return gson.fromJson(json, type);
    }
}
