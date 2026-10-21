package com.pousadamarealta;

import com.pousadamarealta.config.Config;
import com.pousadamarealta.email.EmailRuleChain;
import com.pousadamarealta.handler.ReportHandler;
import com.pousadamarealta.handler.ReservationHandler;
import com.pousadamarealta.handler.RoomHandler;
import com.pousadamarealta.handler.MessageHandler;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;

public class Main {
    public static void main(String[] args) {
        // Initialize repositories
        RoomRepository roomRepo = new RoomRepository();
        ReservationRepository resRepo = new ReservationRepository();
        MessageRepository msgRepo = new MessageRepository();

        // Initialize services
        ReservationService resService = new ReservationService(resRepo, roomRepo);
        EmailRuleChain emailChain = new EmailRuleChain(Config.isTestMode(), Config.isArchiveCopyEnabled());
        MessageService msgService = new MessageService(msgRepo, emailChain);
        ReportService reportService = new ReportService(resRepo, roomRepo);

        // Initialize handlers
        RoomHandler roomHandler = new RoomHandler(roomRepo);
        ReservationHandler resHandler = new ReservationHandler(resService, msgService, resRepo, roomRepo);
        MessageHandler msgHandler = new MessageHandler(msgRepo);
        ReportHandler reportHandler = new ReportHandler(reportService);

        // Create Javalin app
        Javalin app = Javalin.create()
            .get("/rooms", roomHandler::listRooms)
            .post("/reservations", resHandler::createReservation)
            .get("/reservations/{id}", resHandler::getReservation)
            .post("/reservations/{id}/confirmation", resHandler::confirmReservation)
            .post("/reservations/{id}/cancellation", resHandler::cancelReservation)
            .post("/reservations/{id}/check-in", resHandler::checkIn)
            .post("/reservations/{id}/check-out", resHandler::checkOut)
            .get("/reservations/{id}/document", resHandler::getDocument)
            .get("/messages", msgHandler::listMessages)
            .get("/reports/occupancy", reportHandler::occupancy)
            .get("/reports/revenue", reportHandler::revenue)
            .start(Config.getPort());
    }
}
