package hotel;

import hotel.messages.ArchiveCopyRule;
import hotel.messages.EmailSender;
import hotel.messages.EmailValidationRule;
import hotel.messages.MessageRule;
import hotel.messages.MessageRuleChain;
import hotel.messages.SentMessages;
import hotel.messages.TestModeRedirectRule;
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

    public Hotel(DataSource dataSource, boolean testMode) {
        Rooms rooms = new Rooms(dataSource);
        ReservationStore store = new ReservationStore(dataSource);
        SentMessages sentMessages = new SentMessages(dataSource);

        List<MessageRule> rules = new ArrayList<>();
        rules.add(new EmailValidationRule());
        rules.add(new TestModeRedirectRule(testMode));
        if (!testMode) {
            rules.add(new ArchiveCopyRule());
        }
        MessageRuleChain ruleChain = new MessageRuleChain(rules);

        ReservationService reservations =
                new ReservationService(rooms, store, new PriceCalculator(), new EmailSender(sentMessages, ruleChain));
        HttpApi api = new HttpApi(
                rooms, reservations, sentMessages, new OccupancyReport(rooms, store), new RevenueReport(store));
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
        boolean testMode = "true".equalsIgnoreCase(System.getenv().getOrDefault("HOTEL_TEST_MODE", "false"));
        Database database = new Database(System.getenv());
        new Hotel(database.dataSource(), testMode).start(port);
    }
}
