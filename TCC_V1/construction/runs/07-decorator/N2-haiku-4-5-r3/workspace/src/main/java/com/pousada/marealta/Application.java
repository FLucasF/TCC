package com.pousada.marealta;

import com.pousada.marealta.api.MessageController;
import com.pousada.marealta.api.ReportController;
import com.pousada.marealta.api.ReservationController;
import com.pousada.marealta.api.RoomController;
import com.pousada.marealta.infrastructure.Database;
import com.pousada.marealta.infrastructure.MessageRepository;
import com.pousada.marealta.infrastructure.ReservationRepository;
import com.pousada.marealta.infrastructure.RoomRepository;
import com.pousada.marealta.service.MessageService;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

public class Application {
    public static void main(String[] args) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");
        int port = portStr != null ? Integer.parseInt(portStr) : 7070;
        boolean testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));
        boolean copyToArchive = !"false".equalsIgnoreCase(System.getenv("COPY_TO_ARCHIVE"));

        Database database = new Database(dbUrl, dbUser, dbPassword);
        try {
            database.initialize();
        } catch (Exception e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            System.exit(1);
        }

        RoomRepository roomRepository = new RoomRepository(database);
        ReservationRepository reservationRepository = new ReservationRepository(database);
        MessageRepository messageRepository = new MessageRepository(database);
        MessageService messageService = new MessageService(messageRepository, testMode, copyToArchive);

        RoomController roomController = new RoomController(roomRepository);
        ReservationController reservationController = new ReservationController(
            reservationRepository, roomRepository, messageService);
        MessageController messageController = new MessageController(messageRepository);
        ReportController reportController = new ReportController(reservationRepository, roomRepository);

        Javalin app = Javalin.create()
            .exception(Exception.class, (e, ctx) -> {
                e.printStackTrace();
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
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

        app.start(port);
        System.out.println("Pousada Maré Alta API running on port " + port);
    }
}
