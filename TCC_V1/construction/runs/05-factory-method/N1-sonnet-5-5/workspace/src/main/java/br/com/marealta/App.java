package br.com.marealta;

import br.com.marealta.persistence.Database;
import br.com.marealta.persistence.MessageRepository;
import br.com.marealta.persistence.ReservationRepository;
import br.com.marealta.persistence.RoomRepository;
import br.com.marealta.service.ReportService;
import br.com.marealta.service.ReservationService;
import br.com.marealta.web.Routes;
import io.javalin.Javalin;

/** Monta o sistema a partir de um banco; usado pelo Main e pelos testes. */
public final class App {
    private App() {}

    public static Javalin create(Database database) {
        RoomRepository rooms = new RoomRepository();
        ReservationRepository reservations = new ReservationRepository();
        database.initialize(rooms);

        Javalin app = Javalin.create(config -> config.showJavalinBanner = false);
        new Routes(
                new ReservationService(database, rooms, reservations, new MessageRepository()),
                new ReportService(database, rooms, reservations)).register(app);
        return app;
    }
}
