package hotel;

import hotel.messages.ArchiveCopyHandler;
import hotel.messages.EmailSender;
import hotel.messages.MessageHandler;
import hotel.messages.SentMessages;
import hotel.messages.TestModeHandler;
import hotel.messages.ValidateEmailHandler;
import hotel.reports.OccupancyReport;
import hotel.reports.RevenueReport;
import hotel.reservations.PriceCalculator;
import hotel.reservations.ReservationService;
import hotel.reservations.ReservationStore;
import hotel.rooms.Rooms;
import hotel.web.HttpApi;
import io.javalin.Javalin;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

/** Puts the inn's pieces together and runs the HTTP server. */
public final class Hotel {

    private final Javalin server;

    public Hotel(DataSource dataSource) {
        this(dataSource, System.getenv());
    }

    public Hotel(DataSource dataSource, Map<String, String> env) {
        Rooms rooms = new Rooms(dataSource);
        ReservationStore store = new ReservationStore(dataSource);
        SentMessages sentMessages = new SentMessages(dataSource);
        List<MessageHandler> handlers = createHandlers(env);
        ReservationService reservations =
                new ReservationService(rooms, store, new PriceCalculator(), new EmailSender(sentMessages, handlers));
        HttpApi api = new HttpApi(
                rooms, reservations, sentMessages, new OccupancyReport(rooms, store), new RevenueReport(store));
        this.server = api.server();
    }

    private static List<MessageHandler> createHandlers(Map<String, String> env) {
        List<MessageHandler> handlers = new ArrayList<>();
        boolean testMode = "true".equalsIgnoreCase(env.getOrDefault("HOTEL_TEST_MODE", "false"));

        handlers.add(new ValidateEmailHandler());

        if (testMode) {
            handlers.add(new TestModeHandler());
        } else {
            handlers.add(new ArchiveCopyHandler());
        }

        return handlers;
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
