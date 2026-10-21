package com.pousamarealta;

import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import com.pousamarealta.controllers.*;
import com.pousamarealta.models.*;
import com.pousamarealta.services.*;

public class App {
    public static void main(String[] args) {
        ReservationService reservationService = new ReservationService();
        RoomService roomService = new RoomService();
        MessageService messageService = new MessageService();
        ReportService reportService = new ReportService(reservationService);

        int port = 7070;
        String portEnv = System.getenv("PORT");
        if (portEnv != null) {
            try {
                port = Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                // fallback to 7070
            }
        }

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson());
        }).start(port);

        RoomController roomController = new RoomController(roomService);
        ReservationController reservationController = new ReservationController(reservationService, messageService, reportService);

        roomController.setupRoutes(app);
        reservationController.setupRoutes(app);
    }
}
