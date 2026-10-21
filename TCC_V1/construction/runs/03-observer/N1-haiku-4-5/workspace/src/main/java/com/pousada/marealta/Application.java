package com.pousada.marealta;

import io.javalin.Javalin;

import com.pousada.marealta.api.ReservationController;
import com.pousada.marealta.notification.FinanceNotificationHandler;
import com.pousada.marealta.notification.GoveranceNotificationHandler;
import com.pousada.marealta.notification.GuestEmailHandler;
import com.pousada.marealta.notification.NotificationService;
import com.pousada.marealta.service.MessageService;
import com.pousada.marealta.service.OccupancyReportService;
import com.pousada.marealta.service.ReservationService;
import com.pousada.marealta.service.RevenueReportService;
import com.pousada.marealta.service.RoomService;

public class Application {
    public static void main(String[] args) {
        String portStr = System.getenv("PORT");
        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        RoomService roomService = new RoomService();
        MessageService messageService = new MessageService();
        NotificationService notificationService = new NotificationService();

        notificationService.registerHandler(new GuestEmailHandler(messageService));
        notificationService.registerHandler(new GoveranceNotificationHandler(messageService));
        notificationService.registerHandler(new FinanceNotificationHandler(messageService));

        ReservationService reservationService = new ReservationService(roomService, notificationService);
        OccupancyReportService occupancyReportService = new OccupancyReportService(reservationService, roomService);
        RevenueReportService revenueReportService = new RevenueReportService(reservationService);

        ReservationController controller = new ReservationController(
            roomService, reservationService, messageService, occupancyReportService, revenueReportService
        );

        Javalin app = Javalin.create();

        controller.registerRoutes(app);

        app.start(port);
    }
}
