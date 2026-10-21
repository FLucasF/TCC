package com.pousadamareaita;

import com.pousadamareaita.http.ReservationController;
import com.pousadamareaita.http.RoomController;
import com.pousadamareaita.http.MessageController;
import com.pousadamareaita.http.ReportController;
import com.pousadamareaita.service.*;
import com.pousadamareaita.storage.*;
import io.javalin.Javalin;

public class Main {
    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();

        ReservationService reservationService = new ReservationService(
            reservationRepository,
            roomRepository,
            messageRepository
        );
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        Javalin app = Javalin.create();

        RoomController roomController = new RoomController(roomRepository);
        ReservationController reservationController = new ReservationController(reservationService);
        MessageController messageController = new MessageController(messageRepository);
        ReportController reportController = new ReportController(reportService);

        roomController.registerRoutes(app);
        reservationController.registerRoutes(app);
        messageController.registerRoutes(app);
        reportController.registerRoutes(app);

        app.start(port);
    }
}
