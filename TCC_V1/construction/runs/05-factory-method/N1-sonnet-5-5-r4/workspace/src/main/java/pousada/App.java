package pousada;

import io.javalin.Javalin;
import pousada.persistence.Database;
import pousada.service.ReportService;
import pousada.service.ReservationService;
import pousada.web.Api;

public final class App {
    private App() {
    }

    public static void main(String[] args) {
        Database db = Database.connect(
                require("DB_URL"),
                System.getenv("DB_USER"),
                System.getenv("DB_PASSWORD"));
        Javalin app = new Api(new ReservationService(db), new ReportService(db)).create();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            db.close();
        }));
        app.start(port());
    }

    private static int port() {
        String port = System.getenv("PORT");
        return port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim());
    }

    private static String require(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente obrigatória: " + name);
        }
        return value;
    }
}
