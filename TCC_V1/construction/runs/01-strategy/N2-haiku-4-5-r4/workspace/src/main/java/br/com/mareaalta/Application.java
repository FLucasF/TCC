package br.com.mareaalta;

import br.com.mareaalta.api.ReservationAPI;
import br.com.mareaalta.repository.Database;
import br.com.mareaalta.service.ReservationService;
import io.javalin.Javalin;

public class Application {
    public static void main(String[] args) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");

        int port = 7070;
        if (portStr != null && !portStr.isBlank()) {
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                port = 7070;
            }
        }

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            System.err.println("Environment variables DB_URL, DB_USER, and DB_PASSWORD are required");
            System.exit(1);
        }

        try {
            Database db = new Database(dbUrl, dbUser, dbPassword);
            db.initialize();

            ReservationService service = new ReservationService(db);

            Javalin app = Javalin.create()
                    .start(port);

            new ReservationAPI(app, service);

            System.out.println("Pousada Maré Alta running on port " + port);
        } catch (Exception e) {
            System.err.println("Failed to start application: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
