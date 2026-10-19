package com.pousadameraalta;

import com.pousadameraalta.api.GsonJsonMapper;
import com.pousadameraalta.api.handler.ApiHandler;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

public class Main {
    public static void main(String[] args) {
        int port = getPort();

        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
            config.jsonMapper(new GsonJsonMapper());
        });

        ApiHandler handler = new ApiHandler();

        app.get("/rooms", handler::getRooms);
        app.post("/reservations", handler::createReservation);
        app.get("/reservations/{id}", handler::getReservation);
        app.post("/reservations/{id}/confirmation", handler::confirmReservation);
        app.post("/reservations/{id}/cancellation", handler::cancelReservation);
        app.post("/reservations/{id}/check-in", handler::checkIn);
        app.post("/reservations/{id}/check-out", handler::checkOut);
        app.get("/reservations/{id}/document", handler::getDocument);
        app.get("/messages", handler::getMessages);
        app.get("/reports/occupancy", handler::getOccupancyReport);
        app.get("/reports/revenue", handler::getRevenueReport);

        app.start(port);

        System.out.println("Pousada Maré Alta - Sistema de Reservas iniciado na porta " + port);
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                System.err.println("PORT inválida, usando 7070");
            }
        }
        return 7070;
    }
}
