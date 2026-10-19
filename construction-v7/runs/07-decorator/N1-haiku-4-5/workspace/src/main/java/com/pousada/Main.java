package com.pousada;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousada.api.ApiRoutes;
import com.pousada.services.HotelService;
import io.javalin.Javalin;
import io.javalin.json.JsonMapper;
import java.lang.reflect.Type;
import org.jetbrains.annotations.NotNull;

public class Main {
    public static void main(String[] args) {
        boolean testMode = "true".equals(System.getenv("HOTEL_TEST_MODE"));
        int port = 7070;

        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            try {
                port = Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                System.err.println("Invalid PORT value: " + portEnv);
            }
        }

        HotelService hotelService = new HotelService(testMode);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JsonMapper() {
                @NotNull
                @Override
                public String toJsonString(@NotNull Object obj, @NotNull Type type) {
                    try {
                        return objectMapper.writeValueAsString(obj);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                @NotNull
                @Override
                public <T> T fromJsonString(@NotNull String json, @NotNull Type targetType) {
                    try {
                        return objectMapper.readValue(json, objectMapper.constructType(targetType));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        });

        ApiRoutes.setup(app, hotelService);

        app.start(port);
        System.out.println("Server started on port " + port + " (test mode: " + testMode + ")");
    }
}
