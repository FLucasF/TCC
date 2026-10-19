package com.pousada;

import com.pousada.api.*;
import com.pousada.service.*;
import io.javalin.Javalin;
import io.javalin.json.JsonMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.lang.reflect.Type;
import java.math.BigDecimal;

public class Application {
    public static void main(String[] args) {
        int port = getPort();

        RoomService roomService = new RoomService();
        MessageService messageService = new MessageService();
        ReservationService reservationService = new ReservationService(roomService, messageService);
        ReportService reportService = new ReportService(reservationService, roomService);

        RoomController roomController = new RoomController(roomService);
        ReservationController reservationController = new ReservationController(reservationService);
        MessageController messageController = new MessageController(messageService);
        ReportController reportController = new ReportController(reportService);

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(BigDecimal.class, (com.google.gson.JsonSerializer<BigDecimal>) (src, typeOfSrc, context) ->
                        context.serialize(src.doubleValue()))
                .registerTypeAdapter(BigDecimal.class, (com.google.gson.JsonDeserializer<BigDecimal>) (json, typeOfT, context) -> {
                    try {
                        return new BigDecimal(json.getAsString());
                    } catch (Exception e) {
                        return new BigDecimal(json.getAsDouble());
                    }
                })
                .create();

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JsonMapper() {
                @Override
                public <T> T fromJsonString(String jsonString, Type targetType) {
                    return gson.fromJson(jsonString, targetType);
                }

                @Override
                public String toJsonString(Object obj, Type type) {
                    return gson.toJson(obj, type);
                }
            });
        }).start(port);

        app.get("/rooms", roomController::getAllRooms);

        app.post("/reservations", reservationController::createReservation);
        app.get("/reservations/{id}", reservationController::getReservation);
        app.post("/reservations/{id}/confirmation", reservationController::confirmReservation);
        app.post("/reservations/{id}/cancellation", reservationController::cancelReservation);
        app.post("/reservations/{id}/check-in", reservationController::checkIn);
        app.post("/reservations/{id}/check-out", reservationController::checkOut);
        app.get("/reservations/{id}/document", reservationController::getDocument);

        app.get("/messages", messageController::getAllMessages);

        app.get("/reports/occupancy", reportController::getOccupancyReport);
        app.get("/reports/revenue", reportController::getRevenueReport);

        System.out.println("Pousada Maré Alta API running on port " + port);
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                return 7070;
            }
        }
        return 7070;
    }
}
