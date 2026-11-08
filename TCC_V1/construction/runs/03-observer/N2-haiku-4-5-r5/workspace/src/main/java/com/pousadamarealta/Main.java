package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousadamarealta.api.ReservationController;
import com.pousadamarealta.api.RoomController;
import com.pousadamarealta.api.MessageController;
import com.pousadamarealta.api.ReportController;
import com.pousadamarealta.infrastructure.Database;
import com.pousadamarealta.services.HostelNotificationListener;
import com.pousadamarealta.services.ReservationEventPublisher;
import io.javalin.Javalin;
import io.javalin.json.JsonMapper;
import java.lang.reflect.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
  private static final Logger log = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) throws Exception {
    String dbUrl = System.getenv("DB_URL");
    String dbUser = System.getenv("DB_USER");
    String dbPassword = System.getenv("DB_PASSWORD");
    String portStr = System.getenv("PORT");
    int port = portStr != null ? Integer.parseInt(portStr) : 7070;

    if (dbUrl == null || dbUser == null || dbPassword == null) {
      throw new IllegalArgumentException("DB_URL, DB_USER, and DB_PASSWORD must be set");
    }

    log.info("Initializing database...");
    Database.initialize(dbUrl, dbUser, dbPassword);
    log.info("Database initialized");

    log.info("Registering event listeners...");
    ReservationEventPublisher.subscribe(new HostelNotificationListener());

    ObjectMapper objectMapper = new ObjectMapper();

    Javalin app = Javalin.create(config -> {
      config.jsonMapper(new JsonMapper() {
        @Override
        public <T> T fromJsonString(String json, Type targetType) {
          try {
            return objectMapper.readValue(json, objectMapper.constructType(targetType));
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public String toJsonString(Object obj, Type type) {
          try {
            return objectMapper.writeValueAsString(obj);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
      });
    });

    app.get("/rooms", RoomController::listRooms);
    app.post("/reservations", ReservationController::createReservation);
    app.get("/reservations/{id}", ReservationController::getReservation);
    app.post("/reservations/{id}/confirmation", ReservationController::confirmReservation);
    app.post("/reservations/{id}/cancellation", ReservationController::cancelReservation);
    app.post("/reservations/{id}/check-in", ReservationController::checkInReservation);
    app.post("/reservations/{id}/check-out", ReservationController::checkOutReservation);
    app.get("/reservations/{id}/document", ReservationController::getDocument);
    app.get("/messages", MessageController::listMessages);
    app.get("/reports/occupancy", ReportController::getOccupancy);
    app.get("/reports/revenue", ReportController::getRevenue);

    app.exception(Exception.class, (e, ctx) -> {
      if (e.getMessage() == null || e.getMessage().isEmpty()) {
        ctx.status(500);
        ctx.json(new java.util.HashMap<>(java.util.Map.of(
            "error", "INTERNAL_ERROR",
            "message", "Erro interno do servidor"
        )));
      }
    });

    app.start(port);
    log.info("Server started on port {}", port);
  }
}
