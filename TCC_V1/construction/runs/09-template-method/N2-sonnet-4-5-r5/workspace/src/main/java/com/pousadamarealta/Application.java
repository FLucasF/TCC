package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.controller.MessageController;
import com.pousadamarealta.controller.ReportController;
import com.pousadamarealta.controller.ReservationController;
import com.pousadamarealta.controller.RoomController;
import com.pousadamarealta.dto.ErrorResponse;
import com.pousadamarealta.exception.ApiException;
import com.pousadamarealta.exception.InvalidRequestException;
import com.pousadamarealta.repository.Database;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.*;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.json.JavalinJackson;

public class Application {
    public static void main(String[] args) {
        String dbUrl = getEnv("DB_URL");
        String dbUser = getEnv("DB_USER");
        String dbPassword = getEnv("DB_PASSWORD");
        int port = getPort();

        Database database = new Database(dbUrl, dbUser, dbPassword);

        RoomRepository roomRepository = new RoomRepository(database.getDataSource());
        ReservationRepository reservationRepository = new ReservationRepository(database.getDataSource());
        MessageRepository messageRepository = new MessageRepository(database.getDataSource());

        PricingService pricingService = new PricingService();
        DocumentService documentService = new DocumentService();
        MessageService messageService = new MessageService(messageRepository, documentService);
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, pricingService, messageService, documentService
        );
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        RoomController roomController = new RoomController(roomRepository);
        ReservationController reservationController = new ReservationController(reservationService);
        MessageController messageController = new MessageController(messageRepository);
        ReportController reportController = new ReportController(reportService);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        });

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
        app.get("/reports/tourism-tax", reportController::getTourismTaxReport);
        app.get("/reports/rate-plans", reportController::getRatePlansReport);

        app.exception(ApiException.class, (e, ctx) -> {
            ctx.status(e.getHttpStatus())
               .json(new ErrorResponse(e.getCode(), e.getMessage()));
        });

        app.exception(BadRequestResponse.class, (e, ctx) -> {
            ctx.status(400)
               .json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        });

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500)
               .json(new ErrorResponse("INTERNAL_ERROR", "An internal error occurred"));
        });

        app.start(port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            database.close();
        }));
    }

    private static String getEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Environment variable " + name + " is required");
        }
        return value;
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv == null || portEnv.isBlank()) {
            return 7070;
        }
        try {
            return Integer.parseInt(portEnv);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid PORT value: " + portEnv);
        }
    }
}
