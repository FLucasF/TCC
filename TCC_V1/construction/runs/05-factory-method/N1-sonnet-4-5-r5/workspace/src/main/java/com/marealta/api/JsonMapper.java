package com.marealta.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonMapper {
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);

    public static ObjectMapper get() {
        return MAPPER;
    }
}
