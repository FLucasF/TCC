package com.pousadamarealta;

import io.javalin.Javalin;
import com.pousadamarealta.infrastructure.Database;
import com.pousadamarealta.handler.RoomHandler;
import com.pousadamarealta.handler.ReservationHandler;
import com.pousadamarealta.handler.MessageHandler;
import com.pousadamarealta.handler.ReportHandler;

public class Application {
    public static void main(String[] args) throws Exception {
        Database db = new Database();
        db.initialize();

        RoomHandler roomHandler = new RoomHandler(db);
        ReservationHandler reservationHandler = new ReservationHandler(db);
        MessageHandler messageHandler = new MessageHandler(db);
        ReportHandler reportHandler = new ReportHandler(db);

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        Javalin app = Javalin.create()
                .get("/rooms", roomHandler::listRooms)
                .post("/reservations", reservationHandler::createReservation)
                .get("/reservations/{id}", reservationHandler::getReservation)
                .post("/reservations/{id}/confirmation", reservationHandler::confirmReservation)
                .post("/reservations/{id}/cancellation", reservationHandler::cancelReservation)
                .post("/reservations/{id}/check-in", reservationHandler::checkIn)
                .post("/reservations/{id}/check-out", reservationHandler::checkOut)
                .get("/reservations/{id}/document", reservationHandler::getDocument)
                .get("/messages", messageHandler::listMessages)
                .get("/reports/occupancy", reportHandler::occupancy)
                .get("/reports/revenue", reportHandler::revenue)
                .get("/reports/tourism-tax", reportHandler::tourismTax)
                .get("/reports/rate-plans", reportHandler::ratePlans)
                .start(port);

        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
    }
}
