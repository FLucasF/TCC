package br.com.marealta.pousada;

import br.com.marealta.pousada.app.ReportService;
import br.com.marealta.pousada.app.ReservationService;
import br.com.marealta.pousada.notification.EventBus;
import br.com.marealta.pousada.notification.Notifiers;
import br.com.marealta.pousada.persistence.Database;
import br.com.marealta.pousada.persistence.MessageRepository;
import br.com.marealta.pousada.persistence.ReservationRepository;
import br.com.marealta.pousada.web.Routes;

import io.javalin.Javalin;

public class Main {

    public static void main(String[] args) {
        Database db = new Database(required("DB_URL"), required("DB_USER"), required("DB_PASSWORD"));
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
        start(db, port);
    }

    /** Monta o sistema sobre um banco já aberto e o põe no ar. */
    public static Javalin start(Database db, int port) {
        MessageRepository messages = new MessageRepository(db);
        ReservationRepository reservationRepository = new ReservationRepository(db);
        EventBus events = Notifiers.wire(messages);
        ReservationService reservations = new ReservationService(db, reservationRepository, events);
        ReportService reports = new ReportService(db, reservationRepository);

        Javalin app = Javalin.create(config -> config.showJavalinBanner = false);
        new Routes(reservations, reports, messages, db).register(app);
        return app.start(port);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente ausente: " + name);
        }
        return value;
    }
}
