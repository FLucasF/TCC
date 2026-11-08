package com.pousada.marealta.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class GsonConfig {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public static Gson createGson() {
        return new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, typeOfSrc, context) ->
                context.serialize(src.format(DATE_FORMATTER)))
            .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, typeOfT, context) ->
                LocalDate.parse(json.getAsString(), DATE_FORMATTER))
            .registerTypeAdapter(BigDecimal.class, (JsonSerializer<BigDecimal>) (src, typeOfSrc, context) -> {
                if (src == null) return context.serialize(null);
                return context.serialize(src.setScale(2, java.math.RoundingMode.HALF_EVEN).doubleValue());
            })
            .registerTypeAdapter(BigDecimal.class, (JsonDeserializer<BigDecimal>) (json, typeOfT, context) ->
                new BigDecimal(json.getAsString()))
            .create();
    }
}
