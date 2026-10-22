package hotel;

import hotel.messages.ArchiveCopyRule;
import hotel.messages.EmailProcessor;
import hotel.messages.EmailRule;
import hotel.messages.EmailSender;
import hotel.messages.SentMessages;
import hotel.messages.TestModeRule;
import hotel.messages.ValidationRule;
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
import javax.sql.DataSource;

/** Puts the inn's pieces together and runs the HTTP server. */
public final class Hotel {

    private final Javalin server;

    public Hotel(DataSource dataSource) {
        this(dataSource, java.util.Map.of());
    }

    public Hotel(DataSource dataSource, java.util.Map<String, String> environment) {
        boolean testModeEnabled = "true".equals(environment.get("HOTEL_TEST_MODE"));

        Rooms rooms = new Rooms(dataSource);
        ReservationStore store = new ReservationStore(dataSource);
        EmailProcessor processor = createEmailProcessor(testModeEnabled);
        SentMessages sentMessages = new SentMessages(dataSource, processor);
        ReservationService reservations =
                new ReservationService(rooms, store, new PriceCalculator(), new EmailSender(sentMessages));
        HttpApi api = new HttpApi(
                rooms, reservations, sentMessages, new OccupancyReport(rooms, store), new RevenueReport(store));
        this.server = api.server();
    }

    public static EmailProcessor createEmailProcessor(boolean testModeEnabled) {
        List<EmailRule> rules = new ArrayList<>();
        rules.add(new ValidationRule());
        rules.add(new TestModeRule(testModeEnabled));
        rules.add(new ArchiveCopyRule(testModeEnabled));
        return new EmailProcessor(rules);
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
        new Hotel(database.dataSource(), System.getenv()).start(port);
    }
}
