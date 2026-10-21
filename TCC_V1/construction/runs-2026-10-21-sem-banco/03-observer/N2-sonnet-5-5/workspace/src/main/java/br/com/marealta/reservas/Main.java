package br.com.marealta.reservas;

import br.com.marealta.reservas.domain.ReservationListener;
import br.com.marealta.reservas.domain.ReservationService;
import br.com.marealta.reservas.domain.RoomCatalog;
import br.com.marealta.reservas.notification.FinanceNotifier;
import br.com.marealta.reservas.notification.GuestConfirmationEmail;
import br.com.marealta.reservas.notification.HousekeepingNotifier;
import br.com.marealta.reservas.notification.MessageOutbox;
import br.com.marealta.reservas.report.OccupancyReport;
import br.com.marealta.reservas.report.RevenueReport;
import br.com.marealta.reservas.web.Api;
import io.javalin.Javalin;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        String port = System.getenv("PORT");
        build(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }

    /** Wires the application. Who gets told about reservations is decided here, and only here. */
    public static Javalin build(int port) {
        RoomCatalog rooms = RoomCatalog.marealta();
        MessageOutbox outbox = new MessageOutbox();
        List<ReservationListener> listeners = List.of(
                new GuestConfirmationEmail(outbox),
                new HousekeepingNotifier(outbox),
                new FinanceNotifier(outbox));
        ReservationService reservations = new ReservationService(rooms, listeners);
        return new Api(rooms, reservations, outbox, new OccupancyReport(rooms), new RevenueReport()).start(port);
    }
}
