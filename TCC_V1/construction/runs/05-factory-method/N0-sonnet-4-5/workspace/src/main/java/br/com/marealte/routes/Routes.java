package br.com.marealte.routes;

import br.com.marealte.controller.MessageController;
import br.com.marealte.controller.ReportController;
import br.com.marealte.controller.ReservationController;
import br.com.marealte.controller.RoomController;
import br.com.marealte.repository.MessageRepository;
import br.com.marealte.repository.ReservationRepository;
import br.com.marealte.repository.RoomRepository;
import br.com.marealte.service.ReportService;
import io.javalin.Javalin;

public class Routes {

    public static void register(Javalin app) {
        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();

        RoomController roomController = new RoomController(roomRepository);
        ReservationController reservationController = new ReservationController(
            reservationRepository, roomRepository, messageRepository);
        MessageController messageController = new MessageController(messageRepository);
        ReportController reportController = new ReportController(
            new ReportService(reservationRepository));

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
