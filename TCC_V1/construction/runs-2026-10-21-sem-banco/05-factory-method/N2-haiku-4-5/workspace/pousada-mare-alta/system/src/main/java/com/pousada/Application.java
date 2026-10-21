package com.pousada;

import com.pousada.controller.MessageController;
import com.pousada.controller.ReportController;
import com.pousada.controller.ReservationController;
import com.pousada.controller.RoomController;
import com.pousada.document.DocumentRendererFactory;
import com.pousada.service.DocumentService;
import com.pousada.service.MessageService;
import com.pousada.service.PricingService;
import com.pousada.service.ReportService;
import com.pousada.service.ReservationService;
import com.pousada.service.RoomService;
import io.javalin.Javalin;

public class Application {
    public static void main(String[] args) {
        var port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        var roomService = new RoomService();
        var pricingService = new PricingService();
        var reservationService = new ReservationService(roomService, pricingService);
        var messageService = new MessageService();
        var documentRendererFactory = new DocumentRendererFactory(roomService);
        var documentService = new DocumentService(documentRendererFactory);
        var reportService = new ReportService(reservationService, roomService);

        var roomController = new RoomController(roomService);
        var reservationController = new ReservationController(reservationService, messageService, documentService);
        var reportController = new ReportController(reportService);
        var messageController = new MessageController(messageService);

        var app = Javalin.create().start(port);

        roomController.registerRoutes(app);
        reservationController.registerRoutes(app);
        reportController.registerRoutes(app);
        messageController.registerRoutes(app);
    }
}
