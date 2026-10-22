package br.com.pousadamarealta;

import br.com.pousadamarealta.api.*;
import br.com.pousadamarealta.repository.*;
import br.com.pousadamarealta.service.*;
import br.com.pousadamarealta.service.observer.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class App {
    public static void main(String[] args) {
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/pousada");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        DatabaseSetup databaseSetup = new DatabaseSetup(dbUrl, dbUser, dbPassword);
        databaseSetup.initialize();

        RoomRepository roomRepository = new RoomRepository(dbUrl, dbUser, dbPassword);
        ReservationRepository reservationRepository = new ReservationRepository(dbUrl, dbUser, dbPassword);
        MessageRepository messageRepository = new MessageRepository(dbUrl, dbUser, dbPassword);

        NotificationService notificationService = new NotificationService();
        notificationService.addObserver(new GuestNotificationObserver(messageRepository));
        notificationService.addObserver(new HousekeepingNotificationObserver(messageRepository));
        notificationService.addObserver(new FinanceNotificationObserver(messageRepository));

        RoomService roomService = new RoomService(roomRepository);
        ReservationService reservationService = new ReservationService(reservationRepository, notificationService);
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        RoomController roomController = new RoomController(roomService);
        ReservationController reservationController = new ReservationController(reservationService, roomService);
        MessageController messageController = new MessageController(messageRepository);
        ReportController reportController = new ReportController(reportService);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, false));
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
    }
}
