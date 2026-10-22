package com.pousada.api;

import com.pousada.api.handler.ReservationHandler;
import com.pousada.api.handler.RoomHandler;
import com.pousada.api.handler.MessageHandler;
import com.pousada.api.handler.ReportHandler;
import com.pousada.db.Database;
import com.pousada.domain.Room;
import com.pousada.domain.RoomType;
import com.pousada.repository.RoomRepository;
import io.javalin.Javalin;
import java.math.BigDecimal;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");
        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            System.err.println("Missing database environment variables");
            System.exit(1);
        }

        Database.initialize(dbUrl, dbUser, dbPassword);
        initializeRooms();

        var app = Javalin.create();

        RoomHandler.register(app);
        ReservationHandler.register(app);
        MessageHandler.register(app);
        ReportHandler.register(app);

        app.start(port);
        System.out.println("Server started on port " + port);
    }

    private static void initializeRooms() {
        RoomRepository repo = RoomRepository.getInstance();
        if (repo.findAll().isEmpty()) {
            List<Room> rooms = List.of(
                new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90")),
                new Room(102, RoomType.STANDARD, 2, new BigDecimal("249.90")),
                new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90")),
                new Room(202, RoomType.DELUXE, 3, new BigDecimal("389.90")),
                new Room(301, RoomType.SUITE, 4, new BigDecimal("579.90"))
            );
            repo.saveAll(rooms);
        }
    }
}
