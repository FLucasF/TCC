package com.marealata;

import com.marealata.database.Database;
import com.marealata.handler.MessageHandler;
import com.marealata.handler.ReportHandler;
import com.marealata.handler.ReservationHandler;
import com.marealata.handler.RoomHandler;
import io.javalin.Javalin;
import io.javalin.json.JsonMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Type;

public class App {
    private static final Gson gson = new GsonBuilder().create();

    public static void main(String[] args) throws Exception {
        Database.initialize();

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
        var app = Javalin.create(config -> {
            config.jsonMapper(new JsonMapper() {
                @Override
                public String toJsonString(Object obj, Type type) {
                    return gson.toJson(obj);
                }

                @Override
                public Object fromJsonString(String json, Type targetType) {
                    return gson.fromJson(json, targetType);
                }
            });
        }).start(port);

        RoomHandler.register(app);
        ReservationHandler.register(app);
        MessageHandler.register(app);
        ReportHandler.register(app);

        System.out.println("Pousada Maré Alta - Sistema iniciado na porta " + port);
    }
}
