package com.pousada;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousada.api.ErrorHandler;
import com.pousada.api.controller.MessageController;
import com.pousada.api.controller.ReportController;
import com.pousada.api.controller.ReservationController;
import com.pousada.api.controller.RoomController;
import com.pousada.domain.service.*;
import com.pousada.domain.validation.ValidationException;
import com.pousada.infrastructure.repository.MessageRepository;
import com.pousada.infrastructure.repository.ReservationRepository;
import com.pousada.infrastructure.repository.RoomRepository;
import com.pousada.report.generators.OccupancyReportGenerator;
import com.pousada.report.generators.RatePlansReportGenerator;
import com.pousada.report.generators.RevenueReportGenerator;
import com.pousada.report.generators.TourismTaxReportGenerator;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class Main {

    public static void main(String[] args) {
        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();

        RoomService roomService = new RoomService(roomRepository);
        ReservationService reservationService = new ReservationService(reservationRepository, roomService);
        MessageService messageService = new MessageService(messageRepository);
        DocumentService documentService = new DocumentService(roomService, messageService);

        ReportService reportService = new ReportService(reservationRepository, roomRepository);
        reportService.registerGenerator(new OccupancyReportGenerator());
        reportService.registerGenerator(new RevenueReportGenerator());
        reportService.registerGenerator(new TourismTaxReportGenerator());
        reportService.registerGenerator(new RatePlansReportGenerator());

        RoomController roomController = new RoomController(roomService);
        ReservationController reservationController = new ReservationController(
            reservationService, documentService, roomService);
        MessageController messageController = new MessageController(messageService);
        ReportController reportController = new ReportController(reportService);

        int port = getPort();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, false));
        }).start(port);

        app.get("/rooms", roomController::getAllRooms);

        app.post("/reservations", reservationController::createReservation);
        app.get("/reservations/{id}", reservationController::getReservation);
        app.post("/reservations/{id}/confirmation", reservationController::confirmReservation);
        app.post("/reservations/{id}/cancellation", reservationController::cancelReservation);
        app.post("/reservations/{id}/check-in", reservationController::checkInReservation);
        app.post("/reservations/{id}/check-out", reservationController::checkOutReservation);
        app.get("/reservations/{id}/document", reservationController::getDocument);

        app.get("/messages", messageController::getAllMessages);

        app.get("/reports/occupancy", reportController::getOccupancyReport);
        app.get("/reports/revenue", reportController::getRevenueReport);
        app.get("/reports/tourism-tax", reportController::getTourismTaxReport);
        app.get("/reports/rate-plans", reportController::getRatePlansReport);

        app.exception(ValidationException.class, ErrorHandler::handleValidationException);
        app.exception(Exception.class, ErrorHandler::handleGenericException);

        System.out.println("Pousada Maré Alta - Sistema de Reservas");
        System.out.println("Servidor iniciado na porta " + port);
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                System.err.println("Porta inválida na variável PORT: " + portEnv);
            }
        }
        return 7070;
    }
}
