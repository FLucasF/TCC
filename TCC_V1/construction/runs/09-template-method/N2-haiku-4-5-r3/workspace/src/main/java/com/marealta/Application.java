package com.marealta;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.marealta.api.MessagesHandler;
import com.marealta.api.ReportsHandler;
import com.marealta.api.ReservationsHandler;
import com.marealta.api.RoomsHandler;
import com.marealta.db.*;
import com.marealta.report.*;
import com.marealta.service.EmailService;
import com.marealta.service.ReceiptService;
import com.marealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");
        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            logger.error("Missing required environment variables: DB_URL, DB_USER, DB_PASSWORD");
            System.exit(1);
        }

        Database.initialize(dbUrl, dbUser, dbPassword);

        Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();

        ReservationService reservationService = new ReservationService(reservationRepository, roomRepository);
        ReceiptService receiptService = new ReceiptService(roomRepository);
        EmailService emailService = new EmailService(messageRepository, receiptService);

        ReportRegistry reportRegistry = new ReportRegistry();
        reportRegistry.register("occupancy", new OccupancyReport(roomRepository, reservationRepository));
        reportRegistry.register("revenue", new RevenueReport(reservationRepository));
        reportRegistry.register("tourism-tax", new TourismTaxReport(reservationRepository));
        reportRegistry.register("rate-plans", new RatePlansReport(reservationRepository));

        RoomsHandler roomsHandler = new RoomsHandler(roomRepository, gson);
        ReservationsHandler reservationsHandler = new ReservationsHandler(reservationService, emailService, receiptService, roomRepository, gson);
        MessagesHandler messagesHandler = new MessagesHandler(messageRepository, gson);
        ReportsHandler reportsHandler = new ReportsHandler(reportRegistry);

        Javalin app = Javalin.create()
            .get("/rooms", roomsHandler::listRooms)
            .post("/reservations", reservationsHandler::createReservation)
            .get("/reservations/{id}", reservationsHandler::getReservation)
            .post("/reservations/{id}/confirmation", reservationsHandler::confirmReservation)
            .post("/reservations/{id}/cancellation", reservationsHandler::cancelReservation)
            .post("/reservations/{id}/check-in", reservationsHandler::checkInReservation)
            .post("/reservations/{id}/check-out", reservationsHandler::checkOutReservation)
            .get("/reservations/{id}/document", reservationsHandler::getDocument)
            .get("/messages", messagesHandler::listMessages)
            .get("/reports/occupancy", reportsHandler::getOccupancyReport)
            .get("/reports/revenue", reportsHandler::getRevenueReport)
            .get("/reports/tourism-tax", reportsHandler::getTourismTaxReport)
            .get("/reports/rate-plans", reportsHandler::getRatePlansReport);

        app.start(port);
        logger.info("Application started on port {}", port);
    }
}
