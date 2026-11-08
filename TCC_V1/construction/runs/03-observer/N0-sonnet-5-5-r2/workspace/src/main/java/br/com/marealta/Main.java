package br.com.marealta;

import br.com.marealta.events.FinanceNotifier;
import br.com.marealta.events.GuestNotifier;
import br.com.marealta.events.HousekeepingNotifier;
import br.com.marealta.events.MessageSender;
import br.com.marealta.events.ReservationEvents;
import br.com.marealta.events.ReservationListener;
import br.com.marealta.events.StoredMessageSender;
import br.com.marealta.infra.Database;
import br.com.marealta.infra.JdbcMessageStore;
import br.com.marealta.infra.JdbcReservationRepository;
import br.com.marealta.infra.JdbcRoomRepository;
import br.com.marealta.service.ReportService;
import br.com.marealta.service.ReservationService;
import br.com.marealta.web.App;

import java.util.List;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        String dbUrl = System.getenv("DB_URL");
        if (dbUrl == null || dbUrl.isBlank()) {
            throw new IllegalStateException("A variável de ambiente DB_URL é obrigatória.");
        }
        String portEnv = System.getenv("PORT");
        int port = portEnv == null || portEnv.isBlank() ? 7070 : Integer.parseInt(portEnv.trim());

        Database db = new Database(dbUrl, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        db.initSchema();

        JdbcRoomRepository rooms = new JdbcRoomRepository(db);
        JdbcReservationRepository reservations = new JdbcReservationRepository(db);
        JdbcMessageStore messages = new JdbcMessageStore(db);

        // Quem é avisado: para incluir ou tirar uma área (restaurante, transfer...), é só mexer nesta lista.
        MessageSender sender = new StoredMessageSender(messages);
        ReservationEvents events = new ReservationEvents(List.<ReservationListener>of(
                new GuestNotifier(sender),
                new HousekeepingNotifier(sender),
                new FinanceNotifier(sender)));

        App app = new App(
                new ReservationService(db, rooms, reservations, messages, events),
                new ReportService(db, rooms, reservations));
        app.create().start(port);
    }
}
