package com.pousada;

import io.javalin.Javalin;
import com.pousada.database.Database;
import com.pousada.api.RoomsEndpoint;
import com.pousada.api.ReservationsEndpoint;
import com.pousada.api.MessagesEndpoint;
import com.pousada.api.ReportsEndpoint;
import com.pousada.event.EventDispatcher;
import com.pousada.event.listener.GuestNotificationListener;
import com.pousada.event.listener.HousekeepingNotificationListener;
import com.pousada.event.listener.FinancialNotificationListener;

public class Application {
  public static void main(String[] args) {
    int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

    Database.initialize();

    EventDispatcher.subscribe(new GuestNotificationListener());
    EventDispatcher.subscribe(new HousekeepingNotificationListener());
    EventDispatcher.subscribe(new FinancialNotificationListener());

    Javalin app = Javalin.create()
        .get("/rooms", RoomsEndpoint::listRooms)
        .post("/reservations", ReservationsEndpoint::createReservation)
        .get("/reservations/:id", ReservationsEndpoint::getReservation)
        .post("/reservations/:id/confirmation", ReservationsEndpoint::confirmReservation)
        .post("/reservations/:id/cancellation", ReservationsEndpoint::cancelReservation)
        .post("/reservations/:id/check-in", ReservationsEndpoint::checkIn)
        .post("/reservations/:id/check-out", ReservationsEndpoint::checkOut)
        .get("/reservations/:id/document", ReservationsEndpoint::getDocument)
        .get("/messages", MessagesEndpoint::listMessages)
        .get("/reports/occupancy", ReportsEndpoint::getOccupancy)
        .get("/reports/revenue", ReportsEndpoint::getRevenue)
        .start(port);
  }
}
