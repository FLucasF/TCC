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
import javax.sql.DataSource;

/** Puts the inn's pieces together and runs the HTTP server. */
public final class Hotel {

    private final Javalin server;

    /** The tourism tax per guest per night, unless the city changes it (see TOURISM_TAX_PER_GUEST_NIGHT). */
    static final BigDecimal DEFAULT_TOURISM_TAX = new BigDecimal("4.50");

    public Hotel(DataSource dataSource) {
        this(dataSource, DEFAULT_TOURISM_TAX);
    }

    public Hotel(DataSource dataSource, BigDecimal tourismTaxPerGuestNight) {
        Rooms rooms = new Rooms(dataSource);
        ReservationStore store = new ReservationStore(dataSource);
        SentMessages sentMessages = new SentMessages(dataSource);
        ReservationService reservations =
                new ReservationService(rooms, store, new PriceCalculator(), new EmailSender(sentMessages));
        HttpApi api = new HttpApi(
                rooms,
                reservations,
                sentMessages,
                new OccupancyReport(rooms, store),
                new RevenueReport(store),
                new TourismTaxReport(store, tourismTaxPerGuestNight),
                new RatePlanReport(store));
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
        BigDecimal tourismTax = new BigDecimal(
                System.getenv().getOrDefault("TOURISM_TAX_PER_GUEST_NIGHT", DEFAULT_TOURISM_TAX.toPlainString()));
        new Hotel(database.dataSource(), tourismTax).start(port);
    }
}
