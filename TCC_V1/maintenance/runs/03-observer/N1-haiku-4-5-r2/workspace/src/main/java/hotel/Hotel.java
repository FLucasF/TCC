package hotel;

import hotel.messages.EmailSender;
import hotel.messages.SentMessages;
import hotel.notifications.FinanceListener;
import hotel.notifications.GovernanceListener;
import hotel.notifications.GuestConfirmationListener;
import hotel.notifications.ReservationNotificationManager;
import hotel.reports.OccupancyReport;
import hotel.reports.RevenueReport;
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
        EmailSender emailSender = new EmailSender(sentMessages);

        ReservationNotificationManager notificationManager = new ReservationNotificationManager();
        notificationManager.addListener(new GuestConfirmationListener(emailSender));
        notificationManager.addListener(new GovernanceListener(emailSender));
        notificationManager.addListener(new FinanceListener(emailSender));

        ReservationService reservations =
                new ReservationService(rooms, store, new PriceCalculator(), notificationManager);
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
        Database database = new Database(System.getenv());
        new Hotel(database.dataSource()).start(port);
    }
}
