package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

/** The reservations that start in a month, in the order the monthly reports list them. */
final class MonthReservations {

    private MonthReservations() {
    }

    /** The ones the inn kept money from: what the financial report counts. */
    static List<Reservation> withRevenue(ReservationStore store, YearMonth month) {
        return startingIn(store, month).stream().filter(reservation -> reservation.amountKept().signum() > 0).toList();
    }

    /** The paid ones: confirmed, checked in or checked out. */
    static List<Reservation> paid(ReservationStore store, YearMonth month) {
        return startingIn(store, month).stream().filter(Reservation::isPaid).toList();
    }

    private static List<Reservation> startingIn(ReservationStore store, YearMonth month) {
        return store.all().stream()
                .filter(reservation -> YearMonth.from(reservation.checkIn()).equals(month))
                .sorted(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id))
                .toList();
    }
}
