package com.mareaalta;

import com.mareaalta.api.*;
import com.mareaalta.infrastructure.Database;
import com.mareaalta.infrastructure.RoomRepository;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class Application {
    public static void main(String[] args) throws Exception {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new RuntimeException("Variáveis de ambiente DB_URL, DB_USER, DB_PASSWORD são obrigatórias");
        }

        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        Database.initialize(dbUrl, dbUser, dbPassword);
        RoomRepository.initializeRooms();

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson());
        }).start(port);

        app.get("/rooms", RoomController::getRooms);

        app.post("/reservations", ReservationController::createReservation);
        app.get("/reservations/{id}", ReservationController::getReservation);
        app.post("/reservations/{id}/confirmation", ReservationController::confirmReservation);
        app.post("/reservations/{id}/cancellation", ReservationController::cancelReservation);
        app.post("/reservations/{id}/check-in", ReservationController::checkIn);
        app.post("/reservations/{id}/check-out", ReservationController::checkOut);
        app.get("/reservations/{id}/document", ReservationController::getDocument);

        app.get("/messages", MessageController::getMessages);

        app.get("/reports/occupancy", ReportController::getOccupancyReport);
        app.get("/reports/revenue", ReportController::getRevenueReport);

        System.out.println("Pousada Maré Alta - Sistema de Reservas iniciado na porta " + port);
    }
}
