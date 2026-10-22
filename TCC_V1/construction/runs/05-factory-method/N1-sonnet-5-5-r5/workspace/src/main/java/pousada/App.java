package pousada;

import io.javalin.Javalin;
import pousada.persistence.Database;
import pousada.service.ReportService;
import pousada.service.ReservationService;
import pousada.web.Routes;

public final class App {
    private App() {
    }

    public static void main(String[] args) {
        Database db = new Database(required("DB_URL"), required("DB_USER"), required("DB_PASSWORD"));
        db.createSchema();

        Javalin app = Javalin.create(config -> config.showJavalinBanner = false);
        new Routes(new ReservationService(db), new ReportService(db)).register(app);
        app.start(Integer.parseInt(System.getenv().getOrDefault("PORT", "7070")));
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente ausente: " + name);
        }
        return value;
    }
}
