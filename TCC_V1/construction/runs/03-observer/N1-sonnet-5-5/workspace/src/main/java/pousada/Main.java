package pousada;

import pousada.app.EventBus;
import pousada.app.EventSubscriber;
import pousada.app.ReportService;
import pousada.app.ReservationService;
import pousada.domain.RoomCatalog;
import pousada.infra.Database;
import pousada.infra.MessageLog;
import pousada.infra.ReservationRepository;
import pousada.notification.FinanceNotifier;
import pousada.notification.GuestNotifier;
import pousada.notification.HousekeepingNotifier;
import pousada.web.Api;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        Database db = new Database(required("DB_URL"), required("DB_USER"), required("DB_PASSWORD"));
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
        System.out.println("Pousada Maré Alta no ar na porta " + build(db).start(port).port());
    }

    /** Liga as peças. Quem quiser ser avisado dos acontecimentos da pousada entra na lista de inscritos. */
    public static Api build(Database db) {
        RoomCatalog rooms = new RoomCatalog();
        ReservationRepository repository = new ReservationRepository(rooms);
        MessageLog messages = new MessageLog(db);

        EventBus events = new EventBus();
        List<EventSubscriber> subscribers = List.of(
                new GuestNotifier(messages),
                new HousekeepingNotifier(messages),
                new FinanceNotifier(messages));
        subscribers.forEach(s -> s.subscribeTo(events));

        return new Api(new ReservationService(db, repository, rooms, events),
                new ReportService(db, repository, rooms), messages, rooms);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente " + name + " não definida");
        }
        return value;
    }
}
