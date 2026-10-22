package com.pousada;

import com.pousada.api.ReservationRoutes;
import com.pousada.api.RoomRoutes;
import com.pousada.api.MessageRoutes;
import com.pousada.api.ReportRoutes;
import com.pousada.repo.ReservationRepository;
import com.pousada.repo.RoomRepository;
import com.pousada.repo.MessageRepository;
import com.pousada.service.ReservationService;
import com.pousada.util.Database;
import io.javalin.Javalin;
import org.json.JSONObject;

public class Application {
    public static void main(String[] args) throws Exception {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new RuntimeException("Database environment variables (DB_URL, DB_USER, DB_PASSWORD) are required");
        }

        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        Database.init(dbUrl, dbUser, dbPassword);

        RoomRepository roomRepo = new RoomRepository();
        roomRepo.initializeRooms();

        ReservationRepository reservationRepo = new ReservationRepository();
        MessageRepository messageRepo = new MessageRepository();
        ReservationService reservationService = new ReservationService(reservationRepo, roomRepo, messageRepo);

        Javalin app = Javalin.create()
            .exception(Exception.class, (e, ctx) -> {
                e.printStackTrace();
                ctx.json(new JSONObject()
                    .put("error", "INTERNAL_ERROR")
                    .put("message", e.getMessage())
                    .toString());
                ctx.status(500);
            });

        RoomRoutes.register(app, roomRepo);
        ReservationRoutes.register(app, reservationService, reservationRepo);
        MessageRoutes.register(app, messageRepo);
        ReportRoutes.register(app, reservationRepo, roomRepo);

        app.start(port);
        System.out.println("Application started on port " + port);
    }
}
