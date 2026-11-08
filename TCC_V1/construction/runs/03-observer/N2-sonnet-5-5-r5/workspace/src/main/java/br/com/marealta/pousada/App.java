package br.com.marealta.pousada;

import br.com.marealta.pousada.notification.FinanceNotifier;
import br.com.marealta.pousada.notification.GuestConfirmationEmail;
import br.com.marealta.pousada.notification.HousekeepingNotifier;
import br.com.marealta.pousada.notification.ReservationListener;
import br.com.marealta.pousada.persistence.Database;
import br.com.marealta.pousada.persistence.MessageRepository;
import br.com.marealta.pousada.persistence.ReservationRepository;
import br.com.marealta.pousada.persistence.RoomRepository;
import br.com.marealta.pousada.service.ReportService;
import br.com.marealta.pousada.service.ReservationService;
import br.com.marealta.pousada.web.Routes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import java.util.List;

public class App {
    public static void main(String[] args) {
        Database db = new Database(required("DB_URL"), required("DB_USER"), required("DB_PASSWORD"));
        String port = System.getenv("PORT");
        start(db, port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }

    /** Monta o sistema. É aqui que se decide quem é avisado de cada coisa que acontece com as reservas. */
    public static Javalin start(Database db, int port) {
        db.migrate();
        RoomRepository rooms = new RoomRepository(db);
        ReservationRepository reservationRepository = new ReservationRepository(db);
        MessageRepository messages = new MessageRepository(db);

        List<ReservationListener> listeners = List.of(
                new GuestConfirmationEmail(messages),
                new HousekeepingNotifier(messages, "governanca@pousadamarealta.com.br"),
                new FinanceNotifier(messages, "financeiro@pousadamarealta.com.br"));

        ReservationService reservations = new ReservationService(rooms, reservationRepository, listeners);
        ReportService reports = new ReportService(rooms, reservationRepository);

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        Javalin app = Javalin.create(config -> config.jsonMapper(new JavalinJackson(mapper, false)));
        new Routes(rooms, reservations, reports, messages).register(app);
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
