package com.pousada;

import io.javalin.Javalin;
import com.pousada.infrastructure.Database;
import com.pousada.api.*;

public class Application {
  public static void main(String[] args) {
    Database.initialize();

    Javalin app = Javalin.create();

    RoomController.routes(app);
    ReservationController.routes(app);
    MessageController.routes(app);
    ReportController.routes(app);

    int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
    app.start(port);
  }
}
