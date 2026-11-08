package com.pousada.marealta;

import com.pousada.marealta.api.*;
import com.pousada.marealta.persistence.Database;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class App {
  private static final Logger logger = LoggerFactory.getLogger(App.class);

  public static void main(String[] args) {
    String dbUrl = Objects.requireNonNull(System.getenv("DB_URL"), "DB_URL environment variable is required");
    String dbUser = Objects.requireNonNull(System.getenv("DB_USER"), "DB_USER environment variable is required");
    String dbPassword = Objects.requireNonNull(System.getenv("DB_PASSWORD"), "DB_PASSWORD environment variable is required");

    String portEnv = System.getenv("PORT");
    int port = portEnv != null ? Integer.parseInt(portEnv) : 7070;

    logger.info("Initializing database...");
    Database.init(dbUrl, dbUser, dbPassword);

    logger.info("Starting application on port {}", port);
    Javalin app = Javalin.create().start(port);

    setupRoutes(app);

    logger.info("Application started successfully");

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      logger.info("Shutting down application...");
      app.stop();
      Database.close();
    }));
  }

  private static void setupRoutes(Javalin app) {
    app.get("/rooms", RoomController::getRooms);

    app.post("/reservations", ReservationController::createReservation);
    app.get("/reservations/{id}", ReservationController::getReservation);
    app.post("/reservations/{id}/confirmation", ReservationController::confirmReservation);
    app.post("/reservations/{id}/cancellation", ReservationController::cancelReservation);
    app.post("/reservations/{id}/check-in", ReservationController::checkIn);
    app.post("/reservations/{id}/check-out", ReservationController::checkOut);
    app.get("/reservations/{id}/document", ReservationController::getReceipt);

    app.get("/messages", MessageController::getMessages);

    app.get("/reports/occupancy", ReportController::getOccupancyReport);
    app.get("/reports/revenue", ReportController::getRevenueReport);
  }
}
