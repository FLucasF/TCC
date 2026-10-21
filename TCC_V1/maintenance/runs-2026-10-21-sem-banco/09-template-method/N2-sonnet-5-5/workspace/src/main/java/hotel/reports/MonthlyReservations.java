package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/** The reservations that start in a month, in the order the monthly reports list them. */
final class MonthlyReservations {

    private MonthlyReservations() {
    }

    static List<Reservation> startingIn(ReservationStore store, YearMonth month, Predicate<Reservation> filter) {
        return store.all().stream()
                .filter(reservation -> YearMonth.from(reservation.checkIn()).equals(month))
                .filter(filter)
                .sorted(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id))
                .toList();
    }

    /** The reservations from which the inn kept money (the ones in the financial report). */
    static List<Reservation> withRevenue(ReservationStore store, YearMonth month) {
        return startingIn(store, month, reservation -> reservation.amountKept().signum() > 0);
    }
}
