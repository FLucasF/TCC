package com.pousadameraalta.api;

import com.google.gson.Gson;
import io.javalin.json.JsonMapper;

import java.io.IOException;
import java.lang.reflect.Type;

public class GsonJsonMapper implements JsonMapper {
    private final Gson gson = new Gson();

    @Override
    public String toJsonString(Object obj, Type type) {
        return gson.toJson(obj, type);
    }

    @Override
    public Object fromJsonString(String json, Type targetType) {
        return gson.fromJson(json, targetType);
    }
}
