package com.pousada;

import com.pousada.api.ApiRoutes;
import com.pousada.db.Database;
import com.pousada.services.MessageService;
import com.pousada.services.ReportService;
import com.pousada.services.ReservationService;
import io.javalin.Javalin;

public class Application {
    public static void main(String[] args) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");
        int port = portStr != null ? Integer.parseInt(portStr) : 7070;
        boolean testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            System.err.println("Error: DB_URL, DB_USER, and DB_PASSWORD environment variables are required");
            System.exit(1);
        }

        Database db = new Database(dbUrl, dbUser, dbPassword);
        MessageService messageService = new MessageService(testMode, true);
        ReservationService reservationService = new ReservationService(db, messageService);
        ReportService reportService = new ReportService(db);

        Javalin app = Javalin.create();
        ApiRoutes routes = new ApiRoutes(db, reservationService, reportService, messageService);
        routes.register(app);

        app.start(port);
        System.out.println("Server running on port " + port);
    }
}
