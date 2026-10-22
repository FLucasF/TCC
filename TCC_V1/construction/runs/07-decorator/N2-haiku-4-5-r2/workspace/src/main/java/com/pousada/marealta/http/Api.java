package com.pousada.marealta.http;

import com.pousada.marealta.config.DatabaseConfig;
import com.pousada.marealta.messaging.ArchiveCopyRule;
import com.pousada.marealta.messaging.EmailValidationRule;
import com.pousada.marealta.messaging.MessageProcessor;
import com.pousada.marealta.messaging.TestModeRedirectRule;
import com.pousada.marealta.repository.MessageRepository;
import com.pousada.marealta.repository.ReservationRepository;
import com.pousada.marealta.repository.RoomRepository;
import com.pousada.marealta.service.MessageService;
import com.pousada.marealta.service.ReportService;
import com.pousada.marealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

public class Api {
    public static void register(Javalin app) {
        RoomRepository roomRepository = new RoomRepository();
        roomRepository.initialize();

        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();

        ReservationService reservationService = new ReservationService(reservationRepository, roomRepository);
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        MessageProcessor messageProcessor = new MessageProcessor();
        messageProcessor.addRule(new EmailValidationRule());
        messageProcessor.addRule(new TestModeRedirectRule());
        messageProcessor.addRule(new ArchiveCopyRule(true));

        MessageService messageService = new MessageService(messageRepository, messageProcessor);

        RoomHandler roomHandler = new RoomHandler(roomRepository);
        ReservationHandler reservationHandler = new ReservationHandler(reservationService, messageService, roomRepository);
        MessageHandler messageHandler = new MessageHandler(messageService);
        ReportHandler reportHandler = new ReportHandler(reportService);

        app.get("/rooms", roomHandler::listRooms);
        app.post("/reservations", reservationHandler::createReservation);
        app.get("/reservations/{id}", reservationHandler::getReservation);
        app.post("/reservations/{id}/confirmation", reservationHandler::confirmReservation);
        app.post("/reservations/{id}/cancellation", reservationHandler::cancelReservation);
        app.post("/reservations/{id}/check-in", reservationHandler::checkInReservation);
        app.post("/reservations/{id}/check-out", reservationHandler::checkOutReservation);
        app.get("/reservations/{id}/document", reservationHandler::getDocument);
        app.get("/messages", messageHandler::listMessages);
        app.get("/reports/occupancy", reportHandler::occupancyReport);
        app.get("/reports/revenue", reportHandler::revenueReport);
    }
}
