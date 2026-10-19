package com.pousadamareaalta;

import com.pousadamareaalta.controller.ReservationController;
import com.pousadamareaalta.service.*;
import io.javalin.Javalin;

public class Main {
    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        RoomService roomService = new RoomService();
        PricingService pricingService = new PricingService();
        MessageService messageService = new MessageService();
        ReservationService reservationService = new ReservationService(roomService, pricingService, messageService);
        ReportService reportService = new ReportService(roomService, reservationService);

        Javalin app = Javalin.create().start(port);

        ReservationController controller = new ReservationController(roomService, reservationService,
                                                                     messageService, reportService);
        controller.registerRoutes(app);

        System.out.println("Server started on port " + port);
    }
}
