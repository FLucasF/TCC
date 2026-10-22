package br.com.marealta.pousada;

import br.com.marealta.pousada.mail.MailPipeline;
import br.com.marealta.pousada.mail.MailSetup;
import br.com.marealta.pousada.service.Outbox;
import br.com.marealta.pousada.service.ReportService;
import br.com.marealta.pousada.service.ReservationService;
import br.com.marealta.pousada.store.Database;
import br.com.marealta.pousada.store.MessageRepository;
import br.com.marealta.pousada.store.ReservationRepository;
import br.com.marealta.pousada.store.RoomRepository;
import br.com.marealta.pousada.web.Routes;
import io.javalin.Javalin;

public class App {

    public static void main(String[] args) {
        String port = System.getenv().getOrDefault("PORT", "7070");
        boolean testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));

        Database database = new Database(
                System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        database.migrate();

        RoomRepository rooms = new RoomRepository();
        ReservationRepository reservations = new ReservationRepository();
        Outbox outbox = new Outbox(database, new MessageRepository(),
                new MailPipeline(testMode, MailSetup.pousadaMareAlta()));

        Javalin app = Javalin.create();
        new Routes(new ReservationService(database, rooms, reservations, outbox),
                new ReportService(database, rooms, reservations), outbox).register(app);
        app.start(Integer.parseInt(port));
    }
}
