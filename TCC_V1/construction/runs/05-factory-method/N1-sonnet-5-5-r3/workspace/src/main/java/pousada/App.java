package pousada;

import io.javalin.Javalin;
import pousada.api.Routes;
import pousada.persistence.Database;

public final class App {

    private App() {
    }

    public static Javalin start(int port, Database db) {
        Javalin app = Javalin.create(config -> config.showJavalinBanner = false);
        new Routes(new ReservationService(db)).register(app);
        return app.start(port);
    }

    public static void main(String[] args) {
        String url = require("DB_URL");
        Database db = new Database(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        String port = System.getenv("PORT");
        Javalin app = start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()), db);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            db.close();
        }));
    }

    private static String require(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente obrigatória: " + name);
        }
        return value;
    }
}
