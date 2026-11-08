package br.com.marealta;

import br.com.marealta.app.ReportService;
import br.com.marealta.app.ReservationService;
import br.com.marealta.app.RoomCatalog;
import br.com.marealta.notification.FinanceNotifier;
import br.com.marealta.notification.GuestNotifier;
import br.com.marealta.notification.HousekeepingNotifier;
import br.com.marealta.notification.ReservationListener;
import br.com.marealta.persistence.Database;
import br.com.marealta.persistence.MessageStore;
import br.com.marealta.persistence.ReservationRepository;
import br.com.marealta.web.Api;
import io.javalin.Javalin;
import java.util.List;
import javax.sql.DataSource;

/** Monta o sistema. Quem é avisado das reservas é decidido aqui, e só aqui. */
public final class Application {

    private Application() {
    }

    public static Javalin create(DataSource dataSource) {
        Database db = new Database(dataSource);
        db.createSchema();

        RoomCatalog rooms = new RoomCatalog();
        ReservationRepository repository = new ReservationRepository(db, rooms);
        MessageStore messages = new MessageStore(db);

        List<ReservationListener> listeners = List.of(
                new GuestNotifier(messages),
                new HousekeepingNotifier(messages, "governanca@pousadamarealta.com.br"),
                new FinanceNotifier(messages, "financeiro@pousadamarealta.com.br"));

        ReservationService reservations = new ReservationService(db, rooms, repository, listeners);
        ReportService reports = new ReportService(repository, rooms);
        return new Api(rooms, reservations, reports, messages).create();
    }
}
