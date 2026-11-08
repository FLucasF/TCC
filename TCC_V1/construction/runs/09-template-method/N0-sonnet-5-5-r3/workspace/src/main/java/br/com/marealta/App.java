package br.com.marealta;

import br.com.marealta.db.Database;
import br.com.marealta.db.MessageRepository;
import br.com.marealta.db.ReservationRepository;
import br.com.marealta.db.RoomRepository;
import br.com.marealta.report.OccupancyReport;
import br.com.marealta.report.RatePlanReport;
import br.com.marealta.report.ReportCatalog;
import br.com.marealta.report.RevenueReport;
import br.com.marealta.report.TourismTaxReport;
import br.com.marealta.service.ReservationService;
import br.com.marealta.web.Api;
import io.javalin.Javalin;
import java.util.List;

public final class App {

    private App() {}

    public static void main(String[] args) {
        Config config = Config.from(System.getenv());
        Javalin app = start(config);
        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
    }

    /** Wires everything together and starts the server. */
    public static Javalin start(Config config) {
        Database db = new Database(config.dbUrl(), config.dbUser(), config.dbPassword());
        db.migrate();

        RoomRepository rooms = new RoomRepository();
        ReservationRepository reservations = new ReservationRepository();
        MessageRepository messages = new MessageRepository();
        ReservationService service = new ReservationService(db, rooms, reservations, messages);
        ReportCatalog reports = new ReportCatalog(db, List.of(
                new OccupancyReport(rooms, reservations),
                new RevenueReport(reservations),
                new TourismTaxReport(reservations, config.tourismTaxPerGuestNight()),
                new RatePlanReport(reservations)));

        Javalin app = new Api(service, reports).create();
        app.events(e -> e.serverStopped(db::close));
        return app.start(config.port());
    }
}
