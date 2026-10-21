package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/** Picks the reservations that start in a month, in the order the monthly reports list them. */
final class MonthlyReservations {

    private MonthlyReservations() {
    }

    static List<Reservation> startingIn(YearMonth month, ReservationStore store, Predicate<Reservation> filter) {
        return store.all().stream()
                .filter(reservation -> YearMonth.from(reservation.checkIn()).equals(month))
                .filter(filter)
                .sorted(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id))
                .toList();
    }

    static boolean keptMoney(Reservation reservation) {
        return reservation.amountKept().signum() > 0;
    }
}
