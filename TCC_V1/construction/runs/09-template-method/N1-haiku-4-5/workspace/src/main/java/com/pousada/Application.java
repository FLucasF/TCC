package com.pousada;

import com.pousada.api.ApiController;
import com.pousada.service.*;
import io.javalin.Javalin;

public class Application {
    public static void main(String[] args) {
        RoomRepository roomRepo = new RoomRepository();
        ReservationRepository reservationRepo = new ReservationRepository();
        MessageRepository messageRepo = new MessageRepository();

        ReservationService reservationService = new ReservationService(reservationRepo, roomRepo, messageRepo);
        ReportService reportService = new ReportService(reservationRepo, roomRepo);
        ApiController apiController = new ApiController(reservationService, reportService, roomRepo, messageRepo, reservationRepo);

        int port = 7070;
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            try {
                port = Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                System.err.println("Invalid PORT environment variable, using default 7070");
            }
        }

        Javalin app = Javalin.create();
        apiController.setupRoutes(app);
        app.start(port);
    }
}
