package hotel;

import hotel.messages.EmailSender;
import hotel.messages.SentMessages;
import hotel.reports.OccupancyReport;
import hotel.reports.RatePlanReport;
import hotel.reports.RevenueReport;
import hotel.reports.TourisimTaxReport;
import hotel.reservations.PriceCalculator;
import hotel.reservations.ReservationService;
import hotel.reservations.ReservationStore;
import hotel.rooms.Rooms;
import hotel.web.HttpApi;
import io.javalin.Javalin;
import javax.sql.DataSource;

/** Puts the inn's pieces together and runs the HTTP server. */
public final class Hotel {

    private final Javalin server;

    public Hotel(DataSource dataSource) {
        Rooms rooms = new Rooms(dataSource);
        ReservationStore store = new ReservationStore(dataSource);
        SentMessages sentMessages = new SentMessages(dataSource);
        ReservationService reservations =
                new ReservationService(rooms, store, new PriceCalculator(), new EmailSender(sentMessages));
        HttpApi api = new HttpApi(
                rooms, reservations, sentMessages, new OccupancyReport(rooms, store), new RevenueReport(store),
                new TourisimTaxReport(store), new RatePlanReport(store));
        this.server = api.server();
    }

    /** Starts listening; port 0 picks a free port. Returns the port in use. */
    public int start(int port) {
        server.start(port);
        return server.port();
    }

    public void stop() {
        server.stop();
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
        Database database = new Database(System.getenv());
        new Hotel(database.dataSource()).start(port);
    }
}
