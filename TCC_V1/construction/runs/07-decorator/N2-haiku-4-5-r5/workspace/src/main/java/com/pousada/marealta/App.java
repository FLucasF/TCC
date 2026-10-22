package com.pousada.marealta;

import com.pousada.marealta.api.MessagesHandler;
import com.pousada.marealta.api.ReportsHandler;
import com.pousada.marealta.api.ReservationsHandler;
import com.pousada.marealta.api.RoomsHandler;
import com.pousada.marealta.config.Config;
import com.pousada.marealta.config.DatabaseConnection;
import com.pousada.marealta.repository.MessageRepository;
import com.pousada.marealta.repository.ReservationRepository;
import com.pousada.marealta.repository.RoomRepository;
import io.javalin.Javalin;

public class App {
    public static void main(String[] args) throws Exception {
        DatabaseConnection.initialize();

        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();

        RoomsHandler roomsHandler = new RoomsHandler(roomRepository);
        ReservationsHandler reservationsHandler = new ReservationsHandler(
            reservationRepository,
            roomRepository,
            messageRepository
        );
        MessagesHandler messagesHandler = new MessagesHandler(messageRepository);
        ReportsHandler reportsHandler = new ReportsHandler(reservationRepository, roomRepository);

        Javalin app = Javalin.create().start(Config.getPort());

        app.get("/rooms", roomsHandler::getRooms);

        app.post("/reservations", reservationsHandler::createReservation);
        app.get("/reservations/{id}", reservationsHandler::getReservation);
        app.post("/reservations/{id}/confirmation", reservationsHandler::confirmReservation);
        app.post("/reservations/{id}/cancellation", reservationsHandler::cancelReservation);
        app.post("/reservations/{id}/check-in", reservationsHandler::checkIn);
        app.post("/reservations/{id}/check-out", reservationsHandler::checkOut);
        app.get("/reservations/{id}/document", reservationsHandler::getDocument);

        app.get("/messages", messagesHandler::getMessages);

        app.get("/reports/occupancy", reportsHandler::getOccupancyReport);
        app.get("/reports/revenue", reportsHandler::getRevenueReport);

        System.out.println("Server running on port " + Config.getPort());
    }
}
