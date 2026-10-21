package hotel;

import hotel.messages.EmailSender;
import hotel.messages.SendRules;
import hotel.messages.SentMessages;
import hotel.reports.OccupancyReport;
import hotel.reports.RevenueReport;
import hotel.reservations.PriceCalculator;
import hotel.reservations.ReservationService;
import hotel.reservations.ReservationStore;
import hotel.rooms.Rooms;
import hotel.web.HttpApi;
import io.javalin.Javalin;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Map;

/** Puts the inn's pieces together and runs the HTTP server. */
public final class Hotel {

    private final Javalin server;

    public Hotel() {
        this(System.getenv(), Clock.system(ZoneId.of("America/Sao_Paulo")));
    }

    /**
     * {@code HOTEL_TEST_MODE=true} turns on test mode. {@code HOTEL_MESSAGE_RULES} picks the e-mail rules of
     * this installation (see {@link SendRules#parse}); without it, {@link SendRules#DEFAULT_SPEC} is used.
     */
    public Hotel(Map<String, String> env, Clock clock) {
        boolean testMode = "true".equalsIgnoreCase(env.get("HOTEL_TEST_MODE"));
        String ruleSpec = env.getOrDefault("HOTEL_MESSAGE_RULES", SendRules.DEFAULT_SPEC);
        Rooms rooms = new Rooms();
        ReservationStore store = new ReservationStore();
        SentMessages sentMessages = new SentMessages();
        EmailSender emailSender = new EmailSender(sentMessages, SendRules.parse(ruleSpec, testMode, clock));
        ReservationService reservations =
                new ReservationService(rooms, store, new PriceCalculator(), emailSender);
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
        new Hotel().start(port);
    }
}
