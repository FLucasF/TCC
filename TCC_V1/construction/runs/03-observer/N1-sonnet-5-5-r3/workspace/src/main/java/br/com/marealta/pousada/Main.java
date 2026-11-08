package br.com.marealta.pousada;

import br.com.marealta.pousada.notification.FinanceNotifier;
import br.com.marealta.pousada.notification.GuestNotifier;
import br.com.marealta.pousada.notification.HousekeepingNotifier;
import br.com.marealta.pousada.notification.ReservationEvents;
import br.com.marealta.pousada.notification.ReservationListener;
import br.com.marealta.pousada.persistence.Database;
import br.com.marealta.pousada.persistence.MessageRepository;
import br.com.marealta.pousada.persistence.ReservationRepository;
import br.com.marealta.pousada.service.ReportService;
import br.com.marealta.pousada.service.ReservationService;
import br.com.marealta.pousada.web.App;

import java.util.List;

public class Main {

    /** Quem é avisado dos acontecimentos das reservas. Para incluir ou tirar alguém, mexa só aqui. */
    static List<ReservationListener> listeners() {
        return List.of(
                new GuestNotifier(),
                new HousekeepingNotifier(),
                new FinanceNotifier());
    }

    public static void main(String[] args) {
        Database database = new Database(
                requiredEnv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        database.initialize();
        start(database, port());
    }

    public static io.javalin.Javalin start(Database database, int port) {
        ReservationRepository reservations = new ReservationRepository();
        ReservationService service = new ReservationService(
                database, reservations, new MessageRepository(), new ReservationEvents(listeners()));
        ReportService reports = new ReportService(database, reservations);
        return new App(service, reports).create().start(port);
    }

    private static int port() {
        String port = System.getenv("PORT");
        return port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim());
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente " + name + " não definida");
        }
        return value;
    }
}
