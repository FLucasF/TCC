package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/** The reservations that start in a month, in the order the monthly reports list them. */
final class MonthReservations {

    private MonthReservations() {
    }

    /** The ones from which the inn kept money. */
    static List<Reservation> withRevenue(ReservationStore store, YearMonth month) {
        return startingIn(store, month, reservation -> reservation.amountKept().signum() > 0);
    }

    /** The ones the guest paid and did not cancel. */
    static List<Reservation> paid(ReservationStore store, YearMonth month) {
        return startingIn(store, month, Reservation::isPaid);
    }

    private static List<Reservation> startingIn(
            ReservationStore store, YearMonth month, Predicate<Reservation> filter) {
        return store.all().stream()
                .filter(filter)
                .filter(reservation -> YearMonth.from(reservation.checkIn()).equals(month))
                .sorted(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id))
                .toList();
    }
}
