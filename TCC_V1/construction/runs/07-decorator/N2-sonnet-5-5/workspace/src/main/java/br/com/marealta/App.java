package br.com.marealta;

import br.com.marealta.messaging.Installation;
import br.com.marealta.messaging.MessageSender;
import br.com.marealta.persistence.Database;
import br.com.marealta.persistence.JdbcMessageStore;
import br.com.marealta.persistence.ReservationRepository;
import br.com.marealta.service.ReservationService;
import br.com.marealta.web.WebApp;
import io.javalin.Javalin;

public final class App {
    private App() {}

    public static void main(String[] args) {
        Database database = new Database(
                required("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        boolean testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));
        int port = System.getenv("PORT") == null ? 7070 : Integer.parseInt(System.getenv("PORT"));

        Javalin app = build(database, testMode);
        app.start(port);
    }

    public static Javalin build(Database database, boolean testMode) {
        database.initialize();
        ReservationRepository reservations = new ReservationRepository(database);
        JdbcMessageStore messages = new JdbcMessageStore(database);
        MessageSender sender = Installation.marealta(messages, testMode);
        ReservationService service = new ReservationService(reservations, sender);
        return new WebApp(service, reservations, messages).create();
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente " + name + " não definida");
        }
        return value;
    }
}
