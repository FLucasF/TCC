package com.marealta;

import com.marealta.domain.RoomCatalog;
import com.marealta.events.FinanceNotifier;
import com.marealta.events.GuestConfirmationEmail;
import com.marealta.events.HousekeepingNotifier;
import com.marealta.events.ReservationListener;
import com.marealta.persistence.Database;
import com.marealta.persistence.MessageRepository;
import com.marealta.persistence.ReservationRepository;
import com.marealta.service.ReportService;
import com.marealta.service.ReservationService;
import com.marealta.web.Routes;
import io.javalin.Javalin;

import java.util.List;

public final class App {
    private App() {
    }

    public static void main(String[] args) {
        String port = System.getenv("PORT");
        Database db = new Database(System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        start(db, port == null || port.isBlank() ? 7070 : Integer.parseInt(port));
    }

    public static Javalin start(Database db, int port) {
        db.migrate();
        RoomCatalog rooms = new RoomCatalog();
        MessageRepository messages = new MessageRepository(db);
        ReservationRepository reservations = new ReservationRepository(db);

        // Quem é avisado das reservas é decidido aqui: entrar ou sair da lista é uma linha.
        List<ReservationListener> listeners = List.of(
                new GuestConfirmationEmail(messages),
                new HousekeepingNotifier(messages, "governanca@pousadamarealta.com.br"),
                new FinanceNotifier(messages, "financeiro@pousadamarealta.com.br"));

        ReservationService service = new ReservationService(db, reservations, rooms, listeners);
        ReportService reports = new ReportService(db, reservations, rooms);

        Javalin app = Javalin.create(config -> config.showJavalinBanner = false);
        new Routes(service, reports, messages, rooms).register(app);
        return app.start(port);
    }
}
