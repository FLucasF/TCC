package com.pousada.marealta;

import com.pousada.marealta.config.Database;
import com.pousada.marealta.config.Environment;
import com.pousada.marealta.controller.MessageController;
import com.pousada.marealta.controller.ReportController;
import com.pousada.marealta.controller.ReservationController;
import com.pousada.marealta.controller.RoomController;
import com.pousada.marealta.messaging.MessagePipeline;
import com.pousada.marealta.messaging.MessageRule;
import com.pousada.marealta.messaging.rules.ArchiveCopyRule;
import com.pousada.marealta.messaging.rules.TestModeRedirectRule;
import com.pousada.marealta.messaging.rules.ValidateEmailRule;
import com.pousada.marealta.repository.MessageRepository;
import com.pousada.marealta.repository.ReservationRepository;
import com.pousada.marealta.repository.RoomRepository;
import com.pousada.marealta.service.*;
import io.javalin.Javalin;

import java.util.ArrayList;
import java.util.List;

public class App {
    public static void main(String[] args) {
        // Configuração
        String dbUrl = Environment.getDbUrl();
        String dbUser = Environment.getDbUser();
        String dbPassword = Environment.getDbPassword();
        int port = Environment.getPort();
        boolean testMode = Environment.isTestMode();

        // Database
        Database database = new Database(dbUrl, dbUser, dbPassword);
        database.initialize();

        // Repositories
        RoomRepository roomRepository = new RoomRepository(database);
        ReservationRepository reservationRepository = new ReservationRepository(database);
        MessageRepository messageRepository = new MessageRepository(database);

        // Message Pipeline com regras extensíveis
        List<MessageRule> messageRules = new ArrayList<>();
        messageRules.add(new ValidateEmailRule());
        messageRules.add(new TestModeRedirectRule(testMode));
        messageRules.add(new ArchiveCopyRule(!testMode)); // Cópia só em produção

        MessagePipeline messagePipeline = new MessagePipeline(messageRules);

        // Services
        PricingService pricingService = new PricingService();
        RoomService roomService = new RoomService(roomRepository);
        MessageService messageService = new MessageService(messageRepository, messagePipeline);
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomService, pricingService, messageService
        );
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        // Controllers
        RoomController roomController = new RoomController(roomService);
        ReservationController reservationController = new ReservationController(reservationService, roomService);
        MessageController messageController = new MessageController(messageService);
        ReportController reportController = new ReportController(reportService);

        // Javalin app
        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.anyHost();
                });
            });
        });

        // Routes
        app.get("/rooms", roomController::getRooms);
        app.post("/reservations", reservationController::createReservation);
        app.get("/reservations/{id}", reservationController::getReservation);
        app.post("/reservations/{id}/confirmation", reservationController::confirmReservation);
        app.post("/reservations/{id}/cancellation", reservationController::cancelReservation);
        app.post("/reservations/{id}/check-in", reservationController::checkIn);
        app.post("/reservations/{id}/check-out", reservationController::checkOut);
        app.get("/reservations/{id}/document", reservationController::getDocument);
        app.get("/messages", messageController::getMessages);
        app.get("/reports/occupancy", reportController::getOccupancyReport);
        app.get("/reports/revenue", reportController::getRevenueReport);

        app.start(port);

        System.out.println("Pousada Maré Alta - Sistema de Reservas");
        System.out.println("Porta: " + port);
        System.out.println("Modo de teste: " + (testMode ? "ATIVADO" : "desativado"));
    }
}
