package br.com.marealta.reservas;

import br.com.marealta.reservas.api.Api;
import br.com.marealta.reservas.notification.FinanceNotifier;
import br.com.marealta.reservas.notification.GuestConfirmationEmail;
import br.com.marealta.reservas.notification.HousekeepingNotifier;
import br.com.marealta.reservas.notification.MessageLog;
import br.com.marealta.reservas.notification.ReservationListener;
import br.com.marealta.reservas.persistence.Database;
import br.com.marealta.reservas.persistence.MessageRepository;
import br.com.marealta.reservas.persistence.ReservationRepository;
import br.com.marealta.reservas.persistence.RoomRepository;
import br.com.marealta.reservas.report.Reports;
import br.com.marealta.reservas.service.ReservationService;
import io.javalin.Javalin;

import java.util.List;

public final class Main {
    private static final String HOUSEKEEPING_ADDRESS = "governanca@pousadamarealta.com.br";
    private static final String FINANCE_ADDRESS = "financeiro@pousadamarealta.com.br";

    private Main() {
    }

    public static void main(String[] args) {
        Database db = new Database(System.getenv("DB_URL"), System.getenv("DB_USER"),
                System.getenv("DB_PASSWORD"));
        String port = System.getenv("PORT");
        build(db).start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }

    /** Monta a aplicação. Quem é avisado das reservas é decidido aqui: um novo interessado entra nesta lista. */
    public static Javalin build(Database db) {
        db.initialize();
        RoomRepository rooms = new RoomRepository(db);
        rooms.seed();
        ReservationRepository reservations = new ReservationRepository(db);
        MessageRepository messages = new MessageRepository(db);
        MessageLog log = new MessageLog(messages);

        List<ReservationListener> listeners = List.of(
                new GuestConfirmationEmail(log),
                new HousekeepingNotifier(log, HOUSEKEEPING_ADDRESS),
                new FinanceNotifier(log, FINANCE_ADDRESS));

        ReservationService service = new ReservationService(db, rooms, reservations, listeners);
        return new Api(service, new Reports(db, rooms, reservations), messages).create();
    }
}
