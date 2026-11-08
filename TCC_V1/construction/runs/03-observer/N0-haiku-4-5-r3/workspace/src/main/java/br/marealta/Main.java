package br.marealta;

import br.marealta.api.MessageController;
import br.marealta.api.ReportController;
import br.marealta.api.ReservationController;
import br.marealta.api.RoomController;
import br.marealta.db.*;
import br.marealta.service.*;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        Database db = Database.getInstance();

        RoomDAO roomDAO = new RoomDAO(db);
        ReservationDAO reservationDAO = new ReservationDAO(db);
        MessageDAO messageDAO = new MessageDAO(db);

        RoomService roomService = new RoomService(roomDAO);
        NotificationService notificationService = new NotificationService(messageDAO);
        ReservationService reservationService = new ReservationService(reservationDAO, roomDAO, notificationService);
        ReportService reportService = new ReportService(reservationDAO, roomDAO);

        notificationService.registerHandler(new DefaultNotificationHandler(notificationService));

        RoomController roomController = new RoomController(roomService);
        ReservationController reservationController = new ReservationController(reservationService);
        MessageController messageController = new MessageController(messageDAO);
        ReportController reportController = new ReportController(reportService);

        String port = System.getenv("PORT");
        if (port == null) {
            port = "7070";
        }

        Javalin app = Javalin.create()
            .get("/rooms", roomController::getRooms)
            .post("/reservations", reservationController::createReservation)
            .get("/reservations/{id}", reservationController::getReservation)
            .post("/reservations/{id}/confirmation", reservationController::confirmReservation)
            .post("/reservations/{id}/cancellation", reservationController::cancelReservation)
            .post("/reservations/{id}/check-in", reservationController::checkIn)
            .post("/reservations/{id}/check-out", reservationController::checkOut)
            .get("/reservations/{id}/document", reservationController::getDocument)
            .get("/messages", messageController::getMessages)
            .get("/reports/occupancy", reportController::getOccupancyReport)
            .get("/reports/revenue", reportController::getRevenueReport)
            .start(Integer.parseInt(port));

        logger.info("Server started on port {}", port);
    }
}
