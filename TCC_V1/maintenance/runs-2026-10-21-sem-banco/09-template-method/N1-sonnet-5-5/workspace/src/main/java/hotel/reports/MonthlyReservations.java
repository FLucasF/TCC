package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/** The reservations that start in a month, in the order of the monthly reports: check-in, then id. */
final class MonthlyReservations {

    private MonthlyReservations() {
    }

    static List<Reservation> startingIn(ReservationStore store, YearMonth month, Predicate<Reservation> included) {
        return store.all().stream()
                .filter(reservation -> included.test(reservation)
                        && YearMonth.from(reservation.checkIn()).equals(month))
                .sorted(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id))
                .toList();
    }

    /** The reservations in which the inn kept money: what the revenue report counts. */
    static List<Reservation> withRevenueStartingIn(ReservationStore store, YearMonth month) {
        return startingIn(store, month, reservation -> reservation.amountKept().signum() > 0);
    }
}
