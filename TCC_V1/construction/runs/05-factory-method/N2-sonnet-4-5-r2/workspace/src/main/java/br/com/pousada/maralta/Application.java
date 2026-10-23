package br.com.pousada.maralta;

import br.com.pousada.maralta.controller.*;
import br.com.pousada.maralta.database.Database;
import br.com.pousada.maralta.repository.*;
import br.com.pousada.maralta.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class Application {
    public static void main(String[] args) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new IllegalStateException("DB_URL, DB_USER, and DB_PASSWORD must be set");
        }

        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        Database database = new Database(dbUrl, dbUser, dbPassword);

        RoomRepository roomRepository = new RoomRepository(database);
        ReservationRepository reservationRepository = new ReservationRepository(database);
        MessageRepository messageRepository = new MessageRepository(database);

        PricingService pricingService = new PricingService();
        RoomService roomService = new RoomService(roomRepository);
        MessageService messageService = new MessageService(messageRepository);
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomService, pricingService, messageService
        );
        ReportService reportService = new ReportService(reservationRepository, roomService);

        RoomController roomController = new RoomController(roomService);
        ReservationController reservationController = new ReservationController(reservationService);
        MessageController messageController = new MessageController(messageService);
        ReportController reportController = new ReportController(reportService);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        }).start(port);

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

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            database.close();
        }));

        System.out.println("Pousada Maré Alta system started on port " + port);
    }
}
