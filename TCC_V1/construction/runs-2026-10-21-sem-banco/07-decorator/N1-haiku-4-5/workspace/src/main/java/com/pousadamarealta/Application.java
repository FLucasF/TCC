package com.pousadamarealta;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pousadamarealta.api.ApiRoutes;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.RoomService;
import io.javalin.Javalin;
import io.javalin.json.JsonMapper;
import java.lang.reflect.Type;

public class Application {
    public static void main(String[] args) {
        int port = 7070;
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            try {
                port = Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                System.err.println("Invalid PORT environment variable: " + portEnv);
            }
        }

        RoomService roomService = new RoomService();
        MessageService messageService = new MessageService();
        ReservationService reservationService = new ReservationService(roomService, messageService);
        ReportService reportService = new ReportService(reservationService, roomService);

        Gson gson = new GsonBuilder().serializeNulls().create();

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JsonMapper() {
                @Override
                public String toJsonString(Object obj, Type type) {
                    return gson.toJson(obj, type);
                }

                @Override
                public <T> T fromJsonString(String json, Type targetType) {
                    return gson.fromJson(json, targetType);
                }
            });
        }).start(port);

        ApiRoutes routes = new ApiRoutes(roomService, reservationService, messageService, reportService);
        routes.setupRoutes(app);

        System.out.println("Server started on port " + port);
    }
}
