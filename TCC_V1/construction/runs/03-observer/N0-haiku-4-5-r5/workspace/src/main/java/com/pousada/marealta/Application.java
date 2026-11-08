package com.pousada.marealta;

import com.pousada.marealta.config.DatabaseConfig;
import com.pousada.marealta.controller.ReservationController;
import com.pousada.marealta.controller.RoomController;
import com.pousada.marealta.controller.ReportController;
import com.pousada.marealta.controller.MessageController;
import com.pousada.marealta.notification.NotificationService;
import com.pousada.marealta.repository.MessageRepository;
import io.javalin.Javalin;

public class Application {
    public static void main(String[] args) {
        DatabaseConfig.initialize();

        NotificationService notificationService = NotificationService.getInstance();
        notificationService.initialize(new MessageRepository());

        var app = Javalin.create();

        RoomController.register(app);
        ReservationController.register(app);
        ReportController.register(app);
        MessageController.register(app);

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
        app.start(port);
    }
}
