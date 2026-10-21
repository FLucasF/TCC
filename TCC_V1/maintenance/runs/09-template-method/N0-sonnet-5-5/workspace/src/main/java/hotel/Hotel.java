package hotel;

import hotel.messages.EmailSender;
import hotel.messages.SentMessages;
import hotel.reports.OccupancyReport;
import hotel.reports.RatePlanReport;
import hotel.reports.RevenueReport;
import hotel.reports.TourismTaxReport;
import hotel.reservations.PriceCalculator;
import hotel.reservations.ReservationService;
import hotel.reservations.ReservationStore;
import hotel.rooms.Rooms;
import hotel.web.HttpApi;
import io.javalin.Javalin;
import java.math.BigDecimal;

/** Puts the inn's pieces together and runs the HTTP server. */
public final class Hotel {

    /** Set by the city; it changes every year. */
    private static final BigDecimal TOURISM_TAX_PER_GUEST_NIGHT = new BigDecimal("4.50");

    private final Javalin server;

    public Hotel() {
        Rooms rooms = new Rooms();
        ReservationStore store = new ReservationStore();
        SentMessages sentMessages = new SentMessages();
        ReservationService reservations =
                new ReservationService(rooms, store, new PriceCalculator(), new EmailSender(sentMessages));
        HttpApi api = new HttpApi(
                rooms, reservations, sentMessages, new OccupancyReport(rooms, store), new RevenueReport(store),
                new TourismTaxReport(store, TOURISM_TAX_PER_GUEST_NIGHT), new RatePlanReport(store));
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
        new Hotel().start(port);
    }
}
