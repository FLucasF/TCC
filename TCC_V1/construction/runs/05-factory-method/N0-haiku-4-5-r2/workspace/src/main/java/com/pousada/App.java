package com.pousada;

import io.javalin.Javalin;
import com.pousada.db.Database;
import com.pousada.api.RoomController;
import com.pousada.api.ReservationController;
import com.pousada.api.MessageController;
import com.pousada.api.ReportController;

public class App {
    public static void main(String[] args) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");
        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        Database db = new Database(dbUrl, dbUser, dbPassword);
        db.init();

        Javalin app = Javalin.create();

        RoomController.setup(app, db);
        ReservationController.setup(app, db);
        MessageController.setup(app, db);
        ReportController.setup(app, db);

        app.start(port);
    }
}
