package com.pousada;

import com.pousada.api.ApiHandlers;
import com.pousada.business.ReservationService;
import com.pousada.db.*;
import io.javalin.Javalin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Application {
    public static void main(String[] args) throws Exception {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");
        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        Database database = new Database(dbUrl, dbUser, dbPassword);
        database.initialize();

        initializeRooms(database);

        RoomRepository roomRepo = new RoomRepository(database);
        ReservationRepository reservationRepo = new ReservationRepository(database);
        MessageRepository messageRepo = new MessageRepository(database);
        ReservationService reservationService = new ReservationService(reservationRepo, roomRepo, messageRepo);

        ApiHandlers handlers = new ApiHandlers(roomRepo, reservationService, reservationRepo, messageRepo);

        Javalin app = Javalin.create()
                .start(port);

        app.get("/rooms", handlers::getRooms);
        app.post("/reservations", handlers::createReservation);
        app.get("/reservations/{id}", handlers::getReservation);
        app.post("/reservations/{id}/confirmation", handlers::confirmReservation);
        app.post("/reservations/{id}/cancellation", handlers::cancelReservation);
        app.post("/reservations/{id}/check-in", handlers::checkIn);
        app.post("/reservations/{id}/check-out", handlers::checkOut);
        app.get("/reservations/{id}/document", handlers::getDocument);
        app.get("/messages", handlers::getMessages);
        app.get("/reports/occupancy", handlers::getOccupancyReport);
        app.get("/reports/revenue", handlers::getRevenueReport);
        app.get("/reports/tourism-tax", handlers::getTourismTaxReport);
        app.get("/reports/rate-plans", handlers::getRatePlanReport);

        System.out.println("Pousada Maré Alta started on port " + port);
    }

    private static void initializeRooms(Database database) throws Exception {
        try (java.sql.Connection conn = database.getConnection()) {
            java.sql.Statement stmt = conn.createStatement();
            java.sql.ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM rooms");
            rs.next();
            if (rs.getInt("count") == 0) {
                List<Database.RoomData> rooms = new ArrayList<>();
                rooms.add(new Database.RoomData(101, "STANDARD", 2, new BigDecimal("249.90")));
                rooms.add(new Database.RoomData(102, "STANDARD", 2, new BigDecimal("249.90")));
                rooms.add(new Database.RoomData(201, "DELUXE", 3, new BigDecimal("389.90")));
                rooms.add(new Database.RoomData(202, "DELUXE", 3, new BigDecimal("389.90")));
                rooms.add(new Database.RoomData(301, "SUITE", 4, new BigDecimal("579.90")));
                database.insertRooms(rooms);
            }
            stmt.close();
        }
    }
}
